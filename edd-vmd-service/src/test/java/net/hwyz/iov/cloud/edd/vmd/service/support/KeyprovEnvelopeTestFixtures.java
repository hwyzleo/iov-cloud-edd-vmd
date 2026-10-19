package net.hwyz.iov.cloud.edd.vmd.service.support;

import net.hwyz.iov.cloud.framework.security.crypto.codec.CanonicalEncoding;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeViewFactory;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedKeyEnvelopeCanonical;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKeyEnvelopeV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite;

import java.nio.charset.StandardCharsets;

/**
 * VMD CR-062 测试用信封材料工厂。
 * <p>
 * 使用 framework 公开 canonical 编码器冻结 {@code envelopeDigest}，构造通过
 * {@code DerivedEnvelopeDigestVerifier.verify(view)} 的结构化视图与 {@link DerivedWrappedKey}，
 * 供协议 Adapter / 批次 / ACK 测试使用；不进入生产代码路径。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
public final class KeyprovEnvelopeTestFixtures {

    public static final String ENVELOPE_FORMAT = EcdhEsP256EnvelopeViewV1.ENVELOPE_FORMAT;
    public static final String RECIPIENT_FINGERPRINT =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    /** 与 {@link #RECIPIENT_FINGERPRINT} 对应的 32 字节证书策略摘要。 */
    public static final byte[] RECIPIENT_CERT_DIGEST = CanonicalEncoding.hexDecode(RECIPIENT_FINGERPRINT);

    private KeyprovEnvelopeTestFixtures() {
    }

    /**
     * 构造一个通过 canonical 校验的 {@link DerivedWrappedKey}。
     */
    public static DerivedWrappedKey derivedWrappedKey(String logicalKeyId, KeyPurpose purpose, long businessKeyVersion,
                                                      long rootKeyVersion, String derivationVersion, String operationId) {
        return derivedWrappedKey(logicalKeyId, purpose, businessKeyVersion, rootKeyVersion,
                derivationVersion, operationId, RECIPIENT_FINGERPRINT);
    }

    /**
     * 构造一个通过 canonical 校验的 {@link DerivedWrappedKey}，收件证书指纹可指定
     * （用于真实 ImportProofVerifier 端到端测试：指纹必须等于 SHA-256(certDer) 十六进制）。
     */
    public static DerivedWrappedKey derivedWrappedKey(String logicalKeyId, KeyPurpose purpose, long businessKeyVersion,
                                                      long rootKeyVersion, String derivationVersion, String operationId,
                                                      String recipientFingerprint) {
        byte[] kcv = {0x0a, 0x0b, 0x0c};
        DerivedWrappedKeyEnvelopeV1 envelope = envelope(logicalKeyId, purpose, businessKeyVersion, rootKeyVersion,
                derivationVersion, operationId, kcv, recipientFingerprint);
        byte[] digest = DerivedKeyEnvelopeCanonical.envelopeDigest(envelope);
        envelope = new DerivedWrappedKeyEnvelopeV1(envelope.envelopeVersion(), envelope.wrappingSuite(),
                envelope.serverEphemeralPublicKey(), envelope.hkdfSalt(), envelope.hkdfContextVersion(),
                envelope.nonce(), envelope.ciphertext(), envelope.authenticationTag(), envelope.aad(),
                envelope.aadDigest(), envelope.recipientCertificateFingerprint(), envelope.logicalKeyId(),
                envelope.purpose(), envelope.businessKeyVersion(), envelope.rootKeyVersion(),
                envelope.derivationVersion(), envelope.kcv(), envelope.operationId(), digest);
        EcdhEsP256EnvelopeViewV1 view = DerivedEnvelopeViewFactory.toEcdhEsP256View(envelope);
        return new DerivedWrappedKey(logicalKeyId, "kms/ref/" + logicalKeyId, rootKeyVersion, derivationVersion,
                WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM, view,
                DerivedKeyEnvelopeCanonical.envelopeBytesWithoutDigest(envelope), digest, kcv, operationId);
    }

    public static EcdhEsP256EnvelopeViewV1 view(String logicalKeyId, KeyPurpose purpose, long businessKeyVersion,
                                                long rootKeyVersion, String derivationVersion, String operationId) {
        return (EcdhEsP256EnvelopeViewV1) derivedWrappedKey(logicalKeyId, purpose, businessKeyVersion,
                rootKeyVersion, derivationVersion, operationId).envelope();
    }

    /**
     * 构造 canonical 信封（recipientCertificateFingerprint 固定为测试指纹）。
     */
    public static DerivedWrappedKeyEnvelopeV1 envelope(String logicalKeyId, KeyPurpose purpose,
                                                       long businessKeyVersion, long rootKeyVersion,
                                                       String derivationVersion, String operationId, byte[] kcv) {
        return envelope(logicalKeyId, purpose, businessKeyVersion, rootKeyVersion, derivationVersion,
                operationId, kcv, RECIPIENT_FINGERPRINT);
    }

    /**
     * 构造 canonical 信封（收件证书指纹可指定）。
     */
    public static DerivedWrappedKeyEnvelopeV1 envelope(String logicalKeyId, KeyPurpose purpose,
                                                       long businessKeyVersion, long rootKeyVersion,
                                                       String derivationVersion, String operationId, byte[] kcv,
                                                       String recipientFingerprint) {
        byte[] ephemeral = new byte[65];
        ephemeral[0] = 0x04;
        for (int i = 1; i < 65; i++) {
            ephemeral[i] = (byte) i;
        }
        byte[] salt = new byte[32];
        byte[] nonce = new byte[12];
        for (int i = 0; i < 32; i++) {
            salt[i] = (byte) (0x10 + i);
        }
        for (int i = 0; i < 12; i++) {
            nonce[i] = (byte) (0xA0 + i);
        }
        byte[] ciphertext = ("ciphertext-" + logicalKeyId).getBytes(StandardCharsets.UTF_8);
        byte[] tag = new byte[16];
        byte[] aad = ("aad-" + logicalKeyId).getBytes(StandardCharsets.UTF_8);
        byte[] aadDigest = CanonicalEncoding.sha256(aad);
        return new DerivedWrappedKeyEnvelopeV1(DerivedWrappedKeyEnvelopeV1.V1,
                WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM, ephemeral, salt,
                DerivedWrappedKeyEnvelopeV1.HKDF_CONTEXT_V1, nonce, ciphertext, tag, aad, aadDigest,
                recipientFingerprint, logicalKeyId, purpose, businessKeyVersion, rootKeyVersion,
                derivationVersion, kcv, operationId, null);
    }
}
