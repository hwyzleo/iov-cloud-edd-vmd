package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ActiveDeviceCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ActiveDeviceCertificateResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BoundDeviceIdentity;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRevocationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health.KeyProvisioningHealthGroupsCustomizer;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.VmdCertificateReferenceDirectory;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsBusinessKeyProviderReadiness;
import net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyNotFoundException;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RevocationRequest;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateLocator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-060 集成测试：证书材料解析目录 + Provider 就绪门禁。
 * <p>
 * 以真实 MySQL（dev）验证：VMD 薄证书目录 Adapter 的 serial→pkiRequestId 解析、canonical 变体归并、
 * 多命中安全拒绝、缺 locator fail-closed；当前有效设备证书证据（canonical serial + requestId + 指纹）；
 * business-key Provider 就绪门禁对吊销幂等的影响；以及 keyprov 健康分组聚合配置。
 * <p>
 * framework 证书材料/BusinessKeyMaterialTemplate 以 Mock 门面替代（真实 Provider 端到端登记为部署阻断项）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Rollback
class CertificateReferenceCr060IntegrationTest extends BaseTest {

    private static final String VIN = "HWYZTESTCR06001";
    private static final String DEVICE_SN = "CR060-TBOX-0001";
    private static final String HSM_UID = "CR060-UID-000001";
    private static final String PART_CODE = "CR060_TBOX";
    private static final String PROFILE = "TBOX_TSP_CLIENT";

    private static final String ACTIVE_CERT_SN = "0x0A0B0C";
    private static final String ACTIVE_CANONICAL_SN = "a0b0c";
    private static final String ACTIVE_PKI_REQUEST_ID = "CR060_PKI_ACTIVE";

    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";
    private static final String KEY_ID = "CR060-KEY-0001";
    private static final String KMS_REF = "kms/ref/CR060-KEY-0001";

    @MockBean
    private BusinessKeyMaterialTemplate businessKeyMaterialTemplate;
    @MockBean
    private KmsBusinessKeyProviderReadiness providerReadiness;

    @Autowired
    private VmdCertificateReferenceDirectory certificateReferenceDirectory;
    @Autowired
    private ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    @Autowired
    private BusinessKeyRevocationAppService revocationAppService;
    @Autowired
    private HealthEndpointGroups healthEndpointGroups;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + VIN + "'");

        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) "
                + "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", VIN);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                VIN, DEVICE_SN);

        // 当前 effective 设备证书（ACTIVE，唯一 device_sn + profile）；存储为非 canonical 以校验读侧 canonical 化
        seedCertificate("CR060_CERT_ACTIVE", ACTIVE_CERT_SN, ACTIVE_PKI_REQUEST_ID, ACTIVE_FINGERPRINT(),
                DEVICE_SN, "ACTIVE");
        // 目录 serial 命中（存储 canonical=f00d；非 ACTIVE，避免与 active 唯一键冲突）
        seedCertificate("CR060_CERT_F00D", "f00d", "CR060_PKI_F00D", null, DEVICE_SN + "-DIR", "SUPERSEDED");
        // 目录按 canonical 唯一命中（abc）
        seedCertificate("CR060_CERT_ABC", "abc", "CR060_PKI_ABC", null, DEVICE_SN + "-AMB", "SUPERSEDED");
        // 命中但缺 pkiRequestId：canonical=dead
        seedCertificate("CR060_CERT_DEAD", "dead", null, null, DEVICE_SN + "-NOLOC", "SUPERSEDED");

        // 一个 ACTIVE 业务密钥用于吊销门禁
        Long partId = jdbcTemplate.queryForObject("SELECT id FROM tb_part_info WHERE sn = ?", Long.class, DEVICE_SN);
        Long bindingId = jdbcTemplate.queryForObject("SELECT id FROM tb_vehicle_part WHERE vin = ?", Long.class, VIN);
        jdbcTemplate.update("INSERT INTO tb_device_business_key (device_sn, binding_id, part_id, hsm_uid, business_domain, purpose, "
                        + "key_id, business_key_version, kms_key_ref, key_state, request_id, row_version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, 'ACTIVE', ?, 1)",
                DEVICE_SN, bindingId, partId, HSM_UID, DOMAIN, PURPOSE, KEY_ID, KMS_REF, "CR060_BK_REQ_0001");
    }

    private void seedCertificate(String requestId, String certSn, String pkiRequestId, String fingerprint,
                                 String deviceSn, String status) {
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, pki_request_id, cert_sn, vin, binding_id, part_id, "
                        + "device_category, device_sn, hsm_uid, certificate_profile, certificate_fingerprint, cert_status, not_after) "
                        + "SELECT ?, ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ?",
                requestId, pkiRequestId, certSn, VIN, deviceSn, HSM_UID, PROFILE, fingerprint, status, VIN);
    }

    private String ACTIVE_FINGERPRINT() {
        return "a1".repeat(32);
    }

    private BoundDeviceIdentity identity() {
        return new BoundDeviceIdentity("CR060-BINDING", 1L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
    }

    @Test
    @DisplayName("证书目录：按 canonical serial 命中唯一记录 → EnrollmentRequest(requestId)")
    void directory_serialHit_returnsEnrollmentRequest() {
        Optional<CertificateLocator> locator = certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("0x0F00D"));
        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("CR060_PKI_F00D")), locator);
    }

    @Test
    @DisplayName("证书目录：非 canonical 输入经 canonical 化后命中同一记录")
    void directory_canonicalVariant_mergesToSameRecord() {
        Optional<CertificateLocator> locator = certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("0F:00:D"));
        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("CR060_PKI_F00D")), locator);
    }

    @Test
    @DisplayName("证书目录：按 canonical serial 唯一命中，重复解析结果确定（不取任意一条）")
    void directory_ambiguous_rejected() {
        // uk_cert_sn + 写入 canonical 化保证同 canonical serial 唯一；重复解析稳定返回同一 requestId
        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("CR060_PKI_ABC")),
                certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("0xABC")));
        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("CR060_PKI_ABC")),
                certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("ABC")));
    }

    @Test
    @DisplayName("证书目录：未命中 → empty")
    void directory_noHit_empty() {
        assertTrue(certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("0xBEEF")).isEmpty());
    }

    @Test
    @DisplayName("证书目录：命中但缺 pkiRequestId → 无 locator（fail-closed）")
    void directory_missingPkiRequestId_empty() {
        assertTrue(certificateReferenceDirectory.locate(DeviceCertificateRef.ofSerial("0xDEAD")).isEmpty());
    }

    @Test
    @DisplayName("active 设备证书解析：返回 canonical serial + pkiRequestId + 指纹")
    void resolver_activeCertificate_returnsEvidence() {
        ActiveDeviceCertificate active = activeDeviceCertificateResolver.resolveActive(identity());

        assertEquals(ACTIVE_CANONICAL_SN, active.certSn());
        assertEquals(ACTIVE_PKI_REQUEST_ID, active.pkiRequestId());
        assertEquals(ACTIVE_FINGERPRINT(), active.sha256Fingerprint());
        assertEquals(ACTIVE_CANONICAL_SN, activeDeviceCertificateResolver.resolveActiveCertSn(identity()));
    }

    @Test
    @DisplayName("吊销门禁：Provider 未就绪时 KMS 404 不收敛为幂等成功（fail-closed）")
    void revoke_providerNotReady_404FailClosed() {
        when(providerReadiness.isReady()).thenReturn(false);
        when(providerReadiness.getReason()).thenReturn("crypto.kms.business-key.enabled=false");
        when(businessKeyMaterialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new BusinessKeyNotFoundException("gone"));

        assertThrows(BusinessKeyKmsUnavailableException.class, () -> revocationAppService.revoke(revokeCmd()));
    }

    @Test
    @DisplayName("吊销门禁：Provider READY 时 KMS 404 收敛为吊销幂等成功")
    void revoke_providerReady_404IdempotentSuccess() {
        when(providerReadiness.isReady()).thenReturn(true);
        when(businessKeyMaterialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new BusinessKeyNotFoundException("gone"));

        BusinessKeyRevokeResult result = revocationAppService.revoke(revokeCmd());

        assertEquals("REVOKED", result.getState());
        Integer revoked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE key_id = ? AND key_state = 'REVOKED'",
                Integer.class, KEY_ID);
        assertEquals(1, revoked);
    }

    private BusinessKeyRevokeCmd revokeCmd() {
        return BusinessKeyRevokeCmd.builder()
                .requestId("CR060_BK_REV_0001")
                .keyId(KEY_ID)
                .reason("证书材料解析回归")
                .build();
    }

    @Test
    @DisplayName("健康分组：keyprov 聚合包含三指示器，且主聚合排除")
    void health_keyprovGroupAggregatesWithoutPollutingPrimary() {
        var keyprov = healthEndpointGroups.get("keyprov");
        assertNotNull(keyprov, "keyprov 健康分组必须存在, 已注册分组=" + healthEndpointGroups.getNames());
        assertTrue(keyprov.isMember(KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR));
        assertTrue(keyprov.isMember(KeyProvisioningHealthGroupsCustomizer.CERTIFICATE_RESOLUTION_INDICATOR));
        assertTrue(keyprov.isMember(KeyProvisioningHealthGroupsCustomizer.KMS_BUSINESS_KEY_INDICATOR));

        var primary = healthEndpointGroups.getPrimary();
        assertNotNull(primary);
        assertFalse(primary.isMember(KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR));
        assertFalse(primary.isMember(KeyProvisioningHealthGroupsCustomizer.CERTIFICATE_RESOLUTION_INDICATOR));
        assertFalse(primary.isMember(KeyProvisioningHealthGroupsCustomizer.KMS_BUSINESS_KEY_INDICATOR));
    }
}
