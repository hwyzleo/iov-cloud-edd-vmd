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

import java.security.MessageDigest;
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
    private final Kl1BatchTerminator kl1BatchTerminator;
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
     * 处理 canonical KeyProvisioningAck（VMD-DSN-CR-069 §4 三阶段线性化）。
     * <p>
     * 阶段1（锁内）：身份/状态/B 到期/ACK 绑定校验，明确失败与未知缺项落库；COMPLETED 精确重复
     * proof 幂等；KL1 批次 now≥B 未激活 → FAILED(ACK_TIMEOUT)（释放 claims）。
     * 阶段2（锁外）：两用途 Import Attestation 真实 ECDSA 验签（耗时计算不持锁）。
     * 阶段3（锁内）：重验状态/版本/now&lt;B/两用途齐全 → 批次 ACTIVE + 两用途目录 ACTIVE + 唯一 outbox 同事务。
     */
    public KeyProvisioningAckResult confirmImport(KeyProvisioningAck ack, KeyprovAuthenticatedContext ctx) {
        String requestId = ack.getRequestId();
        if (ctx == null || !ctx.hasIdentity()) {
            return failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_PROTOCOL_INVALID, false, "缺少已认证会话身份");
        }
        String ackDigest = domainService.buildAckDigest(ack);
        ActivateContext act;
        try {
            act = tx().execute(status -> doPrecheckAndValidate(ack, ctx, requestId, ackDigest));
        } catch (BatchSignal signal) {
            log.warn("keyprov ACK 完成事务回滚: requestId={}, code={}", requestId, signal.code);
            return failed(requestId, signal.result, signal.code, signal.retryable, signal.message);
        }
        if (act == null || act.terminalResult != null) {
            return act != null ? act.terminalResult
                    : failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                            KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次状态不允许确认");
        }
        // 阶段2：锁外真实验签（任一失败由门面映射为 806 码；不在此处捕获为成功）
        try {
            verifyImportProofs(act.batch, act.batchItems, act.ackItemByPurpose, act.certSn);
        } catch (RuntimeException e) {
            log.warn("keyprov ACK 导入证明验签失败（锁外）: requestId={}, type={}",
                    requestId, e.getClass().getSimpleName(), e);
            throw e;
        }
        // 阶段3：锁内重验并原子激活
        try {
            return tx().execute(status -> doActivate(act.batch.getRequestId(), act.ackItemByPurpose, ctx, ackDigest));
        } catch (BatchSignal signal) {
            log.warn("keyprov ACK 激活事务回滚: requestId={}, code={}", requestId, signal.code);
            return failed(requestId, signal.result, signal.code, signal.retryable, signal.message);
        }
    }

    /**
     * 阶段1：锁内预检与校验；返回激活上下文或终态结果。
     */
    private ActivateContext doPrecheckAndValidate(KeyProvisioningAck ack, KeyprovAuthenticatedContext ctx,
                                                  String requestId, String ackDigest) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestIdForUpdate(requestId);
        if (batch == null) {
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_NOT_FOUND, false, "开通批次不存在"));
        }
        String sessionDigest = domainService.digestHsmUid(ctx.hsmUid());
        if (batch.getHsmUidDigest() == null || !batch.getHsmUidDigest().equals(sessionDigest)) {
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "会话身份与开通批次不一致"));
        }
        // 锁内先判已有 ACTIVE（精确重复 proof 幂等；错 proof 拒绝），再判 B 到期；FAILED 不可复活（LC04/LC05/LC19）
        if (batch.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
            if (ackDigest.equals(batch.getAckDigest())) {
                if (batch.isKl1() && !kl1AckProofsMatch(batch, ack)) {
                    log.error("keyprov ACTIVE 重复 ACK proof 不匹配（安全告警）: requestId={}", requestId);
                    return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT,
                            KeyprovConstants.ERR_ACK_CONFLICT, false, "ACTIVE重复ACK proof不匹配，拒绝"));
                }
                return terminal(succeeded(requestId));
            }
            log.warn("keyprov ACK 摘要冲突（已完成批次）: requestId={}", requestId);
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT,
                    KeyprovConstants.ERR_ACK_CONFLICT, false, "ACK摘要冲突，已记录安全告警"));
        }
        if (batch.isKl1()) {
            // KL1：B 到期未激活 → FAILED(ACK_TIMEOUT)（锁内 CAS + 释放 claims）；迟到 ACK 绝不复活
            kl1BatchTerminator.terminateIfDeadlinePassed(batch, LocalDateTime.now());
            if (batch.getStatus() == BusinessKeyProvisioningState.FAILED
                    || batch.getStatus() == BusinessKeyProvisioningState.EXPIRED) {
                return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次已终结，迟到ACK不可复活"));
            }
        } else if (!batch.getStatus().canAdvance()) {
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "批次状态不允许确认"));
        }
        // 已记录 ACK 摘要且不一致 → 安全冲突，不改状态
        if (batch.getAckDigest() != null && !batch.getAckDigest().equals(ackDigest)) {
            log.error("keyprov ACK 摘要冲突（安全告警）: requestId={}, deviceSn={}", requestId, batch.getDeviceSn());
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_CONFLICT,
                    KeyprovConstants.ERR_ACK_CONFLICT, false, "ACK摘要冲突，已记录安全告警"));
        }

        List<BusinessKeyProvisioningItem> batchItems = provisioningItemRepository.selectByProvisioningId(batch.getId());
        Map<String, BusinessKeyProvisioningItem> batchItemByPurpose = batchItems.stream()
                .collect(Collectors.toMap(BusinessKeyProvisioningItem::getPurpose, i -> i, (a, b) -> a, LinkedHashMap::new));

        Map<String, KeyImportItemResult> ackItemByPurpose = new LinkedHashMap<>();
        for (KeyImportItemResult item : ack.getItemsList()) {
            String code = KeyprovConstants.purposeCode(item.getPurpose());
            if (code == null || ackItemByPurpose.put(code, item) != null) {
                return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK purpose 非法或重复"));
            }
        }

        for (String extra : ackItemByPurpose.keySet()) {
            if (!batchItemByPurpose.containsKey(extra)) {
                return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK item 不属于该批次"));
            }
        }
        Set<String> missing = batchItemByPurpose.keySet().stream()
                .filter(p -> !ackItemByPurpose.containsKey(p)).collect(Collectors.toSet());
        for (Map.Entry<String, KeyImportItemResult> e : ackItemByPurpose.entrySet()) {
            BusinessKeyProvisioningItem batchItem = batchItemByPurpose.get(e.getKey());
            KeyImportItemResult ackItem = e.getValue();
            String mismatch = identityMismatch(batchItem, ackItem);
            if (mismatch != null) {
                return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_ITEM_MISMATCH, false, "ACK item 与批次不匹配: " + mismatch));
            }
            // CR-062 §4.3：仅成功 item 必须与原 Response 全字段绑定并携带合法 Import Attestation
            if (ackItem.getResult() == KeyImportResult.KEY_IMPORT_RESULT_IMPORTED) {
                String envelopeMismatch = envelopeMismatch(batchItem, ackItem);
                if (envelopeMismatch != null) {
                    return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                            KeyprovConstants.ERR_ITEM_MISMATCH, false,
                            "ACK item 与原Response不匹配: " + envelopeMismatch));
                }
                String proofIssue = importProofIssue(ackItem);
                if (proofIssue != null) {
                    return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                            KeyprovConstants.ERR_IMPORT_PROOF_INVALID, false, "ACK 导入证明字段非法: " + proofIssue));
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
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_IMPORT_FAILED, false, "设备导入失败"));
        }

        // 未知或缺项 → 待对账（新目标版本保持 PENDING，不发事件）
        boolean anyUnknown = ackItemByPurpose.values().stream()
                .anyMatch(i -> i.getResult() == KeyImportResult.KEY_IMPORT_RESULT_UNKNOWN
                        || i.getResult() == KeyImportResult.KEY_IMPORT_RESULT_UNSPECIFIED);
        if (anyUnknown || !missing.isEmpty()) {
            batch.markReconcileRequired("ACK存在未知或缺失用途");
            batch.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(batch);
            return terminal(failed(requestId, KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                    KeyprovConstants.ERR_RECONCILE_REQUIRED, true, "ACK未知或用途不完整，待以原requestId对账"));
        }

        // 激活前置：重新校验当前 active 绑定与证书（fail-closed，锁内）
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
            return terminal(failed(batch.getRequestId(), KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "设备绑定或证书失效"));
        }
        String certDigest = domainService.sha256Hex(domainService.utf8(certSn));
        if (batch.getCertificateSnDigest() != null && !batch.getCertificateSnDigest().equals(certDigest)) {
            log.warn("keyprov 完成前设备证书已变化: requestId={}, deviceSn={}", batch.getRequestId(), batch.getDeviceSn());
            batch.markFailed("完成前设备证书失效或轮换");
            batch.setModifyTime(LocalDateTime.now());
            provisioningRepository.update(batch);
            return terminal(failed(batch.getRequestId(), KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, false, "设备证书失效或轮换"));
        }
        return new ActivateContext(batch, batchItems, ackItemByPurpose, certSn, null);
    }

    /**
     * KL1：ACTIVE 批次重复 ACK 快速路径——精确比较持久化两用途 proof 摘要（LC05）。
     *
     * @return true 表示全部成功 item 的 import_proof_digest 与持久化证据一致
     */
    private boolean kl1AckProofsMatch(BusinessKeyProvisioning batch, KeyProvisioningAck ack) {
        Map<String, BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningId(batch.getId())
                .stream().collect(Collectors.toMap(BusinessKeyProvisioningItem::getPurpose, i -> i, (a, b) -> a));
        for (KeyImportItemResult ackItem : ack.getItemsList()) {
            String code = KeyprovConstants.purposeCode(ackItem.getPurpose());
            if (code == null) {
                return false;
            }
            BusinessKeyProvisioningItem stored = items.get(code);
            if (stored == null || stored.getImportProofDigest() == null) {
                return false;
            }
            String proofDigest = domainService.digestImportProof(ackItem.getImportProof().toByteArray());
            if (!stored.getImportProofDigest().equals(proofDigest)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 阶段3：锁内重验（状态/版本/now&lt;B/两用途齐全）后原子激活。
     */
    private KeyProvisioningAckResult doActivate(String requestId, Map<String, KeyImportItemResult> ackItemByPurpose,
                                                KeyprovAuthenticatedContext ctx, String ackDigest) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestIdForUpdate(requestId);
        if (batch == null) {
            throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_NOT_FOUND, false, "批次不存在");
        }
        // 锁内重验：状态未变 + KL1 now<B + 两用途齐全（VMD-DSN-CR-069 §4：ACL 与 sweeper 并发只可能 ACTIVE 或 FAILED）
        if (batch.getStatus() != BusinessKeyProvisioningState.WAIT_DEVICE_ACK) {
            throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "激活前批次状态已变");
        }
        if (batch.isKl1()) {
            LocalDateTime now = LocalDateTime.now();
            if (batch.getAckDeadlineAt() != null && !now.isBefore(batch.getAckDeadlineAt())) {
                kl1BatchTerminator.terminateIfDeadlinePassed(batch, now);
                throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        KeyprovConstants.ERR_STATE_NOT_ALLOWED, false, "激活前已跨B，ACK_TIMEOUT");
            }
        }
        if (ackItemByPurpose.size() < 2
                || !KeyprovConstants.isCompleteV2cPurposeSet(ackItemByPurpose.keySet())) {
            throw new BatchSignal(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                    KeyprovConstants.ERR_ITEM_MISMATCH, false, "激活前两用途不齐全");
        }
        List<BusinessKeyProvisioningItem> batchItems = provisioningItemRepository.selectByProvisioningId(batch.getId());

        // 原子切换目标版本（PENDING→ACTIVE）+ 逐项确认 + 完成批次 + 唯一 outbox 事件
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
     * 成功 item 与原 Response/批次持久化信封元数据逐项绑定（CR-062 §4.3 / CR-068 §5）。
     * <p>CR-068：envelope_digest 的 wire 语义固定为已保存的 {@code wireEnvelopeDigest}，
     * 常量时间比较；core digest 不得进入 ACK DTO 或 proof verifier 请求。
     *
     * @return 不匹配字段名；全部匹配返回 {@code null}
     */
    private String envelopeMismatch(BusinessKeyProvisioningItem batchItem, KeyImportItemResult ackItem) {
        if (!EcdhEsP256EnvelopeViewV1.ENVELOPE_FORMAT.equals(batchItem.getEnvelopeFormat())
                || ackItem.getEnvelopeFormat() != KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1) {
            return "envelopeFormat";
        }
        // wire 权威：ack.envelopeDigest == stored wireEnvelopeDigest（常量时间比较）
        byte[] storedWire = batchItem.getWireEnvelopeDigest();
        byte[] ackDigest = ackItem.getEnvelopeDigest().toByteArray();
        if (storedWire == null || !MessageDigest.isEqual(storedWire, ackDigest)) {
            return "envelopeDigest(wire)";
        }
        if (!MessageDigest.isEqual(batchItem.getKeyCheckValue(), ackItem.getKeyCheckValue().toByteArray())) {
            return "keyCheckValue";
        }
        if (!MessageDigest.isEqual(batchItem.getRecipientCertDigest(),
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
                    // CR-068：proofInputV1 segment[7] 绑定 stored wireEnvelopeDigest（已由 envelopeMismatch 与 ack digest 常量时间比对一致）
                    item.getWireEnvelopeDigest(),
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
     * 阶段1→阶段3 传递的激活上下文。
     */
    private static final class ActivateContext {
        private final BusinessKeyProvisioning batch;
        private final List<BusinessKeyProvisioningItem> batchItems;
        private final Map<String, KeyImportItemResult> ackItemByPurpose;
        private final String certSn;
        private final KeyProvisioningAckResult terminalResult;

        private ActivateContext(BusinessKeyProvisioning batch,
                                List<BusinessKeyProvisioningItem> batchItems,
                                Map<String, KeyImportItemResult> ackItemByPurpose,
                                String certSn, KeyProvisioningAckResult terminalResult) {
            this.batch = batch;
            this.batchItems = batchItems;
            this.ackItemByPurpose = ackItemByPurpose;
            this.certSn = certSn;
            this.terminalResult = terminalResult;
        }
    }

    private static ActivateContext terminal(KeyProvisioningAckResult result) {
        return new ActivateContext(null, java.util.Collections.emptyList(),
                java.util.Collections.emptyMap(), null, result);
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
