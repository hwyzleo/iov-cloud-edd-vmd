package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyLogicalIdentityConflict;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyVersionGenerationMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
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
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
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
import net.hwyz.iov.cloud.framework.security.crypto.exception.DerivedEnvelopeIntegrityException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyMaterialModelMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyRevokedException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKeyEnvelopeView;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
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
 * <strong>CR-066</strong>：目录材料身份与开通 attempt 分离，同逻辑材料复用 canonical 行；
 * item 在 Provider 调用前持久化身份，派生后经 {@link BusinessKeyDerivedMaterialService#persistOrClaimDerivedMaterial}
 * 回填/claim-or-reuse，撞 uk_key_id 不再映射为可重试 806100；声明版本与实际代际不一致、同 keyId 不可变元数据
 * 冲突均为不可重试（806130/806131）。
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
    private final KeyprovEnvelopeAdapter envelopeAdapter;
    private final TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;
    private final BusinessKeyDerivedMaterialService derivedMaterialService;
    private final BusinessKeyProvisioningMetrics metrics;
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
            // CR-063 §10：新 ECDH-ES Request 仅接受逻辑 v1 证书（sign-only 存量 fail-closed，RD-063-5）
            activeCert = activeDeviceCertificateResolver.resolveActiveEcdhV1(identity);
        } catch (RuntimeException e) {
            log.warn("keyprov 无有效 v1 设备证书（sign-only 存量证书对新 ECDH-ES fail-closed）: requestId={}, deviceSn={}",
                    requestId, identity.deviceSn());
            return rejected(requestId, KeyprovConstants.ERR_CERTIFICATE_INVALID, false,
                    "无有效逻辑v1设备证书，新ECDH-ES Keyprov拒绝（sign-only存量证书不适用）");
        }

        // CR-063 §10：证书前置门禁（Profile 启用/leaf digest/目录/FW capability/CR-062 readiness）
        TboxEcdhCertificateEligibilityGate.GateResult gateResult = tboxEcdhCertificateEligibilityGate.evaluate(activeCert);
        if (!gateResult.eligible()) {
            log.warn("keyprov 新 ECDH-ES Request 证书前置门禁拒绝: requestId={}, retryable={}, reason={}",
                    requestId, gateResult.retryable(), gateResult.reason());
            if (gateResult.retryable()) {
                return retryable(requestId, KeyprovConstants.ERR_UNAVAILABLE, gateResult.reason());
            }
            return rejected(requestId, KeyprovConstants.ERR_CERTIFICATE_INVALID, false, gateResult.reason());
        }

        String suite = domainService.negotiateWrappingSuite(request, KeyprovConstants.V2C_SUPPORTED_SUITES);
        if (suite == null) {
            return rejected(requestId, KeyprovConstants.ERR_PROTOCOL_INVALID, false, "无受支持的封装套件");
        }
        // CR-062 §4.1：envelope format 必须由请求显式声明并命中 allowlist，否则不得下发 envelope v1
        if (envelopeAdapter.negotiateEnvelopeFormat(request) == null) {
            log.warn("keyprov 请求未声明受支持的 envelope format/组合，fail-closed 不下发 v1: requestId={}, domain={}",
                    requestId, domain);
            return rejected(requestId, KeyprovConstants.ERR_ENVELOPE_UNSUPPORTED, false,
                    "请求未声明受支持的envelope格式组合，拒绝下发");
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
        } catch (BusinessKeyVersionGenerationMismatchException e) {
            // CR-066 §8：声明的 businessKeyVersion 与实际密码学代际不一致 → 版本代际冲突，不可重试（DO_NOT_RETRY）
            log.warn("keyprov 声明版本与实际代际不一致（不可重试）: requestId={}, cause={}",
                    requestId, e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            metrics.recordRetryLoopPrevented("version_generation_rejected");
            return rejected(requestId, KeyprovConstants.ERR_VERSION_GENERATION_MISMATCH, false,
                    "声明的businessKeyVersion与实际密码学代际不一致，拒绝派生");
        } catch (BusinessKeyLogicalIdentityConflict e) {
            // CR-066 §8：同 keyId 但不可变元数据冲突 → 数据一致性/安全冲突，不可重试；
            // 禁止落可重试 806100（否则每次重试同一 keyId 形成确定性死循环）
            log.warn("keyprov 同keyId不可变元数据冲突（不可重试）: requestId={}, cause={}",
                    requestId, e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            metrics.recordRetryLoopPrevented("identity_conflict_rejected");
            return rejected(requestId, KeyprovConstants.ERR_LOGICAL_KEY_IDENTITY_CONFLICT, false,
                    "同keyId材料身份冲突，数据一致性/安全冲突，禁止自动重试");
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
        } catch (KeyprovEnvelopeIntegrityException | DerivedEnvelopeIntegrityException e) {
            log.warn("keyprov 信封完整性拒绝（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_ENVELOPE_INTEGRITY, false,
                    "envelope摘要/版本校验失败，拒绝下发");
        } catch (CryptoException e) {
            log.warn("keyprov 业务密钥派生或封装失败（不可重试）: requestId={}, cause={}: {}",
                    requestId, e.getClass().getSimpleName(), e.getMessage());
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, false, "业务密钥派生或封装失败");
        }
        return buildResponse(provisioningRepository.selectByRequestId(requestId), identity, request,
                activeCert, derived);
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

        Map<String, Long> currentVersions = domainService.extractCurrentVersions(request);
        Map<String, DerivedWrappedKey> derived = new HashMap<>();

        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            BusinessKeyProvisioningItem item = provisioningItemRepository
                    .selectByProvisioningIdAndPurpose(batch.getId(), purpose);
            DeviceBusinessKey target;
            if (item != null) {
                // CR-066 §6.2：既有 item → 复用 directoryKeyId + businessKeyVersion + operationId
                target = resolveItemDirectory(item);
                if (target == null) {
                    throw new BusinessKeyDerivationRejectedException(
                            "开通批次 item 对应目录版本不存在: purpose=" + purpose);
                }
                if (!target.getId().equals(item.getDirectoryKeyId())) {
                    // 占位行已被并发 claim：跟随 canonical 并重挂 item（防御性）
                    provisioningItemRepository.rebindDirectoryKey(item.getId(), target.getId(),
                            target.getKeyId(), target.getBusinessKeyVersion());
                    item.setDirectoryKeyId(target.getId());
                    item.setKeyId(target.getKeyId());
                    item.setBusinessKeyVersion(target.getBusinessKeyVersion());
                }
                recoverTargetIfNeeded(target, requestId);
            } else {
                // 依赖不可用等原因以原 requestId 重试：复用本请求已占位的目标版本，
                // 保持 businessKeyVersion/operationId/幂等键稳定（Provider 侧幂等命中同一逻辑材料），
                // 避免重试遗留孤儿 PENDING 与版本跳号（VMD-DSN-CR-061 RD-061-5）。
                DeviceBusinessKey reserved = deviceBusinessKeyRepository
                        .selectByRequestIdAndPurpose(requestId, purpose);
                if (reserved != null && isRecoverableReserved(reserved)) {
                    target = reserved;
                    recoverTargetIfNeeded(target, requestId);
                } else {
                    target = resolveNewTarget(identity, domain, purpose, requestId, currentVersions);
                }
            }

            // 材料模型门禁（历史来源未知/非根派生禁止自动派生/迁移/轮换）
            assertDerivedRow(target, purpose);

            BusinessKeyPolicyEntry policy = authorizationPolicy.authorize(
                    BusinessKeyAction.PROVISION, identity.deviceCategory(), domain, purpose);

            if (item == null) {
                // CR-066 §6.2：Provider 调用前持久化 item 身份（directoryKeyId + 版本 + operationId）
                String preOperationId = DerivedKeyOperationIds.operationId(requestId, purpose,
                        target.getBusinessKeyVersion());
                item = insertItemIdentity(batch.getId(), target, purpose, preOperationId);
            }

            // 同 requestId 绝不重新分配目录行、版本或 operationId
            String operationId = item.getOperationId() != null ? item.getOperationId()
                    : DerivedKeyOperationIds.operationId(requestId, purpose, target.getBusinessKeyVersion());
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
                    // CR-062/FW-SEC CR-014：业务版本参与 Provider 幂等摘要，必须显式传入目标版本
                    DerivationContext.of(derivationVersion, target.getBusinessKeyVersion()),
                    operationId,
                    idemKey);

            // CR-066 §6.3：persistOrClaimDerivedMaterial（撞 uk_key_id → 确定性 claim-or-reuse，不再落可重试 806100）
            Long canonicalId = derivedMaterialService.persistOrClaimDerivedMaterial(
                    target, result, policy, operationId, item.getId(),
                    new BusinessKeyDerivedMaterialService.ClaimContext(
                            identity.deviceSn(), domain, purpose, bizType.name(), normalizedVin, requestId));
            // 始终从 DB 重读 canonical 行（attach 路径以 SQL 回填，内存对象可能仍为旧值）
            DeviceBusinessKey canonical = deviceBusinessKeyRepository.selectById(canonicalId);
            if (canonical == null) {
                throw new BusinessKeyLogicalIdentityConflict(
                        "canonical 目录行不存在: id=" + canonicalId + ", purpose=" + purpose);
            }
            // CR-066 §6.2：保存 item 元数据（envelope/KCV/root/derivation/收件指纹）
            updateItemMetadata(item, canonical, result, fingerprintDigest, activeCert);
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
     * 为新用途解析目标目录版本（CR-066 §6.1 修订）：同代际复用既有 ACTIVE（REIMPORT），
     * 声明版本与 ACTIVE 不一致但代际相同 → 版本代际冲突（不可重试）；真实代际变化（派生格式变化）→ 新建 PENDING。
     * <p>
     * 历史 {@code material_model IS NULL} 的 ACTIVE 禁止自动轮换/重建/迁移（VMD-DSN-CR-061 §7.3）。
     */
    private DeviceBusinessKey resolveNewTarget(BoundDeviceIdentity identity, String domain, String purpose,
                                               String requestId, Map<String, Long> currentVersions) {
        DeviceBusinessKey active = deviceBusinessKeyRepository.selectActiveByContext(identity.deviceSn(), domain, purpose);
        // 历史 ACTIVE 材料来源未知：禁止自动轮换/重建/迁移（VMD-DSN-CR-061 §7.3）
        if (active != null && active.getMaterialModel() == null) {
            throw new BusinessKeyDerivationRejectedException(
                    "历史ACTIVE业务密钥材料来源未知（material_model IS NULL），须走专项迁移批次，禁止自动轮换/重建: purpose="
                            + purpose);
        }
        BusinessKeyProvisioningIntent intent = domainService.deriveIntent(active,
                currentVersions.getOrDefault(purpose, 0L), derivationVersion);
        metrics.recordVersionDecision(intent.name().toLowerCase(java.util.Locale.ROOT),
                intent == BusinessKeyProvisioningIntent.REIMPORT ? "same_generation_reuse"
                        : intent == BusinessKeyProvisioningIntent.VERSION_GENERATION_MISMATCH
                        ? "declared_version_conflict" : "no_active_or_generation_change");
        if (intent == BusinessKeyProvisioningIntent.REIMPORT && active != null) {
            // 复用既有 ACTIVE（同逻辑材料，收方/封装不同），不新建版本
            return active;
        }
        if (intent == BusinessKeyProvisioningIntent.VERSION_GENERATION_MISMATCH) {
            metrics.recordRetryLoopPrevented("version_generation_rejected");
            throw new BusinessKeyVersionGenerationMismatchException(
                    "设备声明版本与目录 ACTIVE 不一致但密码学代际相同，禁止提升业务版本: purpose=" + purpose
                            + ", declared=" + currentVersions.getOrDefault(purpose, 0L)
                            + ", active=" + (active == null ? null : active.getBusinessKeyVersion()));
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
     * 解析 item 关联的目录行；占位行已被 claim（SUPERSEDED）时跟随 canonical（防御性）。
     */
    private DeviceBusinessKey resolveItemDirectory(BusinessKeyProvisioningItem item) {
        if (item.getDirectoryKeyId() == null) {
            return null;
        }
        DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
        if (directory == null) {
            return null;
        }
        if (directory.getKeyState() == BusinessKeyState.SUPERSEDED && directory.getSupersededById() != null) {
            return deviceBusinessKeyRepository.selectById(directory.getSupersededById());
        }
        return directory;
    }

    /**
     * 本 requestId 预留行是否可复用（CR-066 §6.4）：PENDING / PENDING_RECOVERY，
     * 或 FAILED/RECONCILE_REQUIRED 且已写 keyId 证据（可恢复同一行，不另建行）。
     */
    private boolean isRecoverableReserved(DeviceBusinessKey reserved) {
        BusinessKeyState state = reserved.getKeyState();
        if (state == null) {
            return false;
        }
        return state == BusinessKeyState.PENDING
                || state == BusinessKeyState.PENDING_RECOVERY
                || ((state == BusinessKeyState.FAILED || state == BusinessKeyState.RECONCILE_REQUIRED)
                && reserved.getKeyId() != null);
    }

    /**
     * 目标目录行 FAILED→PENDING_RECOVERY→PENDING 恢复（CR-066 §6.4，同事务；证据缺失时冲突拒绝）。
     */
    private void recoverTargetIfNeeded(DeviceBusinessKey target, String requestId) {
        if (target.getKeyState() == BusinessKeyState.PENDING_RECOVERY) {
            confirmRecovery(target, requestId);
            return;
        }
        if (target.getKeyState() == BusinessKeyState.FAILED) {
            if (target.getKeyId() == null) {
                metrics.recordIdentityConflict("evidence_incomplete");
                throw new BusinessKeyLogicalIdentityConflict(
                        "FAILED 目标版本证据不完整（key_id IS NULL），不可恢复: id=" + target.getId());
            }
            int r1 = deviceBusinessKeyRepository.markPendingRecovery(target.getId(), "CR066_REQUEST_RETRY_RECOVERY");
            if (r1 != 1) {
                metrics.recordIdentityConflict("state_not_reusable");
                throw new BusinessKeyLogicalIdentityConflict(
                        "目标版本恢复入口失败（状态已变）: id=" + target.getId());
            }
            confirmRecovery(target, requestId);
            metrics.recordClaim("reuse", "recovered", "request_retry_recovered");
        }
    }

    private void confirmRecovery(DeviceBusinessKey target, String requestId) {
        int r2 = deviceBusinessKeyRepository.confirmRecovery(target.getId(), requestId);
        if (r2 != 1) {
            metrics.recordIdentityConflict("state_not_reusable");
            throw new BusinessKeyLogicalIdentityConflict(
                    "目标版本恢复确认失败（状态已变）: id=" + target.getId());
        }
    }

    /**
     * 材料模型门禁：历史来源未知（material_model IS NULL）或非根派生型禁止自动派生/迁移/轮换。
     */
    private void assertDerivedRow(DeviceBusinessKey target, String purpose) {
        if (target == null) {
            throw new BusinessKeyDerivationRejectedException("目标目录行不存在: purpose=" + purpose);
        }
        if (target.getMaterialModel() == null) {
            throw new BusinessKeyDerivationRejectedException(
                    "历史业务密钥材料来源未知（material_model IS NULL），禁止自动派生或迁移: purpose=" + purpose);
        }
        if (!MATERIAL_MODEL_DERIVED_ROOT.equals(target.getMaterialModel())) {
            throw new BusinessKeyDerivationRejectedException(
                    "目标版本材料模型不是根派生型，禁止 V2C 派生: purpose=" + purpose
                            + ", materialModel=" + target.getMaterialModel());
        }
    }

    /**
     * Provider 调用前持久化 item 身份（CR-066 §6.2）：directoryKeyId + businessKeyVersion + operationId，
     * keyId 与信封元数据在派生后回填/claim 收敛。
     */
    private BusinessKeyProvisioningItem insertItemIdentity(Long provisioningId, DeviceBusinessKey target,
                                                           String purpose, String operationId) {
        BusinessKeyProvisioningItem item = BusinessKeyProvisioningItem.builder()
                .provisioningId(provisioningId)
                .purpose(purpose)
                .businessKeyVersion(target.getBusinessKeyVersion())
                .directoryKeyId(target.getId())
                .operationId(operationId)
                .deviceResult(ProvisioningDeviceResult.PENDING)
                .createTime(LocalDateTime.now())
                .build();
        tx().executeWithoutResult(status -> provisioningItemRepository.insert(item));
        return item;
    }

    /**
     * 派生后保存 item 元数据（envelope/KCV/root/derivation/收件指纹；CR-062 §4）。
     * <p>含 CR-063 §10 冻结一致性：envelope view 收件方证书摘要必须等于 ACTIVE 证书 leafCertificateDigest。
     */
    private void updateItemMetadata(BusinessKeyProvisioningItem item, DeviceBusinessKey canonical,
                                    DerivedWrappedKey result, String fingerprintDigest,
                                    ActiveDeviceCertificate activeCert) {
        DerivedWrappedKeyEnvelopeView view = result.envelope();
        if (view == null) {
            throw new KeyprovEnvelopeIntegrityException("派生结果缺少结构化 envelope 视图，禁止落库 envelope v1 元数据");
        }
        byte[] recipientCertDigest = view instanceof EcdhEsP256EnvelopeViewV1 v1
                ? v1.recipientCertificateDigest() : null;
        if (activeCert != null && activeCert.leafCertificateDigest() != null) {
            if (recipientCertDigest == null
                    || !java.util.Arrays.equals(recipientCertDigest, activeCert.leafCertificateDigest())) {
                throw new KeyprovEnvelopeIntegrityException(
                        "envelope view recipientCertificateDigest 与 ACTIVE 证书 leafCertificateDigest 不一致（CR-063 冻结校验失败）");
            }
        }
        item.setKeyId(canonical.getKeyId());
        item.setBusinessKeyVersion(canonical.getBusinessKeyVersion());
        item.setDirectoryKeyId(canonical.getId());
        item.setOperationId(result.operationId() != null ? result.operationId() : item.getOperationId());
        item.setRootKeyVersion(result.rootKeyVersion());
        item.setDerivationVersion(result.derivationVersion());
        item.setRecipientCertFingerprintDigest(fingerprintDigest);
        item.setEnvelopeFormat(view.envelopeFormat());
        item.setWrappingSuite(view.wrappingSuite() == null ? null : view.wrappingSuite().name());
        item.setEnvelopeDigest(view.envelopeDigest());
        item.setKeyCheckValue(view.keyCheckValue());
        item.setRecipientCertDigest(recipientCertDigest);
        if (item.getDeviceResult() == null) {
            item.setDeviceResult(ProvisioningDeviceResult.PENDING);
        }
        item.setModifyTime(LocalDateTime.now());
        tx().executeWithoutResult(status -> provisioningItemRepository.update(item));
    }

    private KeyProvisioningResponse buildResponse(BusinessKeyProvisioning batch, BoundDeviceIdentity identity,
                                                  KeyProvisioningRequest request, ActiveDeviceCertificate activeCert,
                                                  Map<String, DerivedWrappedKey> derived) {
        List<BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningId(batch.getId());
        List<WrappedKeyItem> wrappedItems = new ArrayList<>();
        for (BusinessKeyProvisioningItem item : items) {
            DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
            if (directory == null || directory.getKeyId() == null) {
                continue;
            }
            DerivedWrappedKey wrapped = derived.get(item.getPurpose());
            if (wrapped == null) {
                continue;
            }
            wrappedItems.add(envelopeAdapter.toWrappedKeyItem(item.getPurpose(), directory, wrapped));
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