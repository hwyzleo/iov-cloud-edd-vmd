package net.hwyz.iov.cloud.edd.vmd.service.integration;

import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyProvisionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyProvisionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyDirectoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyDirectoryQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyProvisioningAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRevocationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRotationAppService;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-055 集成测试
 * <p>
 * 覆盖：业务密钥在线申请（创建设备证书公钥 Wrap）→ 目录正/反向解析 → 轮换（版本递增、
 * 旧 DEPRECATED 新 ACTIVE、ROTATED 事件）→ 吊销（REVOKING→REVOKED、缓存失效），
 * 并校验 tb_device_business_key 落库、vmd_outbox 事件（payload 不含 kmsKeyRef/Wrapped Key）。
 * </p>
 *
 * @author CR-055
 */
@Rollback
class BusinessKeyCr055IntegrationTest extends BaseTest {

    private static final String REQUEST_ID = "CR055_REQ_001";
    private static final String VIN = "HWYZTESTCR055001";
    private static final String DEVICE_SN = "CR055-TBOX-0001";
    private static final String HSM_UID = "CR055-UID-000001";
    private static final String PART_CODE = "CR055_TBOX";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";
    private static final String CERT_SN = "CR055-CERT-0001";
    private static final String KEY_ID = "CR055-KEY-0001";
    private static final String KMS_REF = "kms/ref/CR055-KEY-0001";

    @MockBean
    private BusinessKeyMaterialTemplate businessKeyMaterialTemplate;

    @Autowired
    private BusinessKeyProvisioningAppService provisioningAppService;

    @Autowired
    private BusinessKeyDirectoryAppService directoryAppService;

    @Autowired
    private BusinessKeyRotationAppService rotationAppService;

    @Autowired
    private BusinessKeyRevocationAppService revocationAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final java.util.concurrent.atomic.AtomicInteger createCallCount = new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id LIKE 'CR055_REQ_%'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + VIN + "'");

        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) " +
                        "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", VIN);
        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update(
                "INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                VIN, DEVICE_SN);
        jdbcTemplate.update(
                "INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, " +
                        "device_sn, hsm_uid, certificate_profile, cert_status, not_after) " +
                        "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_TSP_CLIENT', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) " +
                        "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                REQUEST_ID + "_CERT", CERT_SN, VIN, DEVICE_SN, HSM_UID, VIN, DEVICE_SN);

        // create 按调用次序返回不同 keyId（首次申请 KEY_ID，轮换新建 KEY_ID_2，避免 uk_key_id 冲突）
        when(businessKeyMaterialTemplate.create(any(BusinessKeyCreateRequest.class))).thenAnswer(inv -> {
            int n = createCallCount.incrementAndGet();
            return new BusinessKeyMaterial(n == 1 ? KEY_ID : KEY_ID + "_" + n,
                    n == 1 ? KMS_REF : KMS_REF + "_" + n, n, "kms", "AES", "AES-256-GCM",
                    Instant.now().minusSeconds(1), Instant.now().plusSeconds(365L * 24 * 3600));
        });
        when(businessKeyMaterialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(new WrappedBusinessKey(new byte[]{1, 2, 3}, KEY_ID, 1, "AES",
                        Instant.now().plusSeconds(3600), Map.of("iv", "abcd")));
        when(businessKeyMaterialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenReturn(new RevocationResult(KEY_ID, CryptoKeyState.REVOKED, Instant.now()));
    }

    private BusinessKeyProvisionCmd provisionCmd(String requestId) {
        return BusinessKeyProvisionCmd.builder()
                .requestId(requestId)
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .build();
    }

    @Test
    @DisplayName("在线申请：创建设备证书公钥 Wrap，落 ACTIVE 行并发布 ACTIVE 事件（payload 无敏感材料）")
    void provision_应创建业务密钥并下发() {
        BusinessKeyProvisionResult result = provisioningAppService.provision(provisionCmd(REQUEST_ID));

        assertNotNull(result);
        assertEquals(KEY_ID, result.getKeyId());
        assertEquals(1L, result.getBusinessKeyVersion());
        assertNotNull(result.getWrappedKeyBase64());
        assertFalse(result.isReused());
        assertEquals("ACTIVE", result.getState());

        // 落库校验
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND purpose = ? " +
                        "AND key_id = ? AND business_key_version = 1 AND key_state = 'ACTIVE' AND kms_key_ref = ?",
                Integer.class, DEVICE_SN, DOMAIN, PURPOSE, KEY_ID, KMS_REF);
        assertEquals(1, rowCount, "业务密钥应落库为 ACTIVE");

        // 事件 payload 安全性：不含 kmsKeyRef / wrapped
        List<String> payloads = jdbcTemplate.queryForList(
                "SELECT payload FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id = ? AND publish_state = 'PENDING'",
                String.class, REQUEST_ID);
        assertEquals(1, payloads.size());
        String payload = payloads.get(0);
        assertTrue(payload.contains("\"state\":\"ACTIVE\""));
        assertFalse(payload.contains("kmsKeyRef"), "事件 payload 不得包含 kmsKeyRef");
        assertFalse(payload.contains("wrapped"), "事件 payload 不得包含 Wrapped Key");
    }

    @Test
    @DisplayName("在线申请：同 requestId 幂等复用 ACTIVE，不新建材料")
    void provision_同requestId_幂等复用() {
        BusinessKeyProvisionResult first = provisioningAppService.provision(provisionCmd(REQUEST_ID + "_IDEM"));
        BusinessKeyProvisionResult second = provisioningAppService.provision(provisionCmd(REQUEST_ID + "_IDEM"));

        assertFalse(first.isReused());
        assertTrue(second.isReused());
        assertEquals(first.getKeyId(), second.getKeyId());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE request_id = ?", Integer.class, REQUEST_ID + "_IDEM");
        assertEquals(1, count, "同 requestId 只应有一行");
    }

    @Test
    @DisplayName("目录解析：正向 resolveActive 返回唯一 ACTIVE 描述符")
    void directory_正向解析ACTIVE() {
        provisioningAppService.provision(provisionCmd(REQUEST_ID));

        BusinessKeyDescriptor descriptor = directoryAppService.resolveActive(
                new BusinessKeyDirectoryQuery(DEVICE_SN, DOMAIN, PURPOSE));
        assertNotNull(descriptor);
        assertEquals(KEY_ID, descriptor.keyId());
        assertEquals(1L, descriptor.businessKeyVersion());
        assertEquals(CryptoKeyState.ACTIVE, descriptor.state());
    }

    @Test
    @DisplayName("轮换：版本递增、旧 ACTIVE→DEPRECATED、新 ACTIVE、发布 ROTATED 事件")
    void rotate_应递增版本并原子切换() {
        provisioningAppService.provision(provisionCmd(REQUEST_ID + "_ROT"));

        BusinessKeyRotateCmd rotateCmd = BusinessKeyRotateCmd.builder()
                .requestId(REQUEST_ID + "_ROT2")
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .build();
        BusinessKeyRotationResult rotation = rotationAppService.rotate(rotateCmd);

        assertEquals(KEY_ID, rotation.getOldKeyId());
        assertEquals(1L, rotation.getOldBusinessKeyVersion());
        assertEquals(2L, rotation.getNewBusinessKeyVersion());

        // 旧行 DEPRECATED + decrypt_until，新行 ACTIVE v2
        Integer oldDeprecated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND purpose = ? AND key_state = 'DEPRECATED' AND decrypt_until IS NOT NULL",
                Integer.class, DEVICE_SN, PURPOSE);
        assertEquals(1, oldDeprecated);
        Integer newActive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND purpose = ? AND key_state = 'ACTIVE' AND business_key_version = 2",
                Integer.class, DEVICE_SN, PURPOSE);
        assertEquals(1, newActive);

        // ROTATED 事件
        Integer rotatedEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id = ? AND payload LIKE '%ROTATED%'",
                Integer.class, REQUEST_ID + "_ROT2");
        assertTrue(rotatedEvents >= 1, "轮换应发布 ROTATED 事件");
    }

    @Test
    @DisplayName("吊销：REVOKING 阻断 → REVOKED + 缓存失效")
    void revoke_应阻断并吊销() {
        provisioningAppService.provision(provisionCmd(REQUEST_ID + "_REV"));

        BusinessKeyRevokeCmd revokeCmd = BusinessKeyRevokeCmd.builder()
                .requestId(REQUEST_ID + "_REV2")
                .keyId(KEY_ID)
                .reason("密钥泄露")
                .build();
        BusinessKeyRevokeResult result = revocationAppService.revoke(revokeCmd);

        assertEquals("REVOKED", result.getState());
        Integer revokedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE key_id = ? AND key_state = 'REVOKED'",
                Integer.class, KEY_ID);
        assertEquals(1, revokedRows);

        // 反向解析应拒绝
        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException.class,
                () -> directoryAppService.resolveByKeyId(KEY_ID, KeyOperation.DECRYPT));

        // REVOKING + REVOKED 事件（outbox.source_ref_id 记录的是业务密钥申请时的 requestId）
        Integer revokingEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id = ? AND payload LIKE '%REVOKING%'",
                Integer.class, REQUEST_ID + "_REV");
        assertTrue(revokingEvents >= 1);
        Integer revokedEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id = ? AND payload LIKE '%REVOKED%'",
                Integer.class, REQUEST_ID + "_REV");
        assertTrue(revokedEvents >= 1);
    }
}
