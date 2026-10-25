package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.codec.CanonicalDerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.exception.DerivedEnvelopeIntegrityException;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite;
import net.hwyz.iov.security.keyprov.v1.EcdhEsP256EnvelopeParametersV1;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link KeyprovEnvelopeAdapter} 单元测试（VMD-DSN-CR-062 §4.1/§4.2）。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
class KeyprovEnvelopeAdapterTest {

    private ObjectProvider<DerivedEnvelopeDigestVerifier> verifierProvider;
    private ParProtoWireMapperV1 wireMapper;
    private KeyprovEnvelopeAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        verifierProvider = mock(ObjectProvider.class);
        when(verifierProvider.getIfAvailable()).thenReturn(CanonicalDerivedEnvelopeDigestVerifier.instance());
        wireMapper = new ParProtoWireMapperV1(verifierProvider);
        adapter = new KeyprovEnvelopeAdapter(wireMapper);
    }

    private DeviceBusinessKey directory() {
        return DeviceBusinessKey.builder().keyId("k").algorithm("AES")
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plus(Duration.ofDays(365)))
                .build();
    }

    @Test
    @DisplayName("声明 ECDH-ES + ECDH_ES_P256_V1 → 命中 allowlist")
    void negotiate_declared_ok() {
        KeyProvisioningRequest request = KeyProvisioningRequest.newBuilder()
                .addSupportedWrappingSuites(
                        net.hwyz.iov.security.keyprov.v1.WrappingSuite
                                .WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .build();
        assertEquals(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1,
                adapter.negotiateEnvelopeFormat(request));
    }

    @Test
    @DisplayName("缺失 format / 未声明 / UNSPECIFIED / suite 缺失 → null（fail-closed 不下发）")
    void negotiate_missing_failClosed() {
        assertNull(adapter.negotiateEnvelopeFormat(KeyProvisioningRequest.newBuilder()
                .addSupportedWrappingSuites(
                        net.hwyz.iov.security.keyprov.v1.WrappingSuite
                                .WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .build()), "未声明 envelope format 不得下发 v1");
        assertNull(adapter.negotiateEnvelopeFormat(KeyProvisioningRequest.newBuilder()
                .addSupportedWrappingSuites(
                        net.hwyz.iov.security.keyprov.v1.WrappingSuite
                                .WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_UNSPECIFIED)
                .build()), "UNSPECIFIED 不得下发 v1");
        assertNull(adapter.negotiateEnvelopeFormat(KeyProvisioningRequest.newBuilder()
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .build()), "未声明 ECDH-ES suite 不得下发 v1");
    }

    @Test
    @DisplayName("有效 view → WrappedKeyItem 无损映射 + deterministic parameters + digest/KCV/format")
    void toWrappedKeyItem_mapsLosslessly() throws Exception {
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        WrappedKeyItem item = adapter.toWrappedKeyItem("CR068-REQ-0001", "V2C_COMM", directory(), source);

        EcdhEsP256EnvelopeViewV1 view = (EcdhEsP256EnvelopeViewV1) source.envelope();
        assertEquals(view.logicalKeyId(), item.getKeyId());
        assertEquals(view.businessKeyVersion(), item.getBusinessKeyVersion());
        assertEquals(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1, item.getEnvelopeFormat());
        // 目录/KMS 通用算法标签 "AES" → wire 必须输出协议 canonical "AES-256"
        // （PAR-PROTO §7.2：ECDH-ES v1 固定 AES-256；SEC validateEnvelope 硬编码 AES-256）。
        assertEquals(KeyprovConstants.KEY_ALGORITHM_ECDH_ES_V1, item.getKeyAlgorithm());
        assertEquals(net.hwyz.iov.security.keyprov.v1.WrappingSuite
                        .WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                item.getWrappingSuite());
        assertArrayEquals(view.ciphertext(), item.getWrappedKey().toByteArray(), "wrappedKey = ciphertext");
        // CR-068：envelope_digest = PAR-PROTO 9 字段 wire digest（非 FW core digest）
        byte[] wireDigest = ParProtoWireCanonical.wireEnvelopeDigest(
                1, "CR068-REQ-0001", "V2C_COMM",
                view.purpose() == KeyPurpose.UPLINK_AEAD ? 1 : 2,
                view.logicalKeyId(), view.businessKeyVersion(), 2,
                view.ciphertext(), item.getParameters().toByteArray());
        assertArrayEquals(wireDigest, item.getEnvelopeDigest().toByteArray(),
                "envelope_digest 必须是 wire digest（非 core）");
        assertFalse(java.util.Arrays.equals(view.coreEnvelopeDigest(), item.getEnvelopeDigest().toByteArray()),
                "core digest 不得进入 wire envelope_digest");
        assertArrayEquals(view.keyCheckValue(), item.getKeyCheckValue().toByteArray());
        assertTrue(item.getValidFromMs() > 0, "目录有效期必须下发 valid_from_ms（缺失=0 会被 SEC 拒）");
        assertTrue(item.getValidToMs() > item.getValidFromMs(), "valid_to_ms 必须大于 valid_from_ms");

        EcdhEsP256EnvelopeParametersV1 parameters =
                EcdhEsP256EnvelopeParametersV1.parseFrom(item.getParameters().toByteArray());
        assertEquals(view.formatVersion(), parameters.getFormatVersion());
        assertEquals(view.rootKeyVersion(), parameters.getRootKeyVersion());
        assertEquals(view.operationId(), parameters.getOperationId());
        assertArrayEquals(view.serverEphemeralPublicKey(), parameters.getServerEphemeralPublicKey().toByteArray());
        assertArrayEquals(view.aadDigest(), parameters.getAadDigest().toByteArray());
        assertArrayEquals(view.recipientCertificateDigest(),
                parameters.getRecipientCertificateDigest().toByteArray());
    }

    @Test
    @DisplayName("envelope=null（旧兼容构造）→ 拒绝，不回退解析 envelopeBytes")
    void toWrappedKeyItem_nullEnvelope_rejected() {
        DerivedWrappedKey legacy = new DerivedWrappedKey("k", "r", 1L, "v1",
                WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM, new byte[]{1}, new byte[32], new byte[3], "op");
        assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> adapter.toWrappedKeyItem("CR068-REQ-0001", "V2C_COMM", directory(), legacy));
    }

    @Test
    @DisplayName("view→canonical 摘要错配 → Proto 映射前整批拒绝")
    void toWrappedKeyItem_digestMismatch_rejected() {
        EcdhEsP256EnvelopeViewV1 good = KeyprovEnvelopeTestFixtures.view(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        byte[] tampered = good.envelopeDigest();
        tampered[0] ^= 0x01;
        EcdhEsP256EnvelopeViewV1 bad = new EcdhEsP256EnvelopeViewV1(good.formatVersion(), good.wrappingSuite(),
                good.serverEphemeralPublicKey(), good.hkdfSalt(), good.hkdfContextVersion(), good.nonce(),
                good.ciphertext(), good.authenticationTag(), good.canonicalAad(), good.aadDigest(),
                good.recipientCertificateDigest(), good.logicalKeyId(), good.purpose(), good.businessKeyVersion(),
                good.rootKeyVersion(), good.derivationVersion(), good.operationId(), tampered, good.keyCheckValue());
        DerivedWrappedKey source = new DerivedWrappedKey(good.logicalKeyId(), "kms/ref/x", good.rootKeyVersion(),
                good.derivationVersion(), good.wrappingSuite(), bad, null, tampered, good.keyCheckValue(),
                good.operationId());
        assertThrows(DerivedEnvelopeIntegrityException.class,
                () -> adapter.toWrappedKeyItem("CR068-REQ-0001", "V2C_COMM", directory(), source));
    }

    @Test
    @DisplayName("verifier 未装配 → 拒绝（capability 未就绪，不下发）")
    void toWrappedKeyItem_verifierMissing_rejected() {
        when(verifierProvider.getIfAvailable()).thenReturn(null);
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> adapter.toWrappedKeyItem("CR068-REQ-0001", "V2C_COMM", directory(), source));
    }

    @Test
    @DisplayName("目录行缺有效期（validFrom/validTo 缺失）→ fail-closed 拒绝下发 envelope")
    void toWrappedKeyItem_missingValidity_failClosed() {
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        DeviceBusinessKey noValidity = DeviceBusinessKey.builder().keyId("k").algorithm("AES").build();
        KeyprovEnvelopeIntegrityException e = assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> adapter.toWrappedKeyItem("CR068-REQ-0001", "V2C_COMM", noValidity, source));
        assertTrue(e.getMessage().contains("有效期"), "异常信息应指明缺有效期");
    }
}
