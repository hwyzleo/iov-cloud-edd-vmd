package net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.model.DeviceCertificateProfile;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CertificateProfileRegistry 单元测试（VMD-DSN-CR-063 §4.1）。
 * <p>
 * 覆盖：逻辑 Profile 严格解析、固定映射（v1 → FW DeviceCertificateProfile / deviceCategory=TBOX /
 * keyprovEligible=true）、禁用拒绝、legacy TBOX_TSP_CLIENT keyprovEligible=false、未知 Profile 拒绝、
 * 外部字符串不得当 Provisioner 名（framework-profile 启动期严格解析）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
class CertificateProfileRegistryTest {

    @Test
    void v1Enabled_requireEnabled_应返回固定映射() {
        CertificateProfileRegistry registry = new DefaultCertificateProfileRegistry(true, "TBOX_DEVICE_ECDH_ES_P256_V1");

        RegisteredCertificateProfile v1 = registry.requireEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1);
        assertEquals(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1, v1.vmdProfile());
        assertEquals(DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1, v1.frameworkProfile());
        assertEquals("TBOX", v1.deviceCategory());
        assertTrue(v1.keyprovEligible());
        assertTrue(v1.enabled());
        assertEquals("DIGITAL_SIGNATURE,KEY_AGREEMENT", v1.applyProfile().usage());
    }

    @Test
    void v1Disabled_requireEnabled_应拒绝() {
        CertificateProfileRegistry registry = new DefaultCertificateProfileRegistry(false, "TBOX_DEVICE_ECDH_ES_P256_V1");

        assertFalse(registry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1));
        assertThrows(CertificateProfileNotAllowedException.class,
                () -> registry.requireEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1));
        assertThrows(CertificateProfileNotAllowedException.class,
                () -> registry.requireEnabled("TBOX_DEVICE_ECDH_ES_P256_V1"));
    }

    @Test
    void legacyTspClient_应始终注册且非keyprovEligible() {
        CertificateProfileRegistry registry = new DefaultCertificateProfileRegistry(true, "TBOX_DEVICE_ECDH_ES_P256_V1");

        RegisteredCertificateProfile legacy = registry.requireEnabled("TBOX_TSP_CLIENT");
        assertEquals(VmdCertificateProfile.TBOX_TSP_CLIENT, legacy.vmdProfile());
        assertFalse(legacy.keyprovEligible());
        assertTrue(legacy.enabled());
        assertEquals("DIGITAL_SIGNATURE", legacy.applyProfile().usage());
        assertNull(legacy.frameworkProfile());
    }

    @Test
    void 外部字符串严格解析_未知Profile拒绝() {
        CertificateProfileRegistry registry = new DefaultCertificateProfileRegistry(true, "TBOX_DEVICE_ECDH_ES_P256_V1");

        assertThrows(CertificateProfileNotAllowedException.class, () -> registry.requireEnabled("UNKNOWN_PROFILE"));
        assertThrows(CertificateProfileNotAllowedException.class, () -> registry.requireEnabled(""));
        assertThrows(CertificateProfileNotAllowedException.class, () -> registry.requireEnabled((String) null));
        // 不 trim 内部、不大小写折叠
        assertThrows(CertificateProfileNotAllowedException.class,
                () -> registry.requireEnabled(" tbox_device_ecdh_es_p256_v1"));
    }

    @Test
    void frameworkProfile配置非法_启动期failFast() {
        assertThrows(IllegalStateException.class,
                () -> new DefaultCertificateProfileRegistry(true, "NOT_A_FW_PROFILE"));
        assertThrows(IllegalStateException.class,
                () -> new DefaultCertificateProfileRegistry(true, ""));
    }

    @Test
    void registeredProfiles_应含legacy与v1() {
        CertificateProfileRegistry registry = new DefaultCertificateProfileRegistry(true, "TBOX_DEVICE_ECDH_ES_P256_V1");
        assertEquals(Set.of(VmdCertificateProfile.TBOX_TSP_CLIENT, VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1),
                registry.registeredProfiles());
    }
}
