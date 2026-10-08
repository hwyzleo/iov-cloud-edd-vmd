package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.*;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 业务密钥轮换应用服务（CR-055 F20 §6.4）
 * <p>
 * 编排「锁定旧 ACTIVE → 创建新材料 → 验证可 Wrap → 持久化新版本 → 原子切换 ACTIVE」。
 * 任何创建/Wrap/落库失败都不得提前降级旧 ACTIVE；成功后旧版本转 DEPRECATED（仅解密窗口），
 * 业务版本递增且 KMS 版本不混入信封。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyRotationAppService {

    private static final String STATE_ROTATED = "ROTATED";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final BusinessKeyBizTypeMapper bizTypeMapper;
    private final ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
    private final PlatformTransactionManager transactionManager;

    private volatile TransactionTemplate transactionTemplate;

    /**
     * 获取 BusinessKeyMaterialTemplate（framework 业务密钥未装配时 fail-closed）
     */
    private BusinessKeyMaterialTemplate materialTemplate() {
        BusinessKeyMaterialTemplate template = businessKeyMaterialTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new BusinessKeyKmsUnavailableException("framework业务密钥模板未装配（crypto.business-key.enabled=false）");
        }
        return template;
    }

    /**
     * 获取缓存失效器（未装配时忽略缓存失效，不影响主流程）
     */
    private BusinessKeyCacheInvalidator cacheInvalidator() {
        return cacheInvalidatorProvider.getIfAvailable();
    }

    /**
     * 惰性初始化 TransactionTemplate（保证单元测试可注入）
     */
    private TransactionTemplate tx() {
        TransactionTemplate tpl = this.transactionTemplate;
        if (tpl == null) {
            synchronized (this) {
                if (this.transactionTemplate == null) {
                    this.transactionTemplate = new TransactionTemplate(transactionManager);
                }
                tpl = this.transactionTemplate;
            }
        }
        return tpl;
    }

    /**
     * 业务密钥轮换
     *
     * @param cmd 轮换命令
     * @return 轮换结果（新/旧 keyId 与业务版本）
     */
    public BusinessKeyRotationResult rotate(BusinessKeyRotateCmd cmd) {
        log.info("业务密钥轮换: requestId={}, deviceSn={}, domain={}, purpose={}",
                cmd.getRequestId(), cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose());
        validateCmd(cmd);

        // 1. 解析 active 绑定身份
        BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolveByDeviceSn(
                cmd.getDeviceSn(), cmd.getDeviceCategory());

        // 2. 授权（ROTATE）
        BusinessKeyPolicyEntry policy = authorizationPolicy.authorize(
                BusinessKeyAction.ROTATE, identity.deviceCategory(), cmd.getBusinessDomain(), cmd.getPurpose());

        // 3. 事务内锁定旧 ACTIVE + 分配新版本 + 落 PENDING 新行（串行化并发轮换）
        RotationPlan plan = lockAndPlan(cmd, policy);
        DeviceBusinessKey oldActive = plan.oldActive;
        DeviceBusinessKey newRow = plan.newRow;

        try {
            // 4. 创建新材料（framework，事务外网络 I/O）
            BusinessKeyMaterial material;
            try {
                material = materialTemplate().create(buildCreateRequest(cmd, identity, policy));
            } catch (CryptoOperationOutcomeUnknownException e) {
                failNewRow(newRow, "framework创建结果未知，待对账");
                throw new BusinessKeyOutcomeUnknownException("轮换材料创建结果未知: requestId=" + cmd.getRequestId());
            } catch (net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyIdempotencyConflictException e) {
                failNewRow(newRow, "framework幂等冲突");
                throw new BusinessKeyIdempotencyConflictException("轮换材料创建幂等冲突: requestId=" + cmd.getRequestId());
            } catch (CryptoDependencyUnavailableException e) {
                failNewRow(newRow, "KMS/HSM或framework安全服务不可用");
                throw new BusinessKeyKmsUnavailableException("KMS/HSM或framework安全服务不可用: requestId=" + cmd.getRequestId());
            } catch (CryptoException e) {
                failNewRow(newRow, truncate(e.getMessage()));
                throw new BusinessKeyKmsUnavailableException("轮换材料创建失败: requestId=" + cmd.getRequestId());
            }

            // 5. 用当前设备证书验证新 keyRef 可 Wrap（仅验证，Wrapped Key 不落库）
            verifyWrap(cmd, identity, material, newRow);

            // 6. 原子切换：旧→DEPRECATED + 新→ACTIVE + ROTATED 事件 + 缓存失效（单事务）
            switchActive(oldActive, newRow, material, cmd, policy);

            log.info("业务密钥轮换成功: requestId={}, oldKeyId={}, newKeyId={}, newVersion={}",
                    cmd.getRequestId(), oldActive.getKeyId(), material.keyId(), newRow.getBusinessKeyVersion());
            return BusinessKeyRotationResult.builder()
                    .requestId(cmd.getRequestId())
                    .newKeyId(material.keyId())
                    .newBusinessKeyVersion(newRow.getBusinessKeyVersion())
                    .oldKeyId(oldActive.getKeyId())
                    .oldBusinessKeyVersion(oldActive.getBusinessKeyVersion())
                    .state(BusinessKeyState.ACTIVE.getValue())
                    .build();
        } catch (RuntimeException e) {
            // 任何失败都不得降级旧 ACTIVE；新行失败已留痕
            log.warn("业务密钥轮换失败（旧ACTIVE保持）: requestId={}, oldKeyId={}", cmd.getRequestId(), oldActive.getKeyId(), e);
            throw e;
        }
    }

    /**
     * 事务内锁定旧 ACTIVE + 分配新版本 + 落 PENDING 新行
     */
    private RotationPlan lockAndPlan(BusinessKeyRotateCmd cmd, BusinessKeyPolicyEntry policy) {
        return tx().execute(status -> {
            DeviceBusinessKey oldActive = deviceBusinessKeyRepository.selectActiveByContextForUpdate(
                    cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose());
            if (oldActive == null) {
                throw new BusinessKeyNotExistException(
                        "轮换失败：当前无ACTIVE业务密钥: deviceSn=" + cmd.getDeviceSn()
                                + ", domain=" + cmd.getBusinessDomain() + ", purpose=" + cmd.getPurpose());
            }
            Long newVersion = deviceBusinessKeyRepository.maxBusinessKeyVersion(
                    cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose()) + 1;
            DeviceBusinessKey newRow = DeviceBusinessKey.builder()
                    .deviceSn(cmd.getDeviceSn())
                    .bindingId(oldActive.getBindingId())
                    .partId(oldActive.getPartId())
                    .hsmUid(oldActive.getHsmUid())
                    .businessDomain(cmd.getBusinessDomain())
                    .purpose(cmd.getPurpose())
                    .businessKeyVersion(newVersion)
                    .keyState(BusinessKeyState.PENDING)
                    .wrapMode("DEVICE_CERT_PUBLIC_KEY")
                    .requestId(cmd.getRequestId())
                    .requestDigest(buildDigest(cmd))
                    .policyVersion(policy.policyVersion())
                    .createTime(LocalDateTime.now())
                    .build();
            try {
                deviceBusinessKeyRepository.insert(newRow);
            } catch (DuplicateKeyException e) {
                throw new BusinessKeyRotationConflictException(
                        "轮换新版本占位冲突: requestId=" + cmd.getRequestId() + ", version=" + newVersion);
            }
            return new RotationPlan(oldActive, newRow);
        });
    }

    /**
     * 设备证书 Wrap 验证（新 keyRef 可 Wrap 才允许切换）
     */
    private void verifyWrap(BusinessKeyRotateCmd cmd, BoundDeviceIdentity identity, BusinessKeyMaterial material,
                            DeviceBusinessKey newRow) {
        String certSn = activeDeviceCertificateResolver.resolveActiveCertSn(identity);
        try {
            materialTemplate().wrap(
                    new BusinessKeyRef(material.keyId(), material.kmsKeyRef()),
                    new RecipientRef.DeviceCertificate(certSn),
                    new WrapContext(WrapMode.DEVICE_CERT_PUBLIC_KEY, cmd.getRequestId() + ":verify",
                            Map.of("deviceSn", cmd.getDeviceSn(), "purpose", "ROTATE_VERIFY")));
        } catch (CryptoOperationOutcomeUnknownException e) {
            failNewRow(newRow, "轮换Wrap验证结果未知，待对账");
            throw new BusinessKeyOutcomeUnknownException("轮换Wrap验证结果未知: keyId=" + material.keyId());
        } catch (CryptoException e) {
            failNewRow(newRow, "轮换Wrap验证失败");
            throw new BusinessKeyWrapFailedException("轮换Wrap验证失败: keyId=" + material.keyId());
        }
    }

    /**
     * 原子切换：旧→DEPRECATED（decrypt_until）+ 新→ACTIVE（回填材料）+ ROTATED 事件 + 缓存失效
     */
    private void switchActive(DeviceBusinessKey oldActive, DeviceBusinessKey newRow, BusinessKeyMaterial material,
                              BusinessKeyRotateCmd cmd, BusinessKeyPolicyEntry policy) {
        tx().executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();

            // 1) 旧 ACTIVE → DEPRECATED（仅解密窗口），乐观锁保护
            DeviceBusinessKey oldCurrent = deviceBusinessKeyRepository.selectById(oldActive.getId());
            if (oldCurrent == null || oldCurrent.getKeyState() != BusinessKeyState.ACTIVE) {
                throw new BusinessKeyRotationConflictException("旧ACTIVE已被变更，轮换冲突: keyId=" + oldActive.getKeyId());
            }
            LocalDateTime decryptUntil = policy.decryptWindow() != null
                    ? now.plus(policy.decryptWindow()) : now;
            oldCurrent.deprecate(decryptUntil);
            oldCurrent.setModifyTime(now);
            int r1 = deviceBusinessKeyRepository.update(oldCurrent);
            if (r1 == 0) {
                throw new BusinessKeyRotationConflictException("旧ACTIVE状态变更并发冲突: keyId=" + oldActive.getKeyId());
            }

            // 2) 新行 PENDING → ACTIVE（回填材料）
            DeviceBusinessKey newCurrent = deviceBusinessKeyRepository.selectById(newRow.getId());
            if (newCurrent == null || newCurrent.getKeyState() != BusinessKeyState.PENDING) {
                throw new BusinessKeyRotationConflictException("新行状态异常: id=" + newRow.getId());
            }
            newCurrent.setKeyId(material.keyId());
            newCurrent.setKmsKeyRef(material.kmsKeyRef());
            newCurrent.setKmsKeyVersion(material.kmsKeyVersion());
            newCurrent.setKmsProvider(material.provider());
            newCurrent.setAlgorithm(material.algorithm());
            newCurrent.setKeySpec(material.keySpec());
            newCurrent.setValidFrom(toLocalDateTime(material.validFrom()));
            newCurrent.setValidTo(toLocalDateTime(material.validTo()));
            newCurrent.activate();
            newCurrent.setModifyTime(now);
            int r2 = deviceBusinessKeyRepository.update(newCurrent);
            if (r2 == 0) {
                throw new BusinessKeyRotationConflictException("新行激活并发冲突: id=" + newRow.getId());
            }

            // 3) ROTATED 事件（Outbox，同一事务）
            businessKeyChangedPublisher.publish(newCurrent, STATE_ROTATED);

            // 4) 缓存失效（旧 key 与上下文）
            try {
                BusinessKeyCacheInvalidator invalidator = cacheInvalidator();
                if (invalidator != null) {
                    invalidator.invalidateKey(oldActive.getKeyId());
                }
            } catch (Exception e) {
                log.warn("业务密钥缓存失效异常（不影响轮换提交）: keyId={}", oldActive.getKeyId(), e);
            }
        });
    }

    /**
     * 新行失败留痕（FAILED），不降级旧 ACTIVE
     */
    private void failNewRow(DeviceBusinessKey newRow, String reason) {
        if (newRow == null) {
            return;
        }
        try {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(newRow.getId());
            if (current != null && current.getKeyState() == BusinessKeyState.PENDING) {
                current.markFailed(reason);
                current.setModifyTime(LocalDateTime.now());
                deviceBusinessKeyRepository.update(current);
            }
        } catch (Exception e) {
            log.warn("轮换新行失败留痕异常: id={}", newRow.getId(), e);
        }
    }

    private BusinessKeyCreateRequest buildCreateRequest(BusinessKeyRotateCmd cmd, BoundDeviceIdentity identity,
                                                        BusinessKeyPolicyEntry policy) {
        BizType bizType = bizTypeMapper.resolveBizType(cmd.getBusinessDomain());
        KeyMaterialPolicy materialPolicy = KeyMaterialPolicy.of(policy.algorithm(), policy.keySpec());
        Map<String, String> auditContext = new HashMap<>();
        auditContext.put("deviceSn", cmd.getDeviceSn());
        auditContext.put("businessDomain", cmd.getBusinessDomain());
        auditContext.put("purpose", cmd.getPurpose());
        auditContext.put("operation", "ROTATE");
        auditContext.put("operatorId", cmd.getOperatorId() == null ? "" : cmd.getOperatorId());
        return new BusinessKeyCreateRequest(bizType, materialPolicy, cmd.getRequestId(), auditContext);
    }

    private String buildDigest(BusinessKeyRotateCmd cmd) {
        return String.join("|", cmd.getDeviceSn(), cmd.getDeviceCategory() == null ? "" : cmd.getDeviceCategory(),
                cmd.getBusinessDomain(), cmd.getPurpose(), "ROTATE");
    }

    private void validateCmd(BusinessKeyRotateCmd cmd) {
        if (cmd == null || StrUtil.isBlank(cmd.getRequestId()) || StrUtil.isBlank(cmd.getDeviceSn())
                || StrUtil.isBlank(cmd.getBusinessDomain()) || StrUtil.isBlank(cmd.getPurpose())) {
            throw new IllegalArgumentException("业务密钥轮换参数不完整: requestId/deviceSn/businessDomain/purpose 必填");
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * 轮换计划（旧 ACTIVE + 新 PENDING 行）
     */
    private record RotationPlan(DeviceBusinessKey oldActive, DeviceBusinessKey newRow) {
    }
}
