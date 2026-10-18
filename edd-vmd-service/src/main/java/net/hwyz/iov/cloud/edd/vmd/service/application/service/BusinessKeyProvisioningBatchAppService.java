package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.DerivedKeyOperationIds;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.VinCanonicalizer;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningIntent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateApplicationRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateMaterialUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateNotReadyException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusConfigurationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusProtocolException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusTrustValidationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyMaterialModelMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyRevokedException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyAnchor;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RecipientRef;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.protobuf.ByteString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务密钥多用途 V2C 开通应用服务（CR-058 F20 Request 段，CR-061 根派生改造）。
 * <p>
 * V2C 材料由随机模型切换为 <strong>Transit/HSM 根派生</strong>（VMD-DSN-CR-061 / FW-SEC-DSN-CR-013）：
 * 以已认证 hsmUid 定位 active 绑定 → 解析唯一有效 VIN 并规范化 → 对 {@code UPLINK_AEAD}/
 * {@code DOWNLINK_AEAD} 分别调用 {@link KeyProvisioningTemplate#deriveAndWrap}，Transit/HSM
 * {@code v2c-comm-root} 按规范化 VIN + purpose + 派生版本确定性派生并在受控边界内封装。
 * <p>
 * VMD 只持久化非敏感字段（logicalKeyId/kmsKeyRef/materialModel/rootKeyVersion/derivationVersion/
 * operationId）；Wrapped Key 仅进当前响应；禁止 VMD 直连 Provider wire 或自行组合 hmac+encryptWith。
 * 完成设备导入 ACK 前不发布目录 ACTIVE 事件（由 ACK 应用服务承接）。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningBatchAppService {

    private static final String WRAP_MODE = "DEVICE_CERT_PUBLIC_KEY";
    private static final String MATERIAL_MODEL_DERIVED_ROOT = KeyMaterialModel.DERIVED_ROOT.name();

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final BusinessKeyPolicyRegistry policyRegistry;
    private final BusinessKeyBizTypeMapper bizTypeMapper;
    private final BusinessKeyMaterialRouter materialRouter;
    private final BusinessKeyProvisioningDomainService domainService;
    private final PlatformTransactionManager transactionManager;

    @Value("${vmd.business-key.provisioning.bundle-ttl-seconds:3600}")
    private long bundleTtlSeconds = 3600;

    @Value("${crypto.provisioning.derived-key.derivation-version:v1}")
    private String derivationVersion = "v1";

    private volatile TransactionTemplate transactionTemplate;

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
     * 处理 canonical KeyProvisioningRequest（V2C 根派生）。
     */
    public KeyProvisioningResponse provision(KeyProvisioningRequest request, KeyprovAuthenticatedContext ctx) {
        String requestId = request.getRequestId();
        if (ctx == null || !ctx.hasIdentity()) {
            return rejected(requestId, KeyprovConstants.ERR_PROTOCOL_INVALID, false, "缺少已认证会话身份");
        }
        String domain = domainService.upper(request.getBusinessDomain());
        Set<String> requestPurposes = domainService.normalizePurposes(request);
        Set<String> policyPurposes = policyRegistry.resolvePurposes(domain).orElse(Set.of());

        if (!KeyprovConstants.BUSINESS_DOMAIN_V2C.equals(domain)
                || !KeyprovConstants.isCompleteV2cPurposeSet(requestPurposes)
                || !domainService.itemsComplete(requestPurposes, policyPurposes)) {
            log.warn("keyprov V2C 用途集合不完整或未授权: requestId={}, domain={}, purposes={}",
                    requestId, domain, requestPurposes);
            return rejected(requestId, KeyprovConstants.ERR_PURPOSE_INCOMPLETE, false, "V2C用途集合不完整或未授权");
        }

        BoundDeviceIdentity identity;
        try {
            identity = boundDeviceIdentityResolver.resolveByHsmUid(ctx.hsmUid(), null);
        } catch (RuntimeException e) {
            log.warn("keyprov 设备身份解析失败: requestId={}, hsmUid摘要={}", requestId, domainService.digestHsmUid(ctx.hsmUid()), e);
            return rejected(requestId, KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "设备身份或active绑定不可用");
        }

        // CR-061 RD-061-2：解析唯一有效 VIN 并规范化，在任何 framework 调用前 fail-closed
        String normalizedVin;
        try {
            normalizedVin = VinCanonicalizer.normalizeAndValidate(identity.vin());
        } catch (RuntimeException e) {
            log.warn("keyprov VIN 绑定缺失或非法（fail-closed，framework调用0次）: requestId={}, deviceSn摘要={}, type={}",
                    requestId, domainService.sha256HexPrefix(identity.deviceSn()), e.getClass().getSimpleName());
            return rejected(requestId, KeyprovConstants.ERR_VIN_BINDING_INVALID, false, "无唯一有效VIN绑定或VIN非法，安全拒绝");
        }

        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            try {
                authorizationPolicy.authorize(BusinessKeyAction.PROVISION, identity.deviceCategory(), domain, purpose);
            } catch (RuntimeException e) {
                log.warn("keyprov 授权拒绝: requestId={}, domain={}, purpose={}", requestId, domain, purpose);
                return rejected(requestId, KeyprovConstants.ERR_PURPOSE_INCOMPLETE, false, "业务域或用途未授权");
            }
        }

        ActiveDeviceCertificate activeCert;
        try {
            activeCert = activeDeviceCertificateResolver.resolveActive(identity);
        } catch (RuntimeException e) {
            log.warn("keyprov 无有效设备证书: requestId={}, deviceSn={}", requestId, identity.deviceSn());
            return rejected(requestId, KeyprovConstants.ERR_CERTIFICATE_INVALID, false, "有效设备证书不存在");
        }

        String suite = domainService.negotiateWrappingSuite(request, KeyprovConstants.DEFAULT_SUPPORTED_SUITES);
        if (suite == null) {
            return rejected(requestId, KeyprovConstants.ERR_PROTOCOL_INVALID, false, "无受支持的封装套件");
        }

        String hsmDigest = domainService.digestHsmUid(identity.hsmUid());
        String vinDigest = domainService.sha256Hex(domainService.utf8(normalizedVin));
        String fingerprintDigest = activeCert.sha256Fingerprint() == null
                ? null : domainService.sha256Hex(domainService.utf8(activeCert.sha256Fingerprint()));
        String requestDigest = domainService.buildRequestDigest(request, hsmDigest, vinDigest, derivationVersion, fingerprintDigest);

        // 幂等：同 requestId 复用原批次与版本
        BusinessKeyProvisioning existing = provisioningRepository.selectByRequestId(requestId);
        BusinessKeyProvisioning batch = existing;
        if (existing != null) {
            if (existing.getRequestDigest() != null && !existing.getRequestDigest().equals(requestDigest)) {
                log.warn("keyprov requestId 摘要冲突: requestId={}", requestId);
                return conflict(requestId, KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT, "同requestId不同请求摘要");
            }
            if (existing.getStatus() != BusinessKeyProvisioningState.COMPLETED && !existing.getStatus().canAdvance()) {
                return rejected(requestId, KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次状态不允许继续申请");
            }
        } else {
            batch = createOrLoadBatch(requestId, identity, domain, hsmDigest, activeCert.certSn(), requestDigest);
            if (batch == null) {
                return rejected(requestId, KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "并发开通冲突，请以原requestId重试");
            }
            // 唯一未终结批次约束（uk_device_domain_open）：同设备+业务域存在其他 requestId 的未终结批次时，
            // 不得以新 requestId 推进旧批次——否则 item/版本会聚合到旧批次、响应按新 requestId 取批次为空（fail-closed 拒绝）。
            if (!requestId.equals(batch.getRequestId())) {
                log.warn("keyprov 同设备业务域已存在未终结批次，拒绝新requestId: requestId={}, openRequestId={}, deviceSn={}, domain={}",
                        requestId, batch.getRequestId(), identity.deviceSn(), domain);
                return rejected(requestId, KeyprovConstants.ERR_STATE_NOT_ALLOWED, false,
                        "同设备业务域存在未终结批次，请以原requestId继续");
            }
        }

        Map<String, DerivedWrappedKey> derived;
        try {
            derived = prepareAndDerive(batch, request, identity, domain, normalizedVin, activeCert, requestId, fingerprintDigest);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov framework 派生/Wrap 结果未知（转待对账，以原requestId对账）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage(), e);
            markBatchReconcile(batch, "framework派生/Wrap结果未知");
            return retryable(requestId, KeyprovConstants.ERR_UNAVAILABLE, "framework结果未知，需以原requestId对账");
        } catch (BusinessKeyDerivedProviderUnavailableException e) {
            log.warn("keyprov 根派生门面未就绪（可同requestId重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage(), e);
            recordDependencyBlocked(batch, truncate(e.getMessage()));
            return retryable(requestId, KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY, "根派生/Transit根能力未就绪");
        } catch (CryptoDependencyUnavailableException e) {
            log.warn("keyprov 根派生依赖不可用（可同requestId重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage(), e);
            recordDependencyBlocked(batch, truncate(e.getMessage()));
            return retryable(requestId, KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY, "根派生依赖不可用");
        } catch (BusinessKeyDerivationRejectedException e) {
            log.warn("keyprov 派生被拒（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, false, "派生参数/材料模型/锚不受支持，拒绝派生");
        } catch (KeyMaterialModelMismatchException e) {
            log.warn("keyprov 材料模型不匹配（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, false, "材料模型不匹配，拒绝派生");
        } catch (CertificateMaterialUnavailableException | CertificateStatusDependencyUnavailableException
                 | CertificateStatusConfigurationException | CertificateStatusOutcomeUnknownException e) {
            // 证书材料/locator 暂不可取、证书状态依赖不可用：确定性可重试（由 KeyProvisioningApplicationFacade
            // 映射为 806101/806105，action=RETRY_SAME_REQUEST_ID）。批次保持未终结、目标版本保持 PENDING，
            // 供车端以原 requestId 重试（VMD-DSN-CR-060 §6.2）——异常上抛交门面统一映射，不在本层降级为派生错误。
            log.warn("keyprov 证书材料/状态依赖不可用（可同requestId重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            recordDependencyBlocked(batch, truncate(e.getMessage()));
            throw e;
        } catch (CertificateEvidenceMismatchException | CertificateReferenceAmbiguousException
                 | CertificateNotReadyException | CertificateProfileNotAllowedException
                 | KeyRevokedException | CertificateStatusTrustValidationException
                 | CertificateApplicationRejectedException | CertificateStatusProtocolException e) {
            // 证书证据冲突/目录多命中/信任链/KU-EKU/Profile/吊销/状态协议：不可重试（门面映射为 806102～806104，
            // action=DO_NOT_RETRY），批次终态化避免同 requestId 盲重试。异常上抛交门面统一映射，
            // 避免被下方 catch(CryptoException) 一律降级为 806108「派生不受支持」而丢失真实根因（CR-060 §6.2）。
            log.warn("keyprov 证书证据/策略拒绝（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            throw e;
        } catch (CryptoException e) {
            log.warn("keyprov 业务密钥派生或封装失败（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, false, "业务密钥派生或封装失败");
        }
        return buildResponse(provisioningRepository.selectByRequestId(requestId), identity, request,
                activeCert, suite, derived);
    }

    private BusinessKeyProvisioning createOrLoadBatch(String requestId, BoundDeviceIdentity identity, String domain,
                                                      String hsmDigest, String certSn, String requestDigest) {
        try {
            return tx().execute(status -> {
                BusinessKeyProvisioning batch = BusinessKeyProvisioning.builder()
                        .requestId(requestId)
                        .requestDigest(requestDigest)
                        .deviceSn(identity.deviceSn())
                        .bindingId(identity.bindingId())
                        .partId(identity.partId())
                        .hsmUidDigest(hsmDigest)
                        .certificateSnDigest(domainService.sha256Hex(domainService.utf8(certSn)))
                        .businessDomain(domain)
                        .status(BusinessKeyProvisioningState.PREPARING)
                        .startedAt(LocalDateTime.now())
                        .createTime(LocalDateTime.now())
                        .build();
                batch.init();
                provisioningRepository.insert(batch);
                return batch;
            });
        } catch (DuplicateKeyException e) {
            BusinessKeyProvisioning raced = provisioningRepository.selectByRequestId(requestId);
            if (raced != null) {
                return raced;
            }
            BusinessKeyProvisioning open = provisioningRepository.selectOpenByDeviceAndDomain(identity.deviceSn(), domain);
            if (open != null) {
                log.warn("keyprov 并发开放批次：复用现有批次 requestId={}", open.getRequestId());
                return open;
            }
            log.warn("keyprov 批次并发冲突且无开放批次: requestId={}", requestId);
            return null;
        }
    }

    /**
     * 逐用途准备目标版本并执行根派生封装（VMD-DSN-CR-061 RD-061-3）。
     *
     * @return purpose → 派生封装结果（Wrapped Key 仅用于本次响应）
     */
    private Map<String, DerivedWrappedKey> prepareAndDerive(BusinessKeyProvisioning batch, KeyProvisioningRequest request,
                                                            BoundDeviceIdentity identity, String domain, String normalizedVin,
                                                            ActiveDeviceCertificate activeCert, String requestId,
                                                            String fingerprintDigest) {
        BizType bizType = bizTypeMapper.resolveBizType(domain);
        // 显式路由：非 DERIVED_ROOT 一律拒绝（禁隐式回退到随机材料）
        KeyProvisioningTemplate template = materialRouter.requireDerivedRoot(bizType);
        KeyAnchor anchor = KeyAnchor.vin(normalizedVin);
        DerivationContext context = DerivationContext.of(derivationVersion);

        Map<String, Long> currentVersions = domainService.extractCurrentVersions(request);
        Map<String, DerivedWrappedKey> derived = new HashMap<>();

        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            BusinessKeyProvisioningItem item = provisioningItemRepository
                    .selectByProvisioningIdAndPurpose(batch.getId(), purpose);
            DeviceBusinessKey target;
            boolean newItem;
            if (item != null) {
                target = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
                if (target == null) {
                    throw new BusinessKeyDerivationRejectedException(
                            "开通批次 item 对应目录版本不存在: purpose=" + purpose);
                }
                newItem = false;
            } else {
                // 依赖不可用等原因以原 requestId 重试：复用本请求已占位的 PENDING 目标版本，
                // 保持 businessKeyVersion/operationId/幂等键稳定（Provider 侧幂等命中同一逻辑材料），
                // 避免重试遗留孤儿 PENDING 与版本跳号（VMD-DSN-CR-061 RD-061-5）。
                DeviceBusinessKey reserved = deviceBusinessKeyRepository
                        .selectByRequestIdAndPurpose(requestId, purpose);
                if (reserved != null && reserved.getKeyState() == BusinessKeyState.PENDING) {
                    target = reserved;
                    newItem = true;
                } else {
                    target = acquireNewTarget(identity, domain, purpose, requestId, currentVersions);
                    newItem = true;
                }
            }

            // 历史材料来源未知（material_model IS NULL）：禁止自动派生/迁移/轮换
            if (target.getMaterialModel() == null) {
                throw new BusinessKeyDerivationRejectedException(
                        "历史业务密钥材料来源未知（material_model IS NULL），禁止自动派生或迁移: purpose=" + purpose);
            }
            if (!MATERIAL_MODEL_DERIVED_ROOT.equals(target.getMaterialModel())) {
                throw new BusinessKeyDerivationRejectedException(
                        "目标版本材料模型不是根派生型，禁止 V2C 派生: purpose=" + purpose
                                + ", materialModel=" + target.getMaterialModel());
            }

            BusinessKeyPolicyEntry policy = authorizationPolicy.authorize(
                    BusinessKeyAction.PROVISION, identity.deviceCategory(), domain, purpose);
            String operationId = DerivedKeyOperationIds.operationId(requestId, purpose, target.getBusinessKeyVersion());
            String idemKey = DerivedKeyOperationIds.idempotencyKey(requestId, identity.deviceSn(), purpose,
                    target.getBusinessKeyVersion(), derivationVersion);
            Map<String, String> audit = new HashMap<>();
            audit.put("purpose", purpose);
            audit.put("operation", "KEYPROV_DERIVED");

            DerivedWrappedKey result = template.deriveAndWrap(
                    bizType,
                    anchor,
                    KeyPurpose.valueOf(purpose),
                    new RecipientRef.DeviceCertificate(activeCert.certSn(), activeCert.pkiRequestId(), null, null,
                            activeCert.sha256Fingerprint(), audit),
                    context,
                    operationId,
                    idemKey);

            if (newItem) {
                if (target.getKeyId() == null) {
                    target = persistDerivedMaterialKeepingPending(target, result, policy, operationId);
                }
                insertItem(batch.getId(), target, purpose, result, fingerprintDigest);
            }
            derived.put(purpose, result);
        }

        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = provisioningRepository.selectById(batch.getId());
            if (current == null || current.getStatus() != BusinessKeyProvisioningState.PREPARING) {
                return;
            }
            current.markWaitDeviceAck();
            current.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(current);
        });
        log.info("keyprov 批次进入WAIT_DEVICE_ACK（根派生）: requestId={}, deviceSn={}, domain={}",
                batch.getRequestId(), identity.deviceSn(), domain);
        return derived;
    }

    /**
     * 为新用途解析目标目录版本：复用既有 ACTIVE（REIMPORT）或占位新 PENDING 版本。
     * <p>
     * 历史 {@code material_model IS NULL} 的 ACTIVE 禁止自动轮换/重建/迁移（VMD-DSN-CR-061 §7.3）。
     */
    private DeviceBusinessKey acquireNewTarget(BoundDeviceIdentity identity, String domain, String purpose,
                                               String requestId, Map<String, Long> currentVersions) {
        DeviceBusinessKey active = deviceBusinessKeyRepository.selectActiveByContext(identity.deviceSn(), domain, purpose);
        // 历史 ACTIVE 材料来源未知：禁止自动轮换/重建/迁移（VMD-DSN-CR-061 §7.3）
        if (active != null && active.getMaterialModel() == null) {
            throw new BusinessKeyDerivationRejectedException(
                    "历史ACTIVE业务密钥材料来源未知（material_model IS NULL），须走专项迁移批次，禁止自动轮换/重建: purpose="
                            + purpose);
        }
        BusinessKeyProvisioningIntent intent = domainService.deriveIntent(active,
                currentVersions.getOrDefault(purpose, 0L));
        if (intent == BusinessKeyProvisioningIntent.REIMPORT && active != null) {
            // 复用既有 ACTIVE（同逻辑材料，收方/封装不同），不新建版本
            return active;
        }
        return createPendingVersion(identity, domain, purpose, requestId);
    }

    private DeviceBusinessKey createPendingVersion(BoundDeviceIdentity identity, String domain, String purpose,
                                                   String requestId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Long nextVersion = deviceBusinessKeyRepository.maxBusinessKeyVersion(
                    identity.deviceSn(), domain, purpose) + 1;
            DeviceBusinessKey row = DeviceBusinessKey.builder()
                    .deviceSn(identity.deviceSn())
                    .bindingId(identity.bindingId())
                    .partId(identity.partId())
                    .hsmUid(identity.hsmUid())
                    .businessDomain(domain)
                    .purpose(purpose)
                    .materialModel(MATERIAL_MODEL_DERIVED_ROOT)
                    .businessKeyVersion(nextVersion)
                    .keyState(BusinessKeyState.PENDING)
                    .wrapMode(WRAP_MODE)
                    .requestId(requestId)
                    .derivationVersion(derivationVersion)
                    .createTime(LocalDateTime.now())
                    .build();
            try {
                deviceBusinessKeyRepository.insert(row);
                return row;
            } catch (DuplicateKeyException e) {
                log.warn("keyprov 目标版本占位冲突，重试: deviceSn={}, purpose={}, version={}",
                        identity.deviceSn(), purpose, nextVersion);
            }
        }
        throw new net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRotationConflictException(
                "keyprov目标版本占位并发冲突: deviceSn=" + identity.deviceSn() + ", purpose=" + purpose);
    }

    /**
     * 回填根派生非敏感材料并保持 PENDING（Wrapped Key/KCV/派生明文均不落库）。
     */
    private DeviceBusinessKey persistDerivedMaterialKeepingPending(DeviceBusinessKey row, DerivedWrappedKey result,
                                                                   BusinessKeyPolicyEntry policy, String operationId) {
        return tx().execute(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(row.getId());
            if (current == null) {
                throw new IllegalStateException("keyprov目标版本行不存在: id=" + row.getId());
            }
            current.setKeyId(result.logicalKeyId());
            current.setKmsKeyRef(result.kmsKeyRef());
            current.setRootKeyVersion(result.rootKeyVersion() == null ? null : result.rootKeyVersion().longValue());
            current.setDerivationVersion(result.derivationVersion() != null ? result.derivationVersion() : derivationVersion);
            current.setOperationId(result.operationId() != null ? result.operationId() : operationId);
            current.setAlgorithm(policy.algorithm());
            current.setKeySpec(policy.keySpec());
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
            return current;
        });
    }

    private void insertItem(Long provisioningId, DeviceBusinessKey target, String purpose, DerivedWrappedKey result,
                            String fingerprintDigest) {
        tx().executeWithoutResult(status -> provisioningItemRepository.insert(BusinessKeyProvisioningItem.builder()
                .provisioningId(provisioningId)
                .purpose(purpose)
                .keyId(target.getKeyId())
                .businessKeyVersion(target.getBusinessKeyVersion())
                .directoryKeyId(target.getId())
                .operationId(result.operationId())
                .rootKeyVersion(result.rootKeyVersion() == null ? null : result.rootKeyVersion().longValue())
                .derivationVersion(result.derivationVersion())
                .recipientCertFingerprintDigest(fingerprintDigest)
                .deviceResult(ProvisioningDeviceResult.PENDING)
                .createTime(LocalDateTime.now())
                .build()));
    }

    private KeyProvisioningResponse buildResponse(BusinessKeyProvisioning batch, BoundDeviceIdentity identity,
                                                  KeyProvisioningRequest request, ActiveDeviceCertificate activeCert,
                                                  String suite, Map<String, DerivedWrappedKey> derived) {
        List<BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningId(batch.getId());
        List<WrappedKeyItem> wrappedItems = new ArrayList<>();
        for (BusinessKeyProvisioningItem item : items) {
            DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
            if (directory == null || directory.getKeyId() == null) {
                continue;
            }
            DerivedWrappedKey wrapped = derived.get(item.getPurpose());
            if (wrapped == null || wrapped.wrapped() == null) {
                continue;
            }
            wrappedItems.add(toWrappedItem(item.getPurpose(), directory, wrapped, suite));
        }
        long expireAt = request.getExpireAtMs() > 0 ? request.getExpireAtMs() : defaultBundleExpireAt();
        KeyProvisioningResponse.Builder builder = KeyProvisioningResponse.newBuilder()
                .setRequestId(batch.getRequestId())
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                .setExpireAtMs(expireAt);
        wrappedItems.sort((a, b) -> a.getPurposeValue() - b.getPurposeValue());
        wrappedItems.forEach(builder::addItems);
        return builder.build();
    }

    private WrappedKeyItem toWrappedItem(String purpose, DeviceBusinessKey directory, DerivedWrappedKey wrapped,
                                         String suite) {
        WrappedKeyItem.Builder builder = WrappedKeyItem.newBuilder()
                .setPurpose(KeyprovConstants.purposeProto(purpose))
                .setKeyId(directory.getKeyId())
                .setBusinessKeyVersion(directory.getBusinessKeyVersion() == null ? 0 : directory.getBusinessKeyVersion())
                .setKeyAlgorithm(directory.getAlgorithm() == null ? "" : directory.getAlgorithm())
                .setWrappingSuite(KeyprovConstants.suiteProto(suite));
        if (wrapped.wrapped() != null) {
            builder.setWrappedKey(ByteString.copyFrom(wrapped.wrapped()));
        }
        if (directory.getValidFrom() != null) {
            builder.setValidFromMs(toEpochMs(directory.getValidFrom()));
        }
        if (directory.getValidTo() != null) {
            builder.setValidToMs(toEpochMs(directory.getValidTo()));
        }
        return builder.build();
    }

    /**
     * 记录确定性可重试的依赖不可用原因：批次保持未终结（PREPARING）、目标版本保持 PENDING，
     * 以便车端按 {@code RETRY_SAME_REQUEST_ID} 以原 requestId 重新派生（VMD-DSN-CR-061 RD-061-5）。
     * <p>
     * 不终态化、不置目标版本 FAILED：若终态化，同 requestId 重试将因批次不可推进被拒（806094），
     * 车端陷入死循环；若置 FAILED，重试复用的目标版本将无法再推进到 ACTIVE。
     */
    private void recordDependencyBlocked(BusinessKeyProvisioning batch, String reason) {
        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = provisioningRepository.selectById(batch.getId());
            if (current == null || !current.getStatus().isOpen()) {
                return;
            }
            current.noteDependencyBlocked(reason);
            current.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(current);
        });
    }

    private void markBatchReconcile(BusinessKeyProvisioning batch, String reason) {
        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = provisioningRepository.selectById(batch.getId());
            if (current != null && current.getStatus().isOpen()) {
                current.markReconcileRequired(reason);
                current.setModifyTime(LocalDateTime.now());
                provisioningRepository.update(current);
            }
        });
    }

    private void markBatchFailed(BusinessKeyProvisioning batch, String reason) {
        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = provisioningRepository.selectById(batch.getId());
            if (current == null || !current.getStatus().isOpen()) {
                return;
            }
            // 1) 已建 item 的目标版本置 FAILED；item 置 FAILED（本批次新建 PENDING 目标版本）
            for (BusinessKeyProvisioningItem item : provisioningItemRepository.selectByProvisioningId(current.getId())) {
                markPendingKeyFailed(deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId()), reason);
                if (item.getDeviceResult() == ProvisioningDeviceResult.PENDING) {
                    item.markFailed(null);
                    item.setModifyTime(LocalDateTime.now());
                    provisioningItemRepository.update(item);
                }
            }
            // 2) 失败发生在 item 落库前（如派生前）时的兜底：本批次已占位 PENDING 目标版本按
            //    requestId+purpose 置 FAILED，避免遗留孤儿 PENDING 阻断后续重试/版本推进
            for (String purpose : KeyprovConstants.V2C_PURPOSES) {
                markPendingKeyFailed(deviceBusinessKeyRepository.selectByRequestIdAndPurpose(current.getRequestId(), purpose),
                        reason);
            }
            current.markFailed(reason);
            current.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(current);
        });
    }

    private void markPendingKeyFailed(DeviceBusinessKey directory, String reason) {
        if (directory != null && directory.getKeyState() == BusinessKeyState.PENDING) {
            directory.markFailed(reason);
            directory.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(directory);
        }
    }

    private KeyProvisioningResponse rejected(String requestId, String code, boolean retryable, String message) {
        return responseWithError(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, code, retryable, message);
    }

    private KeyProvisioningResponse retryable(String requestId, String code, String message) {
        return responseWithError(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, code, true, message);
    }

    private KeyProvisioningResponse conflict(String requestId, String code, String message) {
        return responseWithError(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT, code, false, message);
    }

    private KeyProvisioningResponse responseWithError(String requestId, KeyProvisioningResult result,
                                                      String code, boolean retryable, String message) {
        return KeyProvisioningResponse.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setResult(result)
                .setError(ProtocolError.newBuilder()
                        .setCode(code == null ? "" : code)
                        .setRetryable(retryable)
                        .setAction(retryable ? "RETRY_SAME_REQUEST_ID" : "DO_NOT_RETRY")
                        .setMessage(message == null ? "" : truncate(message))
                        .build())
                .build();
    }

    private long defaultBundleExpireAt() {
        return System.currentTimeMillis() + bundleTtlSeconds * 1000L;
    }

    private long toEpochMs(LocalDateTime time) {
        return time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}