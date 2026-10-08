package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyProvisionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyProvisionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.*;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
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
 * 业务密钥在线申请应用服务（CR-055 F20 §6.1）
 * <p>
 * 在线申请、幂等复用、创建材料（framework BusinessKeyMaterialTemplate.create）与设备证书公钥 Wrap。
 * 流程：
 * <ol>
 *   <li>会话身份与 payload deviceSn 双重校验（RD-055-8），不一致 framework 调用次数为 0；</li>
 *   <li>requestId 幂等：同 requestId 同 digest 复用；同 requestId 异 digest 报幂等冲突；</li>
 *   <li>解析 active 绑定身份 → 授权（域/用途/设备类别/操作）→ 解析当前有效设备证书；</li>
 *   <li>无 ACTIVE：落 PENDING 占位（分配 businessKeyVersion）→ framework create → 回填材料并置 ACTIVE；</li>
 *   <li>framework 结果未知：行转 RECONCILE_REQUIRED，不得换 requestId 重试（以原 requestId 对账）；</li>
 *   <li>wrap 到设备证书公钥（Wrapped Key 不落库、不进日志/事件）。</li>
 * </ol>
 * 本方法不在数据库事务内横跨 framework 网络 I/O：占位插入、状态回填、Wrap 审计为短事务。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningAppService {

    private static final String WRAP_MODE = "DEVICE_CERT_PUBLIC_KEY";
    private static final String STATE_ACTIVE = "ACTIVE";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final BusinessKeyBizTypeMapper bizTypeMapper;
    private final ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
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
     * 在线申请业务密钥并下发到设备（信封加密）
     *
     * @param cmd 申请命令
     * @return 申请结果（含一次性 Wrapped Key）
     */
    public BusinessKeyProvisionResult provision(BusinessKeyProvisionCmd cmd) {
        log.info("业务密钥在线申请: requestId={}, deviceSn={}, domain={}, purpose={}",
                cmd.getRequestId(), cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose());

        // 1. 参数校验
        validateCmd(cmd);

        // 2. 会话身份与 payload deviceSn 双重校验（RD-055-8）
        if (StrUtil.isNotBlank(cmd.getSessionDeviceSn()) && !cmd.getSessionDeviceSn().equals(cmd.getDeviceSn())) {
            log.warn("业务密钥申请拒绝：设备会话身份与payload不一致 session={}, payload={}",
                    cmd.getSessionDeviceSn(), cmd.getDeviceSn());
            throw new BusinessKeyDeviceSessionMismatchException(
                    "设备会话身份与请求deviceSn不一致: session=" + cmd.getSessionDeviceSn() + ", payload=" + cmd.getDeviceSn());
        }

        // 3. 幂等检查（按 requestId）
        DeviceBusinessKey existing = deviceBusinessKeyRepository.selectByRequestId(cmd.getRequestId());
        if (existing != null) {
            // 3.1 同 requestId 异 digest → 幂等冲突
            if (StrUtil.isNotBlank(existing.getRequestDigest()) && !existing.getRequestDigest().equals(buildDigest(cmd))) {
                throw new BusinessKeyIdempotencyConflictException(
                        "同 requestId 不同请求摘要: requestId=" + cmd.getRequestId());
            }
            // 3.2 已 ACTIVE → 复用显式 keyRef，重新 wrap 下发（Wrapped Key 每次新生成）
            if (existing.getKeyState() == BusinessKeyState.ACTIVE) {
                log.info("业务密钥申请幂等复用: requestId={}, keyId={}, version={}",
                        cmd.getRequestId(), existing.getKeyId(), existing.getBusinessKeyVersion());
                return wrapAndBuild(cmd, existing, true);
            }
            // 3.3 PENDING/FAILED/RECONCILE_REQUIRED → 以原 requestId 继续（KMS 幂等）
            if (existing.getKeyState() != BusinessKeyState.PENDING
                    && existing.getKeyState() != BusinessKeyState.FAILED
                    && existing.getKeyState() != BusinessKeyState.RECONCILE_REQUIRED) {
                throw new BusinessKeyStateNotAllowedException(
                        "requestId 对应业务密钥状态不允许继续申请: state=" + existing.getKeyState());
            }
            log.info("业务密钥申请继续补偿（原requestId）: requestId={}, state={}", cmd.getRequestId(), existing.getKeyState());
        }

        // 4. 解析 active 绑定身份（keyprov 入站无 VIN，按 deviceSn 定位）
        BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolveByDeviceSn(
                cmd.getDeviceSn(), cmd.getDeviceCategory());

        // 5. 授权（域/用途/设备类别/操作，fail-closed）
        BusinessKeyPolicyEntry policy = authorizationPolicy.authorize(
                BusinessKeyAction.PROVISION, identity.deviceCategory(), cmd.getBusinessDomain(), cmd.getPurpose());

        // 6. 解析当前有效设备证书（Wrap 收件方）
        String certSn = activeDeviceCertificateResolver.resolveActiveCertSn(identity);

        // 7. 创建或继续（原 requestId 幂等；无占位行则先落 PENDING 再 create）
        DeviceBusinessKey row = existing;
        if (row == null) {
            row = insertPending(cmd, identity, policy);
        }
        DeviceBusinessKey finalized = createMaterialAndActivate(row, cmd, identity, policy);

        // 8. 设备证书公钥 Wrap 并构建响应
        return wrapAndBuild(cmd, finalized, false);
    }

    /**
     * 创建材料并回填 ACTIVE（framework 网络 I/O 在事务外）
     */
    private DeviceBusinessKey createMaterialAndActivate(DeviceBusinessKey row, BusinessKeyProvisionCmd cmd,
                                                        BoundDeviceIdentity identity, BusinessKeyPolicyEntry policy) {
        BusinessKeyMaterial material;
        try {
            material = materialTemplate().create(buildCreateRequest(cmd, identity, policy));
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.error("业务密钥材料创建结果未知: requestId={}", cmd.getRequestId(), e);
            row.markReconcileRequired("framework创建结果未知");
            deviceBusinessKeyRepository.update(row);
            throw new BusinessKeyOutcomeUnknownException("业务密钥材料创建结果未知，待对账: requestId=" + cmd.getRequestId());
        } catch (net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyIdempotencyConflictException e) {
            throw new BusinessKeyIdempotencyConflictException("framework幂等冲突: requestId=" + cmd.getRequestId());
        } catch (CryptoDependencyUnavailableException e) {
            log.error("KMS/HSM或framework安全服务不可用: requestId={}", cmd.getRequestId(), e);
            row.markFailed("KMS/HSM或framework安全服务不可用");
            deviceBusinessKeyRepository.update(row);
            throw new BusinessKeyKmsUnavailableException("KMS/HSM或framework安全服务不可用: requestId=" + cmd.getRequestId());
        } catch (CryptoException e) {
            log.error("业务密钥材料创建失败: requestId={}", cmd.getRequestId(), e);
            row.markFailed(truncate(e.getMessage()));
            deviceBusinessKeyRepository.update(row);
            throw new BusinessKeyKmsUnavailableException("业务密钥材料创建失败: requestId=" + cmd.getRequestId());
        }

        // 回填材料并置 ACTIVE（短事务）
        return persistActivated(row, material);
    }

    /**
     * 落 PENDING 占位行并分配 businessKeyVersion（uk_context_version 冲突时重试分配）
     */
    private DeviceBusinessKey insertPending(BusinessKeyProvisionCmd cmd, BoundDeviceIdentity identity,
                                            BusinessKeyPolicyEntry policy) {
        String digest = buildDigest(cmd);
        for (int attempt = 0; attempt < 3; attempt++) {
            Long nextVersion = deviceBusinessKeyRepository.maxBusinessKeyVersion(
                    cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose()) + 1;
            DeviceBusinessKey row = DeviceBusinessKey.builder()
                    .deviceSn(cmd.getDeviceSn())
                    .bindingId(identity.bindingId())
                    .partId(identity.partId())
                    .hsmUid(identity.hsmUid())
                    .businessDomain(cmd.getBusinessDomain())
                    .purpose(cmd.getPurpose())
                    .businessKeyVersion(nextVersion)
                    .keyState(BusinessKeyState.PENDING)
                    .wrapMode(WRAP_MODE)
                    .requestId(cmd.getRequestId())
                    .requestDigest(digest)
                    .policyVersion(policy.policyVersion())
                    .createTime(LocalDateTime.now())
                    .build();
            try {
                deviceBusinessKeyRepository.insert(row);
                return row;
            } catch (DuplicateKeyException e) {
                DeviceBusinessKey raced = deviceBusinessKeyRepository.selectByRequestId(cmd.getRequestId());
                if (raced != null) {
                    return raced;
                }
                log.warn("业务密钥占位版本冲突，重试分配: attempt={}, deviceSn={}, version={}",
                        attempt, cmd.getDeviceSn(), nextVersion);
            }
        }
        throw new BusinessKeyRotationConflictException("业务密钥占位创建并发冲突: requestId=" + cmd.getRequestId());
    }

    /**
     * 回填 framework 材料并置 ACTIVE（短事务，含 Outbox 事件）
     */
    protected DeviceBusinessKey persistActivated(DeviceBusinessKey row, BusinessKeyMaterial material) {
        return tx().execute(status -> {
            row.setKeyId(material.keyId());
            row.setKmsKeyRef(material.kmsKeyRef());
            row.setKmsKeyVersion(material.kmsKeyVersion());
            row.setKmsProvider(material.provider());
            row.setAlgorithm(material.algorithm());
            row.setKeySpec(material.keySpec());
            row.setValidFrom(toLocalDateTime(material.validFrom()));
            row.setValidTo(toLocalDateTime(material.validTo()));
            row.activate();
            row.setModifyTime(LocalDateTime.now());
            int rows = deviceBusinessKeyRepository.update(row);
            if (rows == 0) {
                // 乐观锁失败：行被并发修改，读回判定
                DeviceBusinessKey current = deviceBusinessKeyRepository.selectByRequestId(row.getRequestId());
                if (current != null && current.getKeyState() == BusinessKeyState.ACTIVE && current.getKeyId() != null) {
                    log.info("业务密钥已被并发激活，复用当前行: requestId={}, keyId={}", row.getRequestId(), current.getKeyId());
                    return current;
                }
                throw new BusinessKeyRotationConflictException("业务密钥激活并发冲突: requestId=" + row.getRequestId());
            }
            businessKeyChangedPublisher.publish(row, STATE_ACTIVE);
            return row;
        });
    }

    /**
     * 设备证书公钥 Wrap 并构建响应（Wrapped Key 不落库）
     */
    private BusinessKeyProvisionResult wrapAndBuild(BusinessKeyProvisionCmd cmd, DeviceBusinessKey row, boolean reused) {
        String certSn = activeDeviceCertificateResolver.resolveActiveCertSn(
                boundDeviceIdentityResolver.resolveByDeviceSn(cmd.getDeviceSn(), cmd.getDeviceCategory()));

        WrappedBusinessKey wrapped;
        try {
            wrapped = materialTemplate().wrap(
                    new BusinessKeyRef(row.getKeyId(), row.getKmsKeyRef()),
                    new RecipientRef.DeviceCertificate(certSn),
                    new WrapContext(WrapMode.DEVICE_CERT_PUBLIC_KEY, cmd.getRequestId() + ":wrap",
                            buildAuditContext(cmd, certSn)));
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.error("业务密钥Wrap结果未知: keyId={}", row.getKeyId(), e);
            throw new BusinessKeyOutcomeUnknownException("业务密钥Wrap结果未知: keyId=" + row.getKeyId());
        } catch (net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyWrapFailedException e) {
            throw new BusinessKeyWrapFailedException("业务密钥Wrap失败: keyId=" + row.getKeyId());
        } catch (CryptoException e) {
            throw new BusinessKeyKmsUnavailableException("业务密钥Wrap不可用: keyId=" + row.getKeyId());
        }

        // 审计：最近封装证书（短事务，不携带 Wrapped Key）
        persistLastRecipientCert(row, certSn);

        return BusinessKeyProvisionResult.builder()
                .requestId(cmd.getRequestId())
                .keyId(row.getKeyId())
                .businessKeyVersion(row.getBusinessKeyVersion())
                .wrappedKeyBase64(wrapped.wrapped() != null ? java.util.Base64.getEncoder().encodeToString(wrapped.wrapped()) : null)
                .algorithm(row.getAlgorithm() != null ? row.getAlgorithm() : wrapped.algorithm())
                .keySpec(row.getKeySpec())
                .validFrom(row.getValidFrom())
                .validTo(row.getValidTo())
                .parameters(wrapped.parameters())
                .state(row.getKeyState().getValue())
                .reused(reused)
                .build();
    }

    protected void persistLastRecipientCert(DeviceBusinessKey row, String certSn) {
        tx().executeWithoutResult(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(row.getId());
            if (current == null) {
                return;
            }
            current.setLastRecipientCertSn(certSn);
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
        });
    }

    private BusinessKeyCreateRequest buildCreateRequest(BusinessKeyProvisionCmd cmd, BoundDeviceIdentity identity,
                                                        BusinessKeyPolicyEntry policy) {
        BizType bizType = bizTypeMapper.resolveBizType(cmd.getBusinessDomain());
        KeyMaterialPolicy materialPolicy = KeyMaterialPolicy.of(policy.algorithm(), policy.keySpec());
        Map<String, String> auditContext = new HashMap<>();
        auditContext.put("deviceSn", cmd.getDeviceSn());
        auditContext.put("businessDomain", cmd.getBusinessDomain());
        auditContext.put("purpose", cmd.getPurpose());
        auditContext.put("bindingId", String.valueOf(identity.bindingId()));
        auditContext.put("hsmUid", identity.hsmUid());
        return new BusinessKeyCreateRequest(bizType, materialPolicy, cmd.getRequestId(), auditContext);
    }

    private Map<String, String> buildAuditContext(BusinessKeyProvisionCmd cmd, String certSn) {
        Map<String, String> auditContext = new HashMap<>();
        auditContext.put("deviceSn", cmd.getDeviceSn());
        auditContext.put("businessDomain", cmd.getBusinessDomain());
        auditContext.put("purpose", cmd.getPurpose());
        auditContext.put("certSn", certSn);
        return auditContext;
    }

    /**
     * 规范化请求摘要（同 requestId 参数冲突识别）
     */
    private String buildDigest(BusinessKeyProvisionCmd cmd) {
        return String.join("|", cmd.getDeviceSn(), cmd.getDeviceCategory() == null ? "" : cmd.getDeviceCategory(),
                cmd.getBusinessDomain(), cmd.getPurpose());
    }

    private void validateCmd(BusinessKeyProvisionCmd cmd) {
        if (cmd == null || StrUtil.isBlank(cmd.getRequestId()) || StrUtil.isBlank(cmd.getDeviceSn())
                || StrUtil.isBlank(cmd.getBusinessDomain()) || StrUtil.isBlank(cmd.getPurpose())) {
            throw new IllegalArgumentException("业务密钥申请参数不完整: requestId/deviceSn/businessDomain/purpose 必填");
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
}
