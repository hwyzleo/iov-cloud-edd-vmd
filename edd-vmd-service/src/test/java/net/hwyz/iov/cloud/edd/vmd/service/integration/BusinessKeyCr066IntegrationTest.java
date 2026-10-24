package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRotationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.TboxEcdhCertificateEligibilityGate;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRotationConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofAlgorithm;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.VerifiedImportProof;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-066 集成测试（真实 dev MySQL + {@code @MockBean KeyProvisioningTemplate}）。
 * <p>
 * 覆盖 logicalKeyId 全局唯一与多业务版本目录冲突修复的关键行为：
 * <ul>
 *   <li>撞 uk_key_id → 确定性 claim-or-reuse 收敛（既有 FAILED 同材料行恢复复用），不落 806100 死循环；</li>
 *   <li>同 keyId 但不可变元数据冲突 → 806130 不可重试；</li>
 *   <li>声明版本与 ACTIVE 不一致但代际相同 → 806131 版本代际冲突，不创建 v2；</li>
 *   <li>换证/换件 REIMPORT 复用 ACTIVE 行，目录行数不变；</li>
 *   <li>同 requestId 重放返回原 envelope bytes；</li>
 *   <li>轮换真实代际变化（rootV7→rootV8）新 keyId 新版本原子切换；同 keyId → 拒绝激活。</li>
 * </ul>
 * OpenBao Transit {@code v2c-comm-root} 真实端到端不可用（部署阻断项），故对
 * {@link KeyProvisioningTemplate} 采用 {@code @MockBean} 口径。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Rollback
class BusinessKeyCr066IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";

    private static final String HSM_UID = "CR066-UID-000001";
    private static final String DEVICE_SN = "CR066-TBOX-0001";
    private static final String PART_CODE = "CR066_TBOX";
    private static final String CERT_SN = "CR066-CERT-0001";

    private static final int ROOT_VERSION = 7;
    private static final String DERIVATION_VERSION = "v1";
    private static final String MATERIAL_MODEL = "DERIVED_ROOT";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @MockBean
    private ImportProofVerifier importProofVerifier;

    @MockBean
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;

    @Autowired
    private BusinessKeyRotationAppService businessKeyRotationAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger deriveCount = new AtomicInteger();

    /** 测试内可覆盖的 Provider 根版本（默认 7；轮换测试置 8 模拟真实代际变化） */
    private final AtomicInteger providerRootVersion = new AtomicInteger(ROOT_VERSION);

    @BeforeEach
    void setUp() {
        providerRootVersion.set(ROOT_VERSION);
        deriveCount.set(0);
        clean();
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id LIKE 'CR066_%'");

        insertFixture();

        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            deriveCount.incrementAndGet();
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            int root = providerRootVersion.get();
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                    keyId(purpose.name(), root), purpose, context.businessKeyVersion(), root, DERIVATION_VERSION, operationId);
        });
        when(importProofVerifier.verify(any())).thenAnswer(inv -> {
            net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofVerificationRequest req =
                    inv.getArgument(0);
            return new VerifiedImportProof(ImportProofAlgorithm.ECDSA_P256_SHA256_P1363,
                    req.expectedCertificateDigest(), req.purpose(), new byte[32], java.time.Instant.now());
        });
        when(tboxEcdhCertificateEligibilityGate.evaluate(any()))
                .thenReturn(new TboxEcdhCertificateEligibilityGate.GateResult(true, false, "ok"));
    }

    private String keyId(String purpose, int root) {
        return "CR066-KEY-" + purpose + "-r" + root;
    }

    private void clean() {
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning_item WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "')");
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + vin + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_security_constant WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + vin + "'");
    }

    private void insertFixture() {
        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) "
                + "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", vin);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update("INSERT INTO tb_part_security_constant (part_code, sn, chip_uid, constant_type, preset_state) "
                + "VALUES (?, ?, ?, 'ROOT', 'PRESET')", PART_CODE, DEVICE_SN, HSM_UID);
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                vin, DEVICE_SN);
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_DEVICE_ECDH_ES_P256_V1', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                "CR066_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
    }

    /**
     * 轮换路径 {@code resolveActive} 需要 TBOX_TSP_CLIENT 证书（仅轮换测试插入，不扰动批次流程）。
     */
    private void insertTspClientCert() {
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT 'CR066_CERT_REQ_TSP', 'CR066-CERT-TSP-0001', ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_TSP_CLIENT', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
    }

    /**
     * 直接落一条完整身份（不可变元数据）的目录行，模拟前一次失败/成功批次的同逻辑材料证据。
     */
    private void insertDirectoryRow(String purpose, Long version, String keyIdValue, int root, String state,
                                    String requestId) {
        jdbcTemplate.update("INSERT INTO tb_device_business_key "
                        + "(device_sn, binding_id, part_id, hsm_uid, business_domain, purpose, material_model, key_id, "
                        + "business_key_version, kms_key_ref, root_key_version, derivation_version, algorithm, key_spec, "
                        + "key_state, request_id, policy_version, row_version, row_valid) "
                        + "SELECT ?, vp.id, pi.id, ?, 'V2C_COMM', ?, 'DERIVED_ROOT', ?, ?, ?, ?, 'v1', 'AES', 'AES-256-GCM', "
                        + "?, ?, 'v1', 1, 1 FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id "
                        + "WHERE vp.vin = ? AND pi.sn = ?",
                DEVICE_SN, HSM_UID, purpose, keyIdValue, version, "kms/ref/" + keyIdValue, root,
                state, requestId, vin, DEVICE_SN);
    }

    private KeyprovAuthenticatedContext ctx() {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", HSM_UID, "client-1",
                CERT_SN, null, 1, "session-1", System.currentTimeMillis());
    }

    private String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyProvisioningRequest request(String requestId, long upVersion, long downVersion) {
        KeyProvisioningRequest.Builder builder = KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .setTimestampMs(System.currentTimeMillis());
        if (upVersion > 0 || downVersion > 0) {
            builder.addCurrentVersions(net.hwyz.iov.security.keyprov.v1.CurrentKeyVersion.newBuilder()
                            .setPurpose(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD).setBusinessKeyVersion(upVersion))
                    .addCurrentVersions(net.hwyz.iov.security.keyprov.v1.CurrentKeyVersion.newBuilder()
                            .setPurpose(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD).setBusinessKeyVersion(downVersion));
        }
        return builder.build();
    }

    private KeyProvisioningOperationResult requestOperation(String requestId, long upVersion, long downVersion) {
        byte[] payload = request(requestId, upVersion, downVersion).toByteArray();
        return keyProvisioningApplicationFacade.request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, sha256(payload),
                "test-release-v1", "test-descriptor-set-hash");
    }

    private KeyProvisioningResponse requestResponse(String requestId, long upVersion, long downVersion) {
        KeyProvisioningOperationResult result = requestOperation(requestId, upVersion, downVersion);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "request 应成功");
        return parse(result);
    }

    private KeyProvisioningResponse parse(KeyProvisioningOperationResult result) {
        try {
            return KeyProvisioningResponse.parseFrom(result.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int countKeys(String keyState) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = ?",
                Integer.class, DEVICE_SN, DOMAIN, keyState);
    }

    private Map<String, Object> row(String purpose) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND purpose = ? "
                        + "ORDER BY id DESC LIMIT 1",
                DEVICE_SN, DOMAIN, purpose);
    }

    // ================= CR-066 场景 =================

    @Test
    @DisplayName("撞 uk_key_id（既有 FAILED 同材料行）→ claim-or-reuse 恢复复用收敛 SUCCESS，不落 806100 死循环")
    void duplicateKey_withFailedExisting_converges() {
        // 前一次失败批次的同逻辑材料证据行（keyId 与 Provider 确定性结果一致）
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "FAILED", "CR066_PREV_REQ_UP");
        insertDirectoryRow("DOWNLINK_AEAD", 1L, keyId("DOWNLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "FAILED", "CR066_PREV_REQ_DN");
        Long failedUpId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_device_business_key WHERE device_sn = ? AND purpose = 'UPLINK_AEAD'", Long.class, DEVICE_SN);

        KeyProvisioningResponse response = requestResponse("CR066_REQ_001", 0, 0);

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertEquals(2, response.getItemsCount());
        assertEquals("CR066-KEY-UPLINK_AEAD-r7", response.getItemsList().get(0).getKeyId());

        // 既有 FAILED 行被恢复复用（recovery_count+1、指向本次 attempt），不新建 canonical 行
        Map<String, Object> up = jdbcTemplate.queryForMap(
                "SELECT key_state, recovery_count, last_recovery_request_id FROM tb_device_business_key WHERE id = ?",
                failedUpId);
        assertEquals("PENDING", up.get("key_state"), "既有 FAILED 行应恢复为 PENDING");
        assertEquals(1, ((Number) up.get("recovery_count")).intValue(), "应记录恢复次数");
        assertEquals("CR066_REQ_001", up.get("last_recovery_request_id"), "应记录最近恢复 attempt 审计引用");

        // 既有行（fixture 未写有效期，模拟历史/缺陷期间行）应被兜底补齐，保证 envelope 可下发
        Map<String, Object> validity = jdbcTemplate.queryForMap(
                "SELECT valid_from, valid_to FROM tb_device_business_key WHERE id = ?", failedUpId);
        assertNotNull(validity.get("valid_from"), "恢复复用的既有行应兜底补齐 valid_from");
        assertNotNull(validity.get("valid_to"), "恢复复用的既有行应兜底补齐 valid_to");

        // 本批次占位行 SUPERSEDED 且保留证据 + 指向 canonical
        Integer superseded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND key_state = 'SUPERSEDED' "
                        + "AND superseded_by_id IS NOT NULL AND key_id IS NULL",
                Integer.class, DEVICE_SN);
        assertEquals(2, superseded, "两用途占位行应 SUPERSEDED 并记录 superseded_by_id");

        // item 重挂 canonical 行
        Long rebound = jdbcTemplate.queryForObject(
                "SELECT directory_key_id FROM tb_business_key_provisioning_item "
                        + "WHERE provisioning_id IN (SELECT id FROM tb_business_key_provisioning WHERE request_id = ?) "
                        + "AND purpose = 'UPLINK_AEAD'", Long.class, "CR066_REQ_001");
        assertEquals(failedUpId, rebound, "item 应重挂既有 canonical 行");

        // 无 806100：响应成功且批次推进 WAIT_DEVICE_ACK
        assertEquals("WAIT_DEVICE_ACK", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, "CR066_REQ_001"));
        assertEquals(0, countKeys("FAILED"), "恢复后不应遗留 FAILED");
    }

    @Test
    @DisplayName("同 keyId 但 rootKeyVersion 不一致 → 806130 不可重试（DO_NOT_RETRY），批次 FAILED")
    void duplicateKey_identityConflict_806130() {
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION + 2, "ACTIVE", "CR066_PREV_REQ_UP");

        KeyProvisioningOperationResult result = requestOperation("CR066_REQ_002", 0, 0);

        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_LOGICAL_IDENTITY_CONFLICT, result.getErrorCode(), "应映射 806130");
        assertFalse(result.isRetryable());
        assertEquals("DO_NOT_RETRY", result.getAction());
        assertEquals("FAILED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, "CR066_REQ_002"),
                "不可重试冲突应终态化批次，阻断自动重试");
    }

    @Test
    @DisplayName("ACTIVE v1 + 声明 v2、代际相同 → 806131 版本代际冲突，不创建 v2")
    void declaredVersionMismatch_sameGeneration_806131() {
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_UP");
        insertDirectoryRow("DOWNLINK_AEAD", 1L, keyId("DOWNLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_DN");

        KeyProvisioningOperationResult result = requestOperation("CR066_REQ_003", 2, 2);

        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_VERSION_GENERATION_MISMATCH, result.getErrorCode(), "应映射 806131");
        assertFalse(result.isRetryable());
        verify(keyProvisioningTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ?", Integer.class, DEVICE_SN),
                "不得创建任何新版本行");
    }

    @Test
    @DisplayName("换证/换件 REIMPORT：同代际复用 ACTIVE 行，不新建目录行，目录行数不变")
    void reimport_reusesActive_noNewDirectoryRow() {
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_UP");
        insertDirectoryRow("DOWNLINK_AEAD", 1L, keyId("DOWNLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_DN");

        KeyProvisioningResponse response = requestResponse("CR066_REQ_004", 0, 0);

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertEquals(2, countKeys("ACTIVE"), "REIMPORT 不得新建目录行");
        assertEquals(0, countKeys("SUPERSEDED"), "REIMPORT 不产生 SUPERSEDED 占位行");
        assertEquals(2, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ?", Integer.class, DEVICE_SN),
                "换证/换件目录行数不变");
        // item 指向既有 ACTIVE 行
        Integer itemsToActive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_device_business_key k ON k.id = i.directory_key_id "
                        + "WHERE i.provisioning_id IN (SELECT id FROM tb_business_key_provisioning WHERE request_id = ?) "
                        + "AND k.key_state = 'ACTIVE'", Integer.class, "CR066_REQ_004");
        assertEquals(2, itemsToActive, "item 应全部指向既有 ACTIVE 行");
    }

    @Test
    @DisplayName("同 requestId 重放：返回原 envelope bytes（字节级幂等），目录行数不变")
    void replay_sameRequestId_returnsOriginalEnvelopeBytes() {
        KeyProvisioningResponse first = requestResponse("CR066_REQ_005", 0, 0);
        KeyProvisioningResponse second = requestResponse("CR066_REQ_005", 0, 0);

        assertEquals(2, first.getItemsCount());
        for (int i = 0; i < first.getItemsCount(); i++) {
            assertTrue(Arrays.equals(first.getItemsList().get(i).getEnvelopeDigest().toByteArray(),
                            second.getItemsList().get(i).getEnvelopeDigest().toByteArray()),
                    "同 requestId 重放 envelopeDigest 必须字节级一致");
            assertArrayEquals(first.getItemsList().get(i).getWrappedKey().toByteArray(),
                    second.getItemsList().get(i).getWrappedKey().toByteArray(),
                    "同 requestId 重放 Wrapped Key 必须字节级一致");
        }
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ?", Integer.class, DEVICE_SN),
                "重放不得新建目录行");
        // 多用途同 requestId：每 purpose 独立 keyId，uk_request_purpose 保持
        List<String> keyIds = jdbcTemplate.queryForList(
                "SELECT key_id FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? ORDER BY purpose",
                String.class, DEVICE_SN, DOMAIN);
        assertEquals(2, keyIds.stream().distinct().count(), "两用途 keyId 必须隔离（uk_key_id 全局唯一）");
    }

    @Test
    @DisplayName("轮换真实代际变化（rootV7→rootV8）：新 keyId 新版本，旧 DEPRECATED、新 ACTIVE 原子切换")
    void rotation_realGenerationChange_switchesAtomic() {
        insertTspClientCert();
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_UP");
        providerRootVersion.set(ROOT_VERSION + 1); // 根版本变化 → 真实代际变化

        BusinessKeyRotationResult result = businessKeyRotationAppService.rotate(BusinessKeyRotateCmd.builder()
                .requestId("CR066_ROT_001").deviceSn(DEVICE_SN).deviceCategory("TBOX")
                .businessDomain(DOMAIN).purpose("UPLINK_AEAD").operatorId("IT-TEST").build());

        assertEquals(keyId("UPLINK_AEAD", ROOT_VERSION + 1), result.getNewKeyId(), "新 keyId 对应新根版本");
        assertEquals(2L, result.getNewBusinessKeyVersion(), "应分配新业务版本");
        assertEquals(1, countKeys("DEPRECATED"), "旧 ACTIVE 应转 DEPRECATED（仅解密窗口）");
        Map<String, Object> newActive = row("UPLINK_AEAD");
        assertEquals("ACTIVE", newActive.get("key_state"));
        assertEquals(ROOT_VERSION + 1, ((Number) newActive.get("root_key_version")).intValue(), "新行应回填新根版本");
        assertEquals(2L, ((Number) newActive.get("business_key_version")).longValue());
    }

    @Test
    @DisplayName("轮换 Provider 返回与 ACTIVE 相同 keyId（未发生真实代际变化）→ 拒绝激活新行")
    void rotation_sameKeyId_rejected() {
        insertTspClientCert();
        insertDirectoryRow("UPLINK_AEAD", 1L, keyId("UPLINK_AEAD", ROOT_VERSION), ROOT_VERSION, "ACTIVE", "CR066_PREV_REQ_UP");

        assertThrows(BusinessKeyRotationConflictException.class,
                () -> businessKeyRotationAppService.rotate(BusinessKeyRotateCmd.builder()
                        .requestId("CR066_ROT_002").deviceSn(DEVICE_SN).deviceCategory("TBOX")
                        .businessDomain(DOMAIN).purpose("UPLINK_AEAD").operatorId("IT-TEST").build()));

        assertEquals(1, countKeys("ACTIVE"), "旧 ACTIVE 不得降级");
        Map<String, Object> newRow = row("UPLINK_AEAD");
        assertEquals("FAILED", newRow.get("key_state"), "新行应留痕 FAILED（未发生真实代际变化）");
    }
}
