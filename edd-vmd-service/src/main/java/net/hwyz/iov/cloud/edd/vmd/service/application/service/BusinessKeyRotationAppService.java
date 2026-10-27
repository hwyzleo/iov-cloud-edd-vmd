package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.*;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.DerivedKeyOperationIds;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.VinCanonicalizer;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyMaterialModelMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyAnchor;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 业务密钥轮换应用服务（CR-055 F20 §6.4，CR-061 §9.3 材料路由改造）
 * <p>
 * 编排「锁定旧 ACTIVE → 准备新材料 → 持久化新版本 → 原子切换 ACTIVE」。
 * 按 {@code BizType.materialModel()} <strong>显式</strong>路由（VMD-DSN-CR-061 RD-061-3）：
 * <ul>
 *   <li>{@link KeyMaterialModel#DERIVED_ROOT}：走 {@code KeyProvisioningTemplate.deriveAndWrap}，
 *       回填 {@code materialModel/rootKeyVersion/derivationVersion/operationId}；</li>
 *   <li>{@link KeyMaterialModel#RANDOM_MATERIAL}：保留 CR-055 的 {@code create + wrap} 语义；</li>
 *   <li>{@code null}（不适用）或历史 {@code material_model IS NULL}：fail-closed 拒绝自动轮换。</li>
 * </ul>
 * 任何创建/派生/落库失败都不得提前降级旧 ACTIVE；成功后旧版本转 DEPRECATED（仅解密窗口），
 * 业务版本递增且 KMS 版本不混入信封。
 * <p>
 * <strong>CR-066 §6.5</strong>：轮换必须发生真实密码学代际变化——Provider 返回与当前 ACTIVE 相同 keyId
 * （rootKeyVersion/derivationVersion/anchor 未变）时拒绝激活新行；新行材料落库复用
 * {@link BusinessKeyDerivedMaterialService#persistOrClaimDerivedMaterial}（撞 uk_key_id → claim-or-reuse 收敛），
 * 与批量开通共用同一代际语义与身份一致性校验。
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
    private final BusinessKeyMaterialRouter materialRouter;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final BusinessKeyDerivedMaterialService derivedMaterialService;
    private final BusinessKeyProvisioningMetrics metrics;
    private final ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
    private final PlatformTransactionManager transactionManager;

    @Value("${crypto.provisioning.derived-key.derivation-version:v1}")
    private String derivationVersion = "v1";

    private volatile TransactionTemplate transactionTemplate;

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

        // 3. 显式材料模型路由（禁隐式回退）：仅 DERIVED_ROOT / RANDOM_MATERIAL 可轮换
        BizType bizType = bizTypeMapper.resolveBizType(cmd.getBusinessDomain());
        boolean derivedRoot = materialRouter.isDerivedRoot(bizType);
        boolean randomMaterial = materialRouter.isRandomMaterial(bizType);
        if (!derivedRoot && !randomMaterial) {
            throw new BusinessKeyDerivationRejectedException(
                    "轮换目标材料模型不适用（materialModel=" + materialRouter.materialModel(bizType) + "），拒绝自动轮换: domain="
                            + cmd.getBusinessDomain());
        }

        // 4. 事务内锁定旧 ACTIVE + 分配新版本 + 落 PENDING 新行（串行化并发轮换）
        RotationPlan plan = lockAndPlan(cmd, policy, bizType);
        DeviceBusinessKey oldActive = plan.oldActive;
        DeviceBusinessKey newRow = plan.newRow;

        try {
            if (derivedRoot) {
                return rotateDerivedRoot(cmd, identity, policy, bizType, oldActive, newRow);
            }
            return rotateRandomMaterial(cmd, identity, policy, bizType, oldActive, newRow);
        } catch (RuntimeException e) {
            // 任何失败都不得降级旧 ACTIVE；新行失败已留痕
            log.warn("业务密钥轮换失败（旧ACTIVE保持）: requestId={}, oldKeyId={}", cmd.getRequestId(),
                    oldActive.getKeyId(), e);
            throw e;
        }
    }

    /**
     * 根派生型轮换：deriveAndWrap 一步完成「根派生 + 设备证书公钥封装」（CR-061 §9.3）。
     */
    private BusinessKeyRotationResult rotateDerivedRoot(BusinessKeyRotateCmd cmd, BoundDeviceIdentity identity,
                                                        BusinessKeyPolicyEntry policy, BizType bizType,
                                                        DeviceBusinessKey oldActive, DeviceBusinessKey newRow) {
        KeyAnchor anchor;
        try {
            anchor = resolveAnchor(bizType, identity);
        } catch (RuntimeException e) {
            failNewRow(newRow, "派生锚（VIN/UID）缺失或非法");
            throw new BusinessKeyDerivationRejectedException(
                    "轮换派生锚缺失或非法: requestId=" + cmd.getRequestId());
        }
        ActiveDeviceCertificate activeCert;
        try {
            activeCert = activeDeviceCertificateResolver.resolveActive(identity);
        } catch (RuntimeException e) {
            failNewRow(newRow, "无有效设备证书");
            throw new BusinessKeyKmsUnavailableException("轮换无有效设备证书: requestId=" + cmd.getRequestId());
        }
        KeyProvisioningTemplate template = materialRouter.requireDerivedRoot(bizType);
        String operationId = DerivedKeyOperationIds.operationId(cmd.getRequestId(), cmd.getPurpose(),
                newRow.getBusinessKeyVersion());
        String idemKey = DerivedKeyOperationIds.idempotencyKey(cmd.getRequestId(), cmd.getDeviceSn(),
                cmd.getPurpose(), newRow.getBusinessKeyVersion(), derivationVersion);
        Map<String, String> audit = new HashMap<>();
        audit.put("purpose", cmd.getPurpose());
        audit.put("operation", "ROTATE_DERIVED");

        DerivedWrappedKey result;
        try {
            result = template.deriveAndWrap(
                    bizType,
                    anchor,
                    KeyPurpose.valueOf(cmd.getPurpose()),
                    new RecipientRef.DeviceCertificate(activeCert.certSn(), activeCert.pkiRequestId(), null, null,
                            activeCert.sha256Fingerprint(), audit),
                    DerivationContext.of(derivationVersion, newRow.getBusinessKeyVersion()),
                    operationId,
                    idemKey,
                    cmd.getRequestId(),
                    cmd.getBusinessDomain());
        } catch (CryptoOperationOutcomeUnknownException e) {
            failNewRow(newRow, "framework派生结果未知，待对账");
            throw new BusinessKeyOutcomeUnknownException("轮换派生结果未知: requestId=" + cmd.getRequestId());
        } catch (BusinessKeyDerivedProviderUnavailableException e) {
            failNewRow(newRow, "根派生Provider未就绪");
            throw new BusinessKeyKmsUnavailableException("根派生Provider未就绪: requestId=" + cmd.getRequestId());
        } catch (CryptoDependencyUnavailableException e) {
            failNewRow(newRow, "KMS/HSM或framework安全服务不可用");
            throw new BusinessKeyKmsUnavailableException("KMS/HSM或framework安全服务不可用: requestId=" + cmd.getRequestId());
        } catch (KeyMaterialModelMismatchException e) {
            failNewRow(newRow, truncate(e.getMessage()));
            throw new BusinessKeyDerivationRejectedException("轮换材料模型不匹配，拒绝派生: requestId=" + cmd.getRequestId());
        } catch (CryptoException e) {
            failNewRow(newRow, truncate(e.getMessage()));
            throw new BusinessKeyKmsUnavailableException("轮换派生失败: requestId=" + cmd.getRequestId());
        }

        // CR-066 §6.5：断言返回代际与当前 ACTIVE 不同；同 keyId 说明无真实代际变化，拒绝激活新行
        if (oldActive.getKeyId() != null && oldActive.getKeyId().equals(result.logicalKeyId())) {
            failNewRow(newRow, "轮换未发生真实代际变化（Provider 返回与 ACTIVE 相同 keyId）");
            metrics.recordRetryLoopPrevented("rotation_same_key_id");
            throw new BusinessKeyRotationConflictException(
                    "轮换配置错误：Provider 返回与当前 ACTIVE 相同 keyId，未发生真实代际变化"
                            + "（rootKeyVersion/derivationVersion/anchor 未变），拒绝激活新行");
        }

        // CR-066 §6.5：与批量开通复用 persistOrClaimDerivedMaterial（撞 uk_key_id → claim-or-reuse 收敛）
        Long canonicalId = derivedMaterialService.persistOrClaimDerivedMaterial(
                newRow, result, policy, operationId, null,
                new BusinessKeyDerivedMaterialService.ClaimContext(
                        cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose(),
                        bizType.name(), anchor.value(), cmd.getRequestId()));
        DeviceBusinessKey canonical = canonicalId.equals(newRow.getId())
                ? newRow : deviceBusinessKeyRepository.selectById(canonicalId);
        if (canonical == null) {
            throw new BusinessKeyLogicalIdentityConflict("轮换 canonical 目录行不存在: id=" + canonicalId);
        }

        // 原子切换：旧→DEPRECATED + canonical→ACTIVE（回填派生非敏感字段）+ ROTATED 事件 + 缓存失效
        switchActive(oldActive, canonical, policy, current -> applyDerivedMaterial(current, result, policy, operationId));

        log.info("业务密钥轮换成功（根派生）: requestId={}, oldKeyId={}, newKeyId={}, newVersion={}, canonicalId={}",
                cmd.getRequestId(), oldActive.getKeyId(), result.logicalKeyId(),
                canonical.getBusinessKeyVersion(), canonical.getId());
        return BusinessKeyRotationResult.builder()
                .requestId(cmd.getRequestId())
                .newKeyId(result.logicalKeyId())
                .newBusinessKeyVersion(canonical.getBusinessKeyVersion())
                .oldKeyId(oldActive.getKeyId())
                .oldBusinessKeyVersion(oldActive.getBusinessKeyVersion())
                .state(BusinessKeyState.ACTIVE.getValue())
                .build();
    }

    /**
     * 独立随机材料型轮换：保留 CR-055 create + wrap 验证语义。
     */
    private BusinessKeyRotationResult rotateRandomMaterial(BusinessKeyRotateCmd cmd, BoundDeviceIdentity identity,
                                                           BusinessKeyPolicyEntry policy, BizType bizType,
                                                           DeviceBusinessKey oldActive, DeviceBusinessKey newRow) {
        BusinessKeyMaterial material;
        try {
            material = materialRouter.requireRandomMaterial(bizType).create(buildCreateRequest(cmd, identity, policy));
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

        // 用当前设备证书验证新 keyRef 可 Wrap（仅验证，Wrapped Key 不落库）
        verifyWrap(cmd, identity, bizType, material, newRow);

        // 原子切换：旧→DEPRECATED + 新→ACTIVE（回填材料）+ ROTATED 事件 + 缓存失效
        switchActive(oldActive, newRow, policy, current -> applyRandomMaterial(current, material));

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
    }

    /**
     * 事务内锁定旧 ACTIVE + 分配新版本 + 落 PENDING 新行
     */
    private RotationPlan lockAndPlan(BusinessKeyRotateCmd cmd, BusinessKeyPolicyEntry policy, BizType bizType) {
        KeyMaterialModel materialModel = materialRouter.materialModel(bizType);
        return tx().execute(status -> {
            DeviceBusinessKey oldActive = deviceBusinessKeyRepository.selectActiveByContextForUpdate(
                    cmd.getDeviceSn(), cmd.getBusinessDomain(), cmd.getPurpose());
            if (oldActive == null) {
                throw new BusinessKeyNotExistException(
                        "轮换失败：当前无ACTIVE业务密钥: deviceSn=" + cmd.getDeviceSn()
                                + ", domain=" + cmd.getBusinessDomain() + ", purpose=" + cmd.getPurpose());
            }
            // 历史材料来源未知：禁止自动轮换/重建/迁移（VMD-DSN-CR-061 §7.3）
            if (oldActive.getMaterialModel() == null) {
                throw new BusinessKeyDerivationRejectedException(
                        "历史业务密钥材料来源未知（material_model IS NULL），禁止自动轮换，须走专项迁移批次: keyId="
                                + oldActive.getKeyId());
            }
            // 目录行材料模型与目标业务域不一致：数据异常，fail-closed
            if (!oldActive.getMaterialModel().equals(materialModel.name())) {
                throw new BusinessKeyDerivationRejectedException(
                        "目录行材料模型与目标业务域不一致，禁止自动轮换: keyId=" + oldActive.getKeyId()
                                + ", recordModel=" + oldActive.getMaterialModel()
                                + ", targetModel=" + materialModel.name());
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
                    .materialModel(materialModel.name())
                    .derivationVersion(materialModel == KeyMaterialModel.DERIVED_ROOT ? derivationVersion : null)
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
     * 按 BizType 锚定层级解析派生锚：VEHICLE→规范化 VIN，DEVICE→UID(device_sn)。
     */
    private KeyAnchor resolveAnchor(BizType bizType, BoundDeviceIdentity identity) {
        BizType.Anchor anchorType = bizType.prov().anchor();
        if (anchorType == BizType.Anchor.VEHICLE) {
            return KeyAnchor.vin(VinCanonicalizer.normalizeAndValidate(identity.vin()));
        }
        return KeyAnchor.uid(identity.deviceSn());
    }

    /**
     * 设备证书 Wrap 验证（新 keyRef 可 Wrap 才允许切换）
     */
    private void verifyWrap(BusinessKeyRotateCmd cmd, BoundDeviceIdentity identity, BizType bizType,
                            BusinessKeyMaterial material, DeviceBusinessKey newRow) {
        ActiveDeviceCertificate activeCert = activeDeviceCertificateResolver.resolveActive(identity);
        try {
            materialRouter.requireRandomMaterial(bizType).wrap(
                    new BusinessKeyRef(material.keyId(), material.kmsKeyRef()),
                    // CR-060 §4.2：轮换必须使用当前 active 证书的 serial + requestId 直接证据
                    new RecipientRef.DeviceCertificate(activeCert.certSn(), activeCert.pkiRequestId(), null, null,
                            activeCert.sha256Fingerprint(),
                            Map.of("deviceSn", cmd.getDeviceSn(), "purpose", "ROTATE_VERIFY")),
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
    private void switchActive(DeviceBusinessKey oldActive, DeviceBusinessKey newRow, BusinessKeyPolicyEntry policy,
                              Consumer<DeviceBusinessKey> materialApplier) {
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
            materialApplier.accept(newCurrent);
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
     * 回填根派生非敏感材料（Wrapped Key/KCV/派生明文均不落库）。
     */
    private void applyDerivedMaterial(DeviceBusinessKey current, DerivedWrappedKey result,
                                      BusinessKeyPolicyEntry policy, String fallbackOperationId) {
        current.setKeyId(result.logicalKeyId());
        current.setKmsKeyRef(result.kmsKeyRef());
        current.setRootKeyVersion(result.rootKeyVersion());
        current.setDerivationVersion(result.derivationVersion() != null ? result.derivationVersion() : derivationVersion);
        current.setOperationId(result.operationId() != null ? result.operationId() : fallbackOperationId);
        current.setAlgorithm(policy.algorithm());
        current.setKeySpec(policy.keySpec());
    }

    /**
     * 回填随机材料字段。
     */
    private void applyRandomMaterial(DeviceBusinessKey current, BusinessKeyMaterial material) {
        current.setKeyId(material.keyId());
        current.setKmsKeyRef(material.kmsKeyRef());
        current.setKmsKeyVersion(material.kmsKeyVersion());
        current.setKmsProvider(material.provider());
        current.setAlgorithm(material.algorithm());
        current.setKeySpec(material.keySpec());
        current.setValidFrom(toLocalDateTime(material.validFrom()));
        current.setValidTo(toLocalDateTime(material.validTo()));
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
