package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
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
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyCreateRequest;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyMaterial;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.KeyMaterialPolicy;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RecipientRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.WrapContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.WrapMode;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.WrappedBusinessKey;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.protobuf.ByteString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务密钥多用途 V2C 开通应用服务（CR-058 F20 Request 段）。
 * <p>
 * 以已认证 hsmUid 定位 active 绑定，按 canonical Request 完整用途集创建或解析目标目录版本，
 * 完成设备证书公钥 Wrap 后将批次推进 WAIT_DEVICE_ACK；设备导入成功前不发布目录 ACTIVE 事件，
 * 也不发布设备注册激活完成事件。目标版本模式由 VMD 依据 current_versions + ACTIVE + 策略推导。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningBatchAppService {

    private static final String WRAP_MODE = "DEVICE_CERT_PUBLIC_KEY";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final BusinessKeyPolicyRegistry policyRegistry;
    private final BusinessKeyBizTypeMapper bizTypeMapper;
    private final ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final BusinessKeyProvisioningDomainService domainService;
    private final PlatformTransactionManager transactionManager;

    @Value("${vmd.business-key.provisioning.bundle-ttl-seconds:3600}")
    private long bundleTtlSeconds = 3600;

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

    private BusinessKeyMaterialTemplate materialTemplate() {
        BusinessKeyMaterialTemplate template = businessKeyMaterialTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new CryptoDependencyUnavailableException("framework业务密钥模板未装配（crypto.business-key.enabled=false）");
        }
        return template;
    }

    /**
     * 处理 canonical KeyProvisioningRequest。
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

        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            try {
                authorizationPolicy.authorize(BusinessKeyAction.PROVISION, identity.deviceCategory(), domain, purpose);
            } catch (RuntimeException e) {
                log.warn("keyprov 授权拒绝: requestId={}, domain={}, purpose={}", requestId, domain, purpose);
                return rejected(requestId, KeyprovConstants.ERR_PURPOSE_INCOMPLETE, false, "业务域或用途未授权");
            }
        }

        String certSn;
        try {
            certSn = activeDeviceCertificateResolver.resolveActiveCertSn(identity);
        } catch (RuntimeException e) {
            log.warn("keyprov 无有效设备证书: requestId={}, deviceSn={}", requestId, identity.deviceSn());
            return rejected(requestId, KeyprovConstants.ERR_CERTIFICATE_INVALID, false, "有效设备证书不存在");
        }

        String suite = domainService.negotiateWrappingSuite(request, KeyprovConstants.DEFAULT_SUPPORTED_SUITES);
        if (suite == null) {
            return rejected(requestId, KeyprovConstants.ERR_PROTOCOL_INVALID, false, "无受支持的封装套件");
        }

        String hsmDigest = domainService.digestHsmUid(identity.hsmUid());
        String requestDigest = domainService.buildRequestDigest(request, hsmDigest);

        // 幂等：同 requestId 复用原批次与版本
        BusinessKeyProvisioning existing = provisioningRepository.selectByRequestId(requestId);
        if (existing != null) {
            if (existing.getRequestDigest() != null && !existing.getRequestDigest().equals(requestDigest)) {
                log.warn("keyprov requestId 摘要冲突: requestId={}", requestId);
                return conflict(requestId, KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT, "同requestId不同请求摘要");
            }
            if (existing.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
                return buildResponse(existing, identity, request, certSn, suite, true);
            }
            if (!existing.getStatus().canAdvance()) {
                return rejected(requestId, KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次状态不允许继续申请");
            }
            return buildResponse(existing, identity, request, certSn, suite, false);
        }

        // 新建批次并逐用途创建/复用目标版本
        BusinessKeyProvisioning batch = createOrLoadBatch(requestId, identity, domain, hsmDigest, certSn, requestDigest);
        if (batch == null) {
            return rejected(requestId, KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "并发开通冲突，请以原requestId重试");
        }
        try {
            prepareTargets(batch, request, identity, domain, certSn, suite, requestDigest);
        } catch (CryptoOperationOutcomeUnknownException e) {
            markBatchReconcile(batch, "framework创建/Wrap结果未知");
            return retryable(requestId, KeyprovConstants.ERR_UNAVAILABLE, "framework结果未知，需以原requestId对账");
        } catch (CryptoException e) {
            markBatchFailed(batch, truncate(e.getMessage()));
            return rejected(requestId, KeyprovConstants.ERR_UNAVAILABLE, false, "业务密钥材料创建或封装失败");
        }
        return buildResponse(provisioningRepository.selectByRequestId(requestId), identity, request, certSn, suite, false);
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

    private void prepareTargets(BusinessKeyProvisioning batch, KeyProvisioningRequest request,
                                BoundDeviceIdentity identity, String domain, String certSn,
                                String suite, String requestDigest) {
        Map<String, Long> currentVersions = domainService.extractCurrentVersions(request);
        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            BusinessKeyProvisioningItem existingItem = provisioningItemRepository
                    .selectByProvisioningIdAndPurpose(batch.getId(), purpose);
            if (existingItem != null) {
                continue;
            }
            DeviceBusinessKey active = deviceBusinessKeyRepository.selectActiveByContext(identity.deviceSn(), domain, purpose);
            BusinessKeyProvisioningIntent intent = domainService.deriveIntent(active,
                    currentVersions.getOrDefault(purpose, 0L));

            DeviceBusinessKey target;
            if (intent == BusinessKeyProvisioningIntent.REIMPORT) {
                // 复用既有 ACTIVE，不新建版本、不重复发布目录 ACTIVE 事件
                target = active;
            } else {
                target = createPendingVersion(identity, domain, purpose, request.getRequestId());
                BusinessKeyPolicyEntry policy = authorizationPolicy.authorize(
                        BusinessKeyAction.PROVISION, identity.deviceCategory(), domain, purpose);
                BusinessKeyMaterial material = materialTemplate().create(buildCreateRequest(
                        request, identity, domain, purpose, target.getBusinessKeyVersion(), policy));
                target = persistMaterialKeepingPending(target, material, request.getRequestId());
            }
            insertItem(batch.getId(), target, purpose);
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
        log.info("keyprov 批次进入WAIT_DEVICE_ACK: requestId={}, deviceSn={}, domain={}",
                batch.getRequestId(), identity.deviceSn(), domain);
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
                    .businessKeyVersion(nextVersion)
                    .keyState(BusinessKeyState.PENDING)
                    .wrapMode(WRAP_MODE)
                    .requestId(requestId)
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

    private DeviceBusinessKey persistMaterialKeepingPending(DeviceBusinessKey row, BusinessKeyMaterial material,
                                                            String requestId) {
        return tx().execute(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(row.getId());
            if (current == null) {
                throw new IllegalStateException("keyprov目标版本行不存在: id=" + row.getId());
            }
            current.setKeyId(material.keyId());
            current.setKmsKeyRef(material.kmsKeyRef());
            current.setKmsKeyVersion(material.kmsKeyVersion());
            current.setKmsProvider(material.provider());
            current.setAlgorithm(material.algorithm());
            current.setKeySpec(material.keySpec());
            current.setValidFrom(toLocalDateTime(material.validFrom()));
            current.setValidTo(toLocalDateTime(material.validTo()));
            current.setRequestId(requestId);
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
            return current;
        });
    }

    private void insertItem(Long provisioningId, DeviceBusinessKey target, String purpose) {
        tx().executeWithoutResult(status -> provisioningItemRepository.insert(BusinessKeyProvisioningItem.builder()
                .provisioningId(provisioningId)
                .purpose(purpose)
                .keyId(target.getKeyId())
                .businessKeyVersion(target.getBusinessKeyVersion())
                .directoryKeyId(target.getId())
                .deviceResult(ProvisioningDeviceResult.PENDING)
                .createTime(LocalDateTime.now())
                .build()));
    }

    private BusinessKeyCreateRequest buildCreateRequest(KeyProvisioningRequest request, BoundDeviceIdentity identity,
                                                        String domain, String purpose, Long version,
                                                        BusinessKeyPolicyEntry policy) {
        BizType bizType = bizTypeMapper.resolveBizType(domain);
        KeyMaterialPolicy materialPolicy = KeyMaterialPolicy.of(policy.algorithm(), policy.keySpec());
        Map<String, String> auditContext = new HashMap<>();
        auditContext.put("deviceSn", identity.deviceSn());
        auditContext.put("businessDomain", domain);
        auditContext.put("purpose", purpose);
        auditContext.put("businessKeyVersion", String.valueOf(version));
        return new BusinessKeyCreateRequest(bizType, materialPolicy,
                request.getRequestId() + ":" + purpose, auditContext);
    }

    private KeyProvisioningResponse buildResponse(BusinessKeyProvisioning batch, BoundDeviceIdentity identity,
                                                  KeyProvisioningRequest request, String certSn,
                                                  String suite, boolean completed) {
        List<BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningId(batch.getId());
        List<WrappedKeyItem> wrappedItems = new ArrayList<>();
        for (BusinessKeyProvisioningItem item : items) {
            DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
            if (directory == null || directory.getKeyId() == null) {
                continue;
            }
            WrappedBusinessKey wrapped = wrap(directory, certSn, request.getRequestId(), item.getPurpose());
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

    private WrappedKeyItem toWrappedItem(String purpose, DeviceBusinessKey directory, WrappedBusinessKey wrapped,
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
        if (wrapped.expiry() != null) {
            builder.setValidToMs(wrapped.expiry().toEpochMilli());
        }
        if (directory.getValidFrom() != null) {
            builder.setValidFromMs(toEpochMs(directory.getValidFrom()));
        }
        if (directory.getValidTo() != null) {
            builder.setValidToMs(toEpochMs(directory.getValidTo()));
        }
        if (wrapped.parameters() != null && !wrapped.parameters().isEmpty()) {
            builder.setParameters(ByteString.copyFromUtf8(JSONUtil.toJsonStr(wrapped.parameters())));
        }
        return builder.build();
    }

    private WrappedBusinessKey wrap(DeviceBusinessKey directory, String certSn, String requestId, String purpose) {
        Map<String, String> audit = new HashMap<>();
        audit.put("purpose", purpose);
        audit.put("operation", "KEYPROV");
        return materialTemplate().wrap(
                new BusinessKeyRef(directory.getKeyId(), directory.getKmsKeyRef()),
                new RecipientRef.DeviceCertificate(certSn),
                new WrapContext(WrapMode.DEVICE_CERT_PUBLIC_KEY, requestId + ":" + purpose + ":wrap", audit));
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
            if (current != null && current.getStatus().isOpen()) {
                // 本批次新建 PENDING 目标版本置 FAILED；复用 ACTIVE 不受影响
                for (BusinessKeyProvisioningItem item : provisioningItemRepository.selectByProvisioningId(current.getId())) {
                    DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
                    if (directory != null && directory.getKeyState() == BusinessKeyState.PENDING) {
                        directory.markFailed(reason);
                        directory.setModifyTime(LocalDateTime.now());
                        deviceBusinessKeyRepository.update(directory);
                    }
                    if (item.getDeviceResult() == ProvisioningDeviceResult.PENDING) {
                        item.markFailed(null);
                        item.setModifyTime(LocalDateTime.now());
                        provisioningItemRepository.update(item);
                    }
                }
                current.markFailed(reason);
                current.setModifyTime(LocalDateTime.now());
                provisioningRepository.update(current);
            }
        });
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    private long toEpochMs(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
