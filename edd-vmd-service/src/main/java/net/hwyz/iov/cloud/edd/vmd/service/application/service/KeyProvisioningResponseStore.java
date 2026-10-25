package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Keyprov 最终 Response 字节级幂等重放存储（VMD-DSN-CR-068 §4/§6，KEYPROV_RESPONSE_REPLAY_STORE_READY）。
 * <p>
 * 在批次表（business_key_provisioning，幂等键=requestId）受控保存最终 KeyProvisioningResponse 事实：
 * <ul>
 *   <li>response_payload：AES-GCM 加密 BLOB（nonce(12) ‖ ciphertext+tag(16)），密钥经 Secret 注入
 *       （{@code vmd.business-key.provisioning.response-store.key}，Base64 AES-256），不在 Nacos/日志输出；</li>
 *   <li>response_digest：SHA-256(final response bytes) hex，重放完整性校验；</li>
 *   <li>response_codec_version：固定 {@code PAR_PROTO_KEYPROV_AAD_V1}；</li>
 *   <li>response_persisted_at：首次持久化时间（重放不更新）。</li>
 * </ul>
 * 约束（RD-068-3/用户评审 2026-10-25）：Response 必须在首次发送前持久化；同 requestId 重放直接返回
 * 原 bytes，不重新选择 item、不重新组装、不重新 derive/serialize/map；Adapter 升级不改变存量 in-flight
 * 响应。response_payload 与 response_ref 二选一（本实现使用 response_payload）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Component
public class KeyProvisioningResponseStore {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_LENGTH = 12;
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningMetrics metrics;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${vmd.business-key.provisioning.response-store.key:}")
    private String base64EncryptionKey;

    public KeyProvisioningResponseStore(BusinessKeyProvisioningRepository provisioningRepository,
                                        BusinessKeyProvisioningMetrics metrics) {
        this.provisioningRepository = provisioningRepository;
        this.metrics = metrics;
    }

    /**
     * Response 字节级重放存储是否就绪：加密密钥已注入且仓库可用。
     * 未就绪时 keyprov Health 的 KEYPROV_RESPONSE_REPLAY_STORE_READY 为 DOWN，禁止接新流量。
     */
    public boolean productionReady() {
        return base64EncryptionKey != null && !base64EncryptionKey.isBlank();
    }

    /**
     * 首次发送前持久化最终 Response 字节（幂等键=requestId）。
     *
     * @throws KeyprovEnvelopeIntegrityException 批次不存在、密钥未配置或加密失败
     */
    public void persist(String requestId, byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length == 0) {
            throw new KeyprovEnvelopeIntegrityException("Response 字节为空，拒绝持久化");
        }
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        if (batch == null) {
            throw new KeyprovEnvelopeIntegrityException("Response 持久化失败：批次不存在 requestId=" + requestId);
        }
        batch.setResponsePayload(encrypt(responseBytes));
        batch.setResponseDigest(hex(ParProtoWireCanonical.sha256(responseBytes)));
        batch.setResponseCodecVersion(ParProtoWireCanonical.AAD_PROFILE);
        batch.setResponsePersistedAt(LocalDateTime.now());
        batch.setModifyTime(LocalDateTime.now());
        provisioningRepository.update(batch);
        metrics.recordResponseStore("persist", ParProtoWireCanonical.AAD_PROFILE, "success");
    }

    /**
     * KL1：首次完整双用途响应原子持久化并建立 F/B 与不可变内容摘要（VMD-DSN-CR-069 §1/§5）。
     * <p>
     * 原 response_payload/response_digest/codec/persisted_at 不覆盖；F=首次持久化时间（不可滑动），
     * B=F+300s；immutable_content_digest 从已验证原始 bytes 计算（去掉唯一外层 field4 的 C）。
     * 重跑迁移/续签均不重置 F/B。
     *
     * @throws KeyprovEnvelopeIntegrityException 批次不存在、密钥未配置、加密失败或响应缺失 field4
     */
    public void persistKl1(String requestId, byte[] responseBytes, LocalDateTime now) {
        if (responseBytes == null || responseBytes.length == 0) {
            throw new KeyprovEnvelopeIntegrityException("KL1 Response 字节为空，拒绝持久化");
        }
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        if (batch == null) {
            throw new KeyprovEnvelopeIntegrityException("KL1 Response 持久化失败：批次不存在 requestId=" + requestId);
        }
        byte[] digest = Kl1WireCodec.immutableContentDigest(responseBytes);
        batch.setResponsePayload(encrypt(responseBytes));
        batch.setResponseDigest(hex(ParProtoWireCanonical.sha256(responseBytes)));
        batch.setResponseCodecVersion(ParProtoWireCanonical.AAD_PROFILE);
        batch.setResponsePersistedAt(now);
        batch.setLifecycleProfile(Kl1Constants.PROFILE);
        batch.setFirstIssuedAt(now);
        batch.setAckDeadlineAt(now.plus(java.time.Duration.ofMillis(Kl1Constants.BATCH_MS)));
        batch.setImmutableContentDigest(hex(digest));
        batch.setModifyTime(now);
        provisioningRepository.update(batch);
        metrics.recordResponseStore("persist_kl1", ParProtoWireCanonical.AAD_PROFILE, "success");
    }

    /**
     * 受保护 payload 加密（租约表复用同一密钥；与批次 response_payload 同一受控策略）。
     */
    public byte[] seal(byte[] plain) {
        return encrypt(plain);
    }

    /**
     * 受保护 payload 解密。
     */
    public byte[] open(byte[] sealed) {
        return decrypt(sealed);
    }

    /**
     * 按 requestId 读取原 Response 字节（重放）。返回 {@code null} 表示批次不存在或尚无持久化响应。
     *
     * @throws KeyprovEnvelopeIntegrityException 解密失败或 digest 校验不一致（fail-closed，禁止伪造重放）
     */
    public byte[] load(String requestId) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        if (batch == null || batch.getResponsePayload() == null) {
            return null;
        }
        byte[] plain = decrypt(batch.getResponsePayload());
        if (batch.getResponseDigest() != null
                && !batch.getResponseDigest().equalsIgnoreCase(hex(ParProtoWireCanonical.sha256(plain)))) {
            throw new KeyprovEnvelopeIntegrityException("Response 重放字节完整性校验失败，拒绝返回");
        }
        metrics.recordResponseStore("replay", ParProtoWireCanonical.AAD_PROFILE, "success");
        return plain;
    }

    private byte[] encrypt(byte[] plain) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plain);
            byte[] sealed = new byte[NONCE_LENGTH + ciphertext.length];
            System.arraycopy(nonce, 0, sealed, 0, NONCE_LENGTH);
            System.arraycopy(ciphertext, 0, sealed, NONCE_LENGTH, ciphertext.length);
            return sealed;
        } catch (Exception e) {
            throw new KeyprovEnvelopeIntegrityException("Response 加密失败（fail-closed）", e);
        }
    }

    private byte[] decrypt(byte[] sealed) {
        try {
            if (sealed == null || sealed.length < NONCE_LENGTH + GCM_TAG_BITS / 8) {
                throw new KeyprovEnvelopeIntegrityException("Response 密文长度非法");
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            System.arraycopy(sealed, 0, nonce, 0, NONCE_LENGTH);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(sealed, NONCE_LENGTH, sealed.length - NONCE_LENGTH);
        } catch (KeyprovEnvelopeIntegrityException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyprovEnvelopeIntegrityException("Response 解密失败（fail-closed，不得回退重放）", e);
        }
    }

    private SecretKeySpec keySpec() {
        if (!productionReady()) {
            throw new KeyprovEnvelopeIntegrityException("Response 存储密钥未配置（vmd.business-key.provisioning.response-store.key），禁止重放");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64EncryptionKey.trim());
        } catch (IllegalArgumentException e) {
            throw new KeyprovEnvelopeIntegrityException("Response 存储密钥非法（非 Base64）", e);
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new KeyprovEnvelopeIntegrityException("Response 存储密钥长度非法（需 16/24/32 字节）");
        }
        return new SecretKeySpec(key, "AES");
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
