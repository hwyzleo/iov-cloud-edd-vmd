package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.KeyProvisioningLease;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.KeyProvisioningLeaseRepository;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * KL1 交付租约签发服务（VMD-DSN-CR-069 §2）。
 * <p>
 * ACK_PENDING（WAIT_DEVICE_ACK）批次按 KL-REQ-002 签发新租约：
 * <ul>
 *   <li>E = min(now + 60s, B - 30s)；E - now &lt; 20s → EXPIRED 响应（无 items，不 SUCCEEDED）；</li>
 *   <li>取已冻结原 Response raw bytes，仅对外层 field4 做受限 wire patch（禁止
 *       domain-map → protobuf 重建 items）；新 payload 摘要单独计算（delivery_payload_digest），
 *       不覆盖原 response_digest；</li>
 *   <li>原子落租约（批次+租约序号唯一、messageId 唯一、受保护 payload）并更新批次 lease_seq；</li>
 *   <li>同 requestId 重放绝不调用 Provider/重 wrap。</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Kl1LeaseIssuanceService {

    private final KeyProvisioningResponseStore responseStore;
    private final KeyProvisioningLeaseRepository leaseRepository;
    private final BusinessKeyProvisioningRepository provisioningRepository;
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
     * 签发结果：patched bytes 与新 E；或 EXPIRED 响应（无 items）。
     */
    public static final class LeaseIssueResult {
        private final KeyProvisioningResponse response;
        private final byte[] deliveryBytes;
        private final long expireAtMs;
        private final String messageId;

        private LeaseIssueResult(KeyProvisioningResponse response, byte[] deliveryBytes, long expireAtMs, String messageId) {
            this.response = response;
            this.deliveryBytes = deliveryBytes;
            this.expireAtMs = expireAtMs;
            this.messageId = messageId;
        }

        public KeyProvisioningResponse response() {
            return response;
        }

        public byte[] deliveryBytes() {
            return deliveryBytes;
        }

        public long expireAtMs() {
            return expireAtMs;
        }

        public String messageId() {
            return messageId;
        }

        public boolean expiredNoItems() {
            return deliveryBytes == null;
        }
    }

    /**
     * 为 ACK_PENDING 批次签发新租约（调用方须已在事务内持有批次行锁）。
     *
     * @param batch        已加锁的 KL1 批次（WAIT_DEVICE_ACK）
     * @param now          服务端 UTC 当前时间
     * @param safetyValid  KL1 readiness/证书/材料安全期限检查通过（不覆盖拟签发 E）
     * @return 签发结果；EXPIRED_NO_ITEMS 时 deliveryBytes 为 null
     * @throws KeyprovEnvelopeIntegrityException 原响应缺失/损坏（fail-closed，STORE_QUARANTINE）
     */
    public LeaseIssueResult issue(BusinessKeyProvisioning batch, LocalDateTime now, boolean safetyValid) {
        if (batch == null || !batch.isKl1()
                || batch.getStatus() != BusinessKeyProvisioningState.WAIT_DEVICE_ACK) {
            throw new KeyprovEnvelopeIntegrityException("KL1 租约签发前提不满足: requestId="
                    + (batch == null ? "null" : batch.getRequestId()) + ", state="
                    + (batch == null ? "null" : batch.getStatus()));
        }
        long nowMs = toEpochMs(now);
        Kl1LifecycleDecide.IssueDecision decision = Kl1LifecycleDecide.issue(nowMs, toEpochMs(batch.getAckDeadlineAt()), safetyValid);
        if ("EXPIRED_NO_ITEMS".equals(decision.action())) {
            // LC03：返回 EXPIRED 无 items，不 SUCCEEDED
            return new LeaseIssueResult(expiredResponse(batch.getRequestId()), null, 0L, null);
        }
        if ("SECURITY_BLOCKED".equals(decision.action())) {
            throw new KeyprovEnvelopeIntegrityException("KL1 安全期限不覆盖拟签发 E，停止交付: requestId=" + batch.getRequestId());
        }
        // 取已冻结原响应 raw bytes → 仅 patch 唯一外层 field4 → delivery_payload_digest
        byte[] original = responseStore.load(batch.getRequestId());
        if (original == null) {
            throw new KeyprovEnvelopeIntegrityException("KL1 原响应缺失，STORE_QUARANTINE（禁止重新 derive 补齐）: requestId=" + batch.getRequestId());
        }
        byte[] patched = Kl1WireCodec.patchField4(original, decision.expireAtMs());
        KeyProvisioningResponse parsed;
        try {
            parsed = KeyProvisioningResponse.parseFrom(patched);
        } catch (Exception e) {
            throw new KeyprovEnvelopeIntegrityException("KL1 patch 后响应损坏（fail-closed）: requestId=" + batch.getRequestId(), e);
        }
        final byte[] delivery = patched;
        final long expire = decision.expireAtMs();
        final String messageId = newMessageId();
        // 原子落租约（批次+租约序号唯一；messageId 唯一；受保护 payload）并更新批次 lease_seq
        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = provisioningRepository.selectByRequestIdForUpdate(batch.getRequestId());
            if (current == null || !current.getStatus().isOpen()) {
                throw new KeyprovEnvelopeIntegrityException("KL1 签发时批次状态已变: requestId=" + batch.getRequestId());
            }
            int seq = (current.getLeaseSeq() == null ? 0 : current.getLeaseSeq()) + 1;
            KeyProvisioningLease lease = KeyProvisioningLease.builder()
                    .provisioningId(current.getId())
                    .leaseSeq(seq)
                    .issuedAt(now)
                    .expireAt(toLocalDateTime(expire))
                    .messageId(messageId)
                    .correlationId(batch.getRequestId())
                    .deliveryPayloadDigest(hex(Kl1WireCodec.deliveryPayloadDigest(delivery)))
                    .contentDigest(current.getImmutableContentDigest())
                    .payload(responseStore.seal(delivery))
                    .createTime(now)
                    .modifyTime(now)
                    .build();
            lease.init();
            leaseRepository.insert(lease);
            current.setLeaseSeq(seq);
            current.setModifyTime(now);
            provisioningRepository.update(current);
        });
        log.info("KL1 租约签发: requestId={}, leaseSeq={}, messageId={}, E={}", batch.getRequestId(), messageId, expire);
        return new LeaseIssueResult(parsed, delivery, expire, messageId);
    }

    private KeyProvisioningResponse expiredResponse(String requestId) {
        return KeyProvisioningResponse.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_EXPIRED)
                .setError(ProtocolError.newBuilder()
                        .setCode(KeyprovConstants.ERR_EXPIRED)
                        .setRetryable(false)
                        .setAction("DO_NOT_RETRY")
                        .setMessage("bundle到期无items可交付，请以原requestId查询Status")
                        .build())
                .build();
    }

    private static String newMessageId() {
        return "kl1-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static long toEpochMs(LocalDateTime time) {
        return time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime toLocalDateTime(long epochMs) {
        return java.time.Instant.ofEpochMilli(epochMs)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
