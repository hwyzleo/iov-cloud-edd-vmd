package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningIntent;
import net.hwyz.iov.security.keyprov.v1.CurrentKeyVersion;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessKeyProvisioningDomainService 单元测试（CR-058 Q4a=A）。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
class BusinessKeyProvisioningDomainServiceTest {

    private BusinessKeyProvisioningDomainService service;

    @BeforeEach
    void setUp() {
        service = new BusinessKeyProvisioningDomainService();
    }

    private KeyProvisioningRequest request(String idempotencyKey) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId("R-1")
                .setIdempotencyKey(idempotencyKey)
                .setBusinessDomain("V2C_COMM")
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addCurrentVersions(CurrentKeyVersion.newBuilder()
                        .setPurpose(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD).setBusinessKeyVersion(1))
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_HPKE_BASE_X25519_SHA256_AES128GCM)
                .build();
    }

    private DeviceBusinessKey active(long version) {
        return DeviceBusinessKey.builder().businessKeyVersion(version).build();
    }

    @Test
    @DisplayName("无 ACTIVE → INITIAL")
    void deriveIntent_noActive_initial() {
        assertEquals(BusinessKeyProvisioningIntent.INITIAL, service.deriveIntent(null, 0));
    }

    @Test
    @DisplayName("设备声明版本 == ACTIVE 版本 → REIMPORT（复用 ACTIVE 不新建版本）")
    void deriveIntent_sameVersion_reimport() {
        assertEquals(BusinessKeyProvisioningIntent.REIMPORT, service.deriveIntent(active(3), 3));
    }

    @Test
    @DisplayName("设备声明版本缺失或落后 → ROTATE（新建 PENDING）")
    void deriveIntent_missingOrStale_rotate() {
        assertEquals(BusinessKeyProvisioningIntent.ROTATE, service.deriveIntent(active(3), 0));
        assertEquals(BusinessKeyProvisioningIntent.ROTATE, service.deriveIntent(active(3), 2));
    }

    @Test
    @DisplayName("V2C 用途集合必须完整等于 {UPLINK_AEAD, DOWNLINK_AEAD}")
    void purposeCompleteness() {
        assertTrue(KeyprovConstants.isCompleteV2cPurposeSet(Set.of("UPLINK_AEAD", "DOWNLINK_AEAD")));
        assertFalse(KeyprovConstants.isCompleteV2cPurposeSet(Set.of("UPLINK_AEAD")));
        assertFalse(KeyprovConstants.isCompleteV2cPurposeSet(Set.of("UPLINK_AEAD", "DOWNLINK_AEAD", "EXTRA")));
        assertEquals(Set.of("UPLINK_AEAD", "DOWNLINK_AEAD"), service.normalizePurposes(request("IK")));
    }

    @Test
    @DisplayName("请求摘要确定且随 idempotencyKey / 派生版本 / 收件指纹变化")
    void requestDigestDeterministic() {
        String d1 = service.buildRequestDigest(request("IK-1"), "HSM", "VINDIG", "v1", "FP");
        String d2 = service.buildRequestDigest(request("IK-1"), "HSM", "VINDIG", "v1", "FP");
        String d3 = service.buildRequestDigest(request("IK-2"), "HSM", "VINDIG", "v1", "FP");
        String d4 = service.buildRequestDigest(request("IK-1"), "HSM", "VINDIG", "v2", "FP");
        String d5 = service.buildRequestDigest(request("IK-1"), "HSM", "VINDIG", "v1", "FP2");
        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, d4, "派生版本必须参与摘要");
        assertNotEquals(d1, d5, "收件证书指纹摘要必须参与摘要");
    }

    @Test
    @DisplayName("sha256HexPrefix 短前缀用于关联，不做原文回显；null/空白安全")
    void sha256HexPrefix() {
        String prefix = service.sha256HexPrefix("CR058-TBOX-0001");
        assertEquals(16, prefix.length());
        assertEquals(prefix, service.sha256HexPrefix("CR058-TBOX-0001"));
        assertNull(service.sha256HexPrefix(null));
        assertNull(service.sha256HexPrefix("   "));
    }

    @Test
    @DisplayName("封装套件协商在 allowlist 内选择，无交集返回 null")
    void negotiateWrappingSuite() {
        KeyProvisioningRequest req = request("IK");
        assertEquals(KeyprovConstants.SUITE_HPKE_BASE_X25519_SHA256_AES128GCM,
                service.negotiateWrappingSuite(req, List.of(KeyprovConstants.SUITE_HPKE_BASE_X25519_SHA256_AES128GCM)));
        assertNull(service.negotiateWrappingSuite(req, List.of(KeyprovConstants.SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)));
    }
}
