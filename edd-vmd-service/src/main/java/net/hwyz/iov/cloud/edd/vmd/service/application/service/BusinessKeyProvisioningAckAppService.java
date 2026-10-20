package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.TboxV2cKeyProvisionedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.exception.ImportProofVerificationUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofAlgorithm;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofVerificationRequest;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务密钥设备导入 ACK 应用服务（CR-058 §5 完成事务）。
 * <p>
 * 在同一事务内锁定批次、校验 ACK 摘要/用途全集/keyId/版本、重新校验 active 绑定与证书、
 * 原子切换两目标版本 ACTIVE（REIMPORT 复用 ACTIVE 不重复切换/不重复发目录事件）、
 * 批次置 COMPLETED 并写入专用 Outbox 事件；任一步失败均不产生成功事件。
 * 明确失败 / 未知 / 缺项等需要落库的状态变更在事务内完成后正常提交；仅在完成事务中途
 * 出现需整体回滚的冲突时抛出受控异常。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningAckAppService {

    private static final String STATE_ACTIVE = "ACTIVE";

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final BusinessKeyPolicyRegistry policyRegistry;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final TboxV2cKeyProvisionedPublisher tboxV2cKeyProvisionedPublisher;
    private final BusinessKeyProvisioningDomainService domainService;
    private final ObjectProvider<ImportProofVerifier> importProofVerifierProvider;
    private final PlatformTransactionManager transactionManager;

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
     * 处理 canonical KeyProvisioningAck。
     */
    public KeyProvisioningAckResult confirmImport(KeyProvisioningAck ack, KeyprovAuthenticatedContext ctx) {
        String requestId = ack.getRequestId();
        if (ctx == null || !ctx.hasIdentity()) {
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_PROTOCOL_INVALID, false, "缺少已认证会话身份");
        }
        String ackDigest = domainService.buildAckDigest(ack);
        try {
            return tx().execute(status -> doConfirm(ack, ctx, requestId, ackDigest));
        } catch (BatchSignal signal) {
            log.warn("keyprov ACK 完成事务回滚: requestId={}, code={}", requestId, signal.code);
            return failed(requestId, signal.result, signal.code, signal.retryable, signal.message);
        }
    }

    private KeyProvisioningAckResult doConfirm(KeyProvisioningAck ack, KeyprovAuthenticatedContext ctx,
                                               String requestId, String ackDigest) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestIdForUpdate(requestId);
        if (batch == null) {
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_NOT_FOUND, false, "开通批次不存在");
        }
        String sessionDigest = domainService.digestHsmUid(ctx.hsmUid());
        if (batch.getHsmUidDigest() == null || !batch.getHsmUidDigest().equals(sessionDigest)) {
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "会话身份与开通批次不一致");
        }

        // 幂等：已完成
        if (batch.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
            if (ackDigest.equals(batch.getAckDigest())) {
                return succeeded(requestId);
            }
            log.warn("keyprov ACK 摘要冲突（已完成批次）: requestId={}", requestId);
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT,
                    KeyprovConstants.ERR_ACK_CONFLICT, false, "ACK摘要冲突，已记录安全告警");
        }
        if (!batch.getStatus().canAdvance()) {
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次状态不允许确认");
        }
        // 已记录 ACK 摘要且不一致 → 安全冲突，不改状态
        if (batch.getAckDigest() != null && !batch.getAckDigest().equals(ackDigest)) {
            log.error("keyprov ACK 摘要冲突（安全告警）: requestId={}, deviceSn={}", requestId, batch.getDeviceSn());
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT,
                    KeyprovConstants.ERR_ACK_CONFLICT, false, "ACK摘要冲突，已记录安全告警");
        }

        List<BusinessKeyProvisioningItem> batchItems = provisioningItemRepository.selectByProvisioningId(batch.getId());
        Map<String, BusinessKeyProvisioningItem> batchItemByPurpose = batchItems.stream()
                .collect(Collectors.toMap(BusinessKeyProvisioningItem::getPurpose, i -> i, (a, b) -> a, LinkedHashMap::new));

        Map<String, KeyImportItemResult> ackItemByPurpose = new LinkedHashMap<>();
        for (KeyImportItemResult item : ack.getItemsList()) {
            String code = KeyprovConstants.purposeCode(item.getPurpose());
            if (code == null || ackItemByPurpose.put(code, item) != null) {
                return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK purpose 非法或重复");
            }
        }

        for (String extra : ackItemByPurpose.keySet()) {
            if (!batchItemByPurpose.containsKey(extra)) {
                return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK item 不属于该批次");
            }
        }
        Set<String> missing = batchItemByPurpose.keySet().stream()
                .filter(p -> !ackItemByPurpose.containsKey(p)).collect(Collectors.toSet());
        for (Map.Entry<String, KeyImportItemResult> e : ackItemByPurpose.entrySet()) {
            BusinessKeyProvisioningItem batchItem = batchItemByPurpose.get(e.getKey());
            KeyImportItemResult ackItem = e.getValue();
            String mismatch = identityMismatch(batchItem, ackItem);
            if (mismatch != null) {
                return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK item 与批次不匹配: " + mismatch);
            }
            // CR-062 §4.3：仅成功 item 必须与原 Response 全字段绑定并携带合法 Import Attestation
            if (ackItem.getResult() == KeyImportResult.KEY_IMPORT_RESULT_IMPORTED) {
                String envelopeMismatch = envelopeMismatch(batchItem, ackItem);
                if (envelopeMismatch != null) {
                    return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                            KeyprovConstants.ERR_ITEM_MISMATCH, false,
                            "ACK item 与原Response不匹配: " + envelopeMismatch);
                }
                String proofIssue = importProofIssue(ackItem);
                if (proofIssue != null) {
                    return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                            KeyprovConstants.ERR_IMPORT_PROOF_INVALID, false, "ACK 导入证明字段非法: " + proofIssue);
                }
            }
        }

        // 明确失败 → FAILED（新建 PENDING 目标版本 FAILED，复用 ACTIVE 不变，不发事件）
        boolean anyFailed = ackItemByPurpose.values().stream()
                .anyMatch(i -> i.getResult() == KeyImportResult.KEY_IMPORT_RESULT_FAILED);
        if (anyFailed) {
            LocalDateTime now = LocalDateTime.now();
            for (BusinessKeyProvisioningItem batchItem : batchItems) {
                DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(batchItem.getDirectoryKeyId());
                if (directory != null && directory.getKeyState() == BusinessKeyState.PENDING) {
                    directory.markFailed("设备导入失败");
                    directory.setModifyTime(now);
                    deviceBusinessKeyRepository.update(directory);
                }
                batchItem.markFailed(null);
                batchItem.setModifyTime(now);
                provisioningItemRepository.update(batchItem);
            }
            batch.markFailed("设备导入明确失败");
            batch.setModifyTime(now);
            provisioningRepository.update(batch);
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_IMPORT_FAILED, false, "设备导入失败");
        }

        // 未知或缺项 → 待对账（新目标版本保持 PENDING，不发事件）
        boolean anyUnknown = ackItemByPurpose.values().stream()
                .anyMatch(i -> i.getResult() == KeyImportResult.KEY_IMPORT_RESULT_UNKNOWN
                        || i.getResult() == KeyImportResult.KEY_IMPORT_RESULT_UNSPECIFIED);
        if (anyUnknown || !missing.isEmpty()) {
            batch.markReconcileRequired("ACK存在未知或缺失用途");
            batch.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(batch);
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                    KeyprovConstants.ERR_RECONCILE_REQUIRED, true, "ACK未知或用途不完整，待以原requestId对账");
        }

        return complete(batch, batchItems, ackItemByPurpose, ctx, ackDigest);
    }

    private KeyProvisioningAckResult complete(BusinessKeyProvisioning batch,
                                              List<BusinessKeyProvisioningItem> batchItems,
                                              Map<String, KeyImportItemResult> ackItemByPurpose,
                                              KeyprovAuthenticatedContext ctx, String ackDigest) {
        // 1) 重新校验当前 active 绑定与证书（fail-closed）
        BoundDeviceIdentity identity;
        String certSn;
        try {
            identity = boundDeviceIdentityResolver.resolveByHsmUid(ctx.hsmUid(), null);
            if (!identity.deviceSn().equals(batch.getDeviceSn())
                    || !Objects.equals(identity.bindingId(), batch.getBindingId())
                    || !Objects.equals(identity.partId(), batch.getPartId())) {
                throw new IllegalStateException("active绑定已变化");
            }
            certSn = activeDeviceCertificateResolver.resolveActiveCertSnForProvisioning(identity, batch.getCertificateSnDigest());
        } catch (RuntimeException e) {
            log.warn("keyprov 完成前绑定/证书复核失败: requestId={}, deviceSn={}", batch.getRequestId(), batch.getDeviceSn(), e);
            batch.markFailed("完成前设备绑定或证书失效");
            batch.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(batch);
            return failed(batch.getRequestId(), KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "设备绑定或证书失效");
        }
        String certDigest = domainService.sha256Hex(domainService.utf8(certSn));
        if (batch.getCertificateSnDigest() != null && !batch.getCertificateSnDigest().equals(certDigest)) {
            log.warn("keyprov 完成前设备证书已变化: requestId={}, deviceSn={}", batch.getRequestId(), batch.getDeviceSn());
            batch.markFailed("完成前设备证书失效或轮换");
            batch.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(batch);
            return failed(batch.getRequestId(), KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "设备证书失效或轮换");
        }

        // CR-062 §4.3/RD-062-8：两用途均经 FW-SEC 真实 ECDSA 验签后，才允许进入激活事务
        verifyImportProofs(batch, batchItems, ackItemByPurpose, certSn);

        // 2) 原子切换目标版本（PENDING→ACTIVE）+ 逐项确认 + 完成批次 + 专用事件
        List<BusinessKeyProvisioningItem> succeeded = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (BusinessKeyProvisioningItem batchItem : batchItems) {
            DeviceBusinessKey directory = deviceBusinessKeyRepository.selectByIdForUpdate(batchItem.getDirectoryKeyId());
            if (directory == null) {
                throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                        KeyprovConstants.ERR_STATE_NOT_ALLOWED, true, "目标目录版本不存在");
            }
            if (directory.getKeyState() == BusinessKeyState.PENDING) {
                deprecateOldActive(batch.getDeviceSn(), batch.getBusinessDomain(), batchItem.getPurpose(), directory.getId(), now);
                directory.activate();
                directory.setModifyTime(now);
                if (deviceBusinessKeyRepository.update(directory) == 0) {
                    throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                            KeyprovConstants.ERR_STATE_NOT_ALLOWED, true, "目标版本激活并发冲突");
                }
                businessKeyChangedPublisher.publish(directory, STATE_ACTIVE);
            } else if (directory.getKeyState() != BusinessKeyState.ACTIVE) {
                throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "目标版本状态不允许激活");
            }
            // REIMPORT 复用 ACTIVE：不重复发布目录变更事件

            KeyImportItemResult ackItem = ackItemByPurpose.get(batchItem.getPurpose());
            batchItem.markSucceeded(domainService.digestImportProof(ackItem.getImportProof().toByteArray()), now);
            batchItem.setModifyTime(now);
            provisioningItemRepository.update(batchItem);
            succeeded.add(batchItem);
        }

        batch.setAckDigest(ackDigest);
        batch.markCompleted();
        batch.setModifyTime(now);
        provisioningRepository.update(batch);

        tboxV2cKeyProvisionedPublisher.publish(batch.getRequestId(), batch.getDeviceSn(),
                batch.getBusinessDomain(), succeeded);

        return succeeded(batch.getRequestId());
    }

    /**
     * ACK item 与原批次的身份绑定（purpose 已校验，此处比对 keyId/版本）。
     *
     * @return 不匹配字段名；全部匹配返回 {@code null}
     */
    private String identityMismatch(BusinessKeyProvisioningItem batchItem, KeyImportItemResult ackItem) {
        if (!Objects.equals(batchItem.getKeyId(), ackItem.getKeyId())) {
            return "keyId";
        }
        if (batchItem.getBusinessKeyVersion() == null
                || batchItem.getBusinessKeyVersion() != ackItem.getBusinessKeyVersion()) {
            return "businessKeyVersion";
        }
        return null;
    }

    /**
     * 成功 item 与原 Response/批次持久化信封元数据逐项绑定（CR-062 §4.3）。
     *
     * @return 不匹配字段名；全部匹配返回 {@code null}
     */
    private String envelopeMismatch(BusinessKeyProvisioningItem batchItem, KeyImportItemResult ackItem) {
        if (!EcdhEsP256EnvelopeViewV1.ENVELOPE_FORMAT.equals(batchItem.getEnvelopeFormat())
                || ackItem.getEnvelopeFormat() != KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1) {
            return "envelopeFormat";
        }
        if (!Arrays.equals(batchItem.getEnvelopeDigest(), ackItem.getEnvelopeDigest().toByteArray())) {
            return "envelopeDigest";
        }
        if (!Arrays.equals(batchItem.getKeyCheckValue(), ackItem.getKeyCheckValue().toByteArray())) {
            return "keyCheckValue";
        }
        if (!Arrays.equals(batchItem.getRecipientCertDigest(),
                ackItem.getRecipientCertificateDigest().toByteArray())) {
            return "recipientCertificateDigest";
        }
        return null;
    }

    /**
     * CR-008 §3.1 成功 item 导入证明字段门禁。
     *
     * @return 非法原因；合法返回 {@code null}
     */
    private String importProofIssue(KeyImportItemResult ackItem) {
        if (!ackItem.getProofAlgorithm().isBlank()) {
            return "遗留 proof_algorithm 必须为空（不得形成第二事实源）";
        }
        if (ackItem.getImportProofAlgorithm()
                != net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm.IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363) {
            return "import_proof_algorithm 非 ECDSA_P256_SHA256_P1363";
        }
        if (ackItem.getImportProof().size() != ImportProofAlgorithm.SIGNATURE_LENGTH) {
            return "import_proof 长度非 " + ImportProofAlgorithm.SIGNATURE_LENGTH + " 字节";
        }
        if (ackItem.getRecipientCertificateDigest().size() != ImportProofAlgorithm.DIGEST_LENGTH) {
            return "recipient_certificate_digest 长度非 " + ImportProofAlgorithm.DIGEST_LENGTH + " 字节";
        }
        return null;
    }

    /**
     * 逐用途调用 FW-SEC CR-016 ImportProofVerifier 执行真实 ECDSA P-256/SHA-256/P1363 验签。
     * <p>
     * 不在此处捕获异常：确定性拒绝（{@link net.hwyz.iov.cloud.framework.security.crypto.exception.ImportProofRejectedException}、
     * {@code CertificateEvidenceRejectedException}）与依赖/材料不可用由应用门面统一映射，
     * 任一用途失败均不进入激活事务（CR-062 §4.3/§8）。
     */
    private void verifyImportProofs(BusinessKeyProvisioning batch, List<BusinessKeyProvisioningItem> batchItems,
                                    Map<String, KeyImportItemResult> ackItemByPurpose, String certSn) {
        ImportProofVerifier verifier = importProofVerifierProvider.getIfAvailable();
        if (verifier == null) {
            throw new ImportProofVerificationUnavailableException(
                    "ImportProofVerifier 未装配（crypto.import-proof.enabled=false），禁止激活");
        }
        DeviceCertificateRef certificateRef = DeviceCertificateRef.ofSerial(certSn);
        for (BusinessKeyProvisioningItem item : batchItems) {
            KeyImportItemResult ackItem = ackItemByPurpose.get(item.getPurpose());
            ImportProofVerificationRequest request = new ImportProofVerificationRequest(
                    ImportProofAlgorithm.ECDSA_P256_SHA256_P1363,
                    ackItem.getImportProof().toByteArray(),
                    certificateRef,
                    ackItem.getRecipientCertificateDigest().toByteArray(),
                    batch.getRequestId(),
                    KeyprovConstants.BUSINESS_DOMAIN_V2C,
                    KeyPurpose.valueOf(item.getPurpose()),
                    item.getKeyId(),
                    item.getBusinessKeyVersion() == null ? 0L : item.getBusinessKeyVersion(),
                    ackItem.getEnvelopeDigest().toByteArray(),
                    ackItem.getKeyCheckValue().toByteArray());
            verifier.verify(request);
        }
    }

    private void deprecateOldActive(String deviceSn, String domain, String purpose, Long targetId, LocalDateTime now) {
        DeviceBusinessKey oldActive = deviceBusinessKeyRepository.selectActiveByContextForUpdate(deviceSn, domain, purpose);
        if (oldActive == null || oldActive.getId().equals(targetId)) {
            return;
        }
        Duration window = policyRegistry.resolve(domain, purpose)
                .map(BusinessKeyPolicyEntry::decryptWindow).orElse(null);
        LocalDateTime decryptUntil = window != null ? now.plus(window) : now;
        oldActive.deprecate(decryptUntil);
        oldActive.setModifyTime(now);
        if (deviceBusinessKeyRepository.update(oldActive) == 0) {
            throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                    KeyprovConstants.ERR_STATE_NOT_ALLOWED, true, "旧版本降级并发冲突");
        }
    }

    private KeyProvisioningAckResult succeeded(String requestId) {
        return KeyProvisioningAckResult.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                .setOccurredAtMs(System.currentTimeMillis())
                .build();
    }

    private KeyProvisioningAckResult failed(String requestId, KeyProvisioningResult result,
                                            String code, boolean retryable, String message) {
        return KeyProvisioningAckResult.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setResult(result)
                .setOccurredAtMs(System.currentTimeMillis())
                .setError(ProtocolError.newBuilder()
                        .setCode(code == null ? "" : code)
                        .setRetryable(retryable)
                        .setAction(retryable ? "RETRY_SAME_REQUEST_ID" : "DO_NOT_RETRY")
                        .setMessage(message == null ? "" : message)
                        .build())
                .build();
    }

    /**
     * 事务内需整体回滚的受控信号
     */
    private static final class BatchSignal extends RuntimeException {
        private final KeyProvisioningResult result;
        private final String code;
        private final boolean retryable;
        private final String message;

        private BatchSignal(KeyProvisioningResult result, String code, boolean retryable, String message) {
            super(code, null, false, false);
            this.result = result;
            this.code = code;
            this.retryable = retryable;
            this.message = message;
        }
    }
}
