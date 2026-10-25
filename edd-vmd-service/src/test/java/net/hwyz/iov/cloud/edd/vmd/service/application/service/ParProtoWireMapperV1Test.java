package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.codec.CanonicalDerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.EcdhEsP256EnvelopeParametersV1;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
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
 * {@link ParProtoWireMapperV1} 单元测试（VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009 §2.1）。
 * <p>
 * 覆盖：9 字段 wire digest 计算、envelope_digest=wire（非 core）、canonical AAD 一致性断言、
 * parameters deterministic 序列化、view→wire 字段一一对应、缺 verifier/view 为空 fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class ParProtoWireMapperV1Test {

    private static final String REQUEST_ID = "CR068-REQ-0001";
    private static final String DOMAIN = "V2C_COMM";

    private ObjectProvider<DerivedEnvelopeDigestVerifier> verifierProvider;
    private ParProtoWireMapperV1 mapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        verifierProvider = mock(ObjectProvider.class);
        when(verifierProvider.getIfAvailable()).thenReturn(CanonicalDerivedEnvelopeDigestVerifier.instance());
        mapper = new ParProtoWireMapperV1(verifierProvider);
    }

    private DeviceBusinessKey directory() {
        return DeviceBusinessKey.builder().keyId("k").algorithm("AES")
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plus(Duration.ofDays(365)))
                .build();
    }

    private KeyProvisioningWireContext ctx() {
        return new KeyProvisioningWireContext(REQUEST_ID, DOMAIN);
    }

    @Test
    @DisplayName("有效 view → wire digest（9 段）写入 envelope_digest，core digest 不进 wire")
    void mapComputesWireDigest() throws Exception {
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        ParProtoWireMapperV1.WireMappedItem mapped = mapper.map(source, ctx(), directory());
        WrappedKeyItem item = mapped.item();
        EcdhEsP256EnvelopeViewV1 view = (EcdhEsP256EnvelopeViewV1) source.envelope();

        // envelope_digest == 独立重算的 9 字段 wire digest（且非 core digest）
        byte[] wireDigest = ParProtoWireCanonical.wireEnvelopeDigest(
                1, REQUEST_ID, DOMAIN,
                view.purpose() == KeyPurpose.UPLINK_AEAD ? 1 : 2,
                view.logicalKeyId(), view.businessKeyVersion(), 2,
                view.ciphertext(), item.getParameters().toByteArray());
        assertArrayEquals(wireDigest, item.getEnvelopeDigest().toByteArray());
        assertArrayEquals(wireDigest, mapped.wireEnvelopeDigest());
        assertFalse(java.util.Arrays.equals(mapped.coreEnvelopeDigest(), item.getEnvelopeDigest().toByteArray()),
                "core digest 不得作为 wire envelope_digest");

        // parameters deterministic bytes 可解析且字段一一对应；parameters/wrappedKey digest 与 bytes 一致
        EcdhEsP256EnvelopeParametersV1 parameters =
                EcdhEsP256EnvelopeParametersV1.parseFrom(item.getParameters().toByteArray());
        assertEquals(view.formatVersion(), parameters.getFormatVersion());
        assertArrayEquals(view.canonicalAad(), parameters.getCanonicalAad().toByteArray());
        assertArrayEquals(view.aadDigest(), parameters.getAadDigest().toByteArray());
        assertArrayEquals(view.recipientCertificateDigest(),
                parameters.getRecipientCertificateDigest().toByteArray());
        assertArrayEquals(ParProtoWireCanonical.sha256(item.getParameters().toByteArray()),
                mapped.parametersDigest());
        assertArrayEquals(ParProtoWireCanonical.sha256(view.ciphertext()), mapped.wrappedKeyDigest());
        assertEquals(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1, item.getEnvelopeFormat());
        assertArrayEquals(view.keyCheckValue(), item.getKeyCheckValue().toByteArray());
        assertEquals(ParProtoWireCanonical.AAD_PROFILE, mapped.aadProfileId(),
                "aad_profile_id 固定 PAR_PROTO_KEYPROV_AAD_V1");
    }

    @Test
    @DisplayName("AAD 一致性断言：view.aadDigest != SHA-256(canonicalAad) → 拒绝（不得加密后替换 AAD）")
    void mapAadDigestMismatchRejected() {
        EcdhEsP256EnvelopeViewV1 good = KeyprovEnvelopeTestFixtures.view(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        byte[] tamperedAadDigest = good.aadDigest();
        tamperedAadDigest[0] ^= 0x01;
        EcdhEsP256EnvelopeViewV1 bad = new EcdhEsP256EnvelopeViewV1(good.formatVersion(), good.wrappingSuite(),
                good.serverEphemeralPublicKey(), good.hkdfSalt(), good.hkdfContextVersion(), good.nonce(),
                good.ciphertext(), good.authenticationTag(), good.canonicalAad(), tamperedAadDigest,
                good.recipientCertificateDigest(), good.logicalKeyId(), good.purpose(), good.businessKeyVersion(),
                good.rootKeyVersion(), good.derivationVersion(), good.operationId(), good.envelopeDigest(),
                good.keyCheckValue());
        DerivedWrappedKey source = new DerivedWrappedKey(good.logicalKeyId(), "kms/ref/x", good.rootKeyVersion(),
                good.derivationVersion(), good.wrappingSuite(), bad, null, good.envelopeDigest(),
                good.keyCheckValue(), good.operationId());
        assertThrows(RuntimeException.class,
                () -> mapper.map(source, ctx(), directory()), "aad_digest 不一致必须拒绝（core 校验或 AAD 断言任一层）");
    }

    @Test
    @DisplayName("view 空 canonicalAad → framework 构造层 fail-closed 拒绝（mapper 无法接收空 AAD 视图）")
    void mapEmptyCanonicalAadRejected() {
        EcdhEsP256EnvelopeViewV1 good = KeyprovEnvelopeTestFixtures.view(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        assertThrows(IllegalArgumentException.class, () -> new EcdhEsP256EnvelopeViewV1(
                good.formatVersion(), good.wrappingSuite(),
                good.serverEphemeralPublicKey(), good.hkdfSalt(), good.hkdfContextVersion(), good.nonce(),
                good.ciphertext(), good.authenticationTag(), new byte[0], good.aadDigest(),
                good.recipientCertificateDigest(), good.logicalKeyId(), good.purpose(), good.businessKeyVersion(),
                good.rootKeyVersion(), good.derivationVersion(), good.operationId(), good.envelopeDigest(),
                good.keyCheckValue()), "空 canonicalAad 视图构造必须被拒");
    }

    @Test
    @DisplayName("verifier 未装配 → 拒绝（capability 未就绪，不下发）")
    void mapVerifierMissingRejected() {
        when(verifierProvider.getIfAvailable()).thenReturn(null);
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        assertThrows(KeyprovEnvelopeIntegrityException.class, () -> mapper.map(source, ctx(), directory()));
    }

    @Test
    @DisplayName("envelope=null（旧兼容构造）→ 拒绝，不回退解析 envelopeBytes")
    void mapNullEnvelopeRejected() {
        DerivedWrappedKey legacy = new DerivedWrappedKey("k", "r", 1L, "v1",
                net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                new byte[]{1}, new byte[32], new byte[3], "op");
        assertThrows(KeyprovEnvelopeIntegrityException.class, () -> mapper.map(legacy, ctx(), directory()));
    }

    @Test
    @DisplayName("目录行缺有效期 → fail-closed 拒绝下发（valid_from_ms=0 会被 SEC 拒）")
    void mapMissingValidityFailClosed() {
        DerivedWrappedKey source = KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                "logical-uplink", KeyPurpose.UPLINK_AEAD, 3L, 7L, "v1", "op-1");
        DeviceBusinessKey noValidity = DeviceBusinessKey.builder().keyId("k").algorithm("AES").build();
        KeyprovEnvelopeIntegrityException e = assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> mapper.map(source, ctx(), noValidity));
        assertTrue(e.getMessage().contains("有效期"), "异常信息应指明缺有效期");
    }

    @Test
    @DisplayName("确定性自检：固定输入两次计算一致且 32 字节")
    void deterministicSelfCheck() {
        assertTrue(ParProtoWireMapperV1.deterministicSelfCheck());
    }
}
