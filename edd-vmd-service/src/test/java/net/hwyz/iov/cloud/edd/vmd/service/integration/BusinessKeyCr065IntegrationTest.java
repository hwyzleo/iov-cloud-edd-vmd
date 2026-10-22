package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OperatorContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.TerminateStaleProvisioningCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningBlockingDiagnosisResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningTerminationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyMaintenanceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyProvisioningMaintenanceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.TboxEcdhCertificateEligibilityGate;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateReconcileFirstException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsClient;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsOperationResult;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsOperationState;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofAlgorithm;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.VerifiedImportProof;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-065 集成测试（§9.3 MySQL 闭环 + 补充回归）。
 * <p>
 * 场景：证书 F1 签发时创建 requestId=kp-… 批次滞留 PREPARING 并关联两个 PENDING 目录版本；
 * ACTIVE 证书切换到 F2 后，旧 requestId+F2 幂等冲突、新 requestId 被 uk_device_domain_open 阻断；
 * 运维预检 → 一键终止（批次 FAILED + 两个 PENDING 目录 FAILED + item/摘要保留 + 无成功 Outbox）→
 * 新 requestId+F2 重开 → deriveAndWrap → WAIT_DEVICE_ACK → ACK → COMPLETED，唯一约束/KCV/envelope/完成事件
 * 符合 CR-061/062/063。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Rollback
class BusinessKeyCr065IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String HSM_UID = "CR065-UID-000001";
    private static final String DEVICE_SN = "CR065-TBOX-0001";
    private static final String PART_CODE = "CR065_TBOX";
    private static final String CERT_SN_F1 = "CR065-CERT-F1";
    private static final String CERT_SN_F2 = "CR065-CERT-F2";
    private static final String STUCK_REQUEST_ID = "kp-8d0442793522daeeed74dcef5a9c80ce";
    private static final String NEW_REQUEST_ID = "kp-cr065-reopen-0001";
    private static final String CALLER = "VAGW";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;
    @MockBean
    private ImportProofVerifier importProofVerifier;
    @MockBean
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;
    @MockBean
    private KmsClient kmsClient;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;
    @Autowired
    private BusinessKeyProvisioningMaintenanceAppService maintenanceAppService;
    @Autowired
    private BusinessKeyProvisioningRepository provisioningRepository;
    @Autowired
    private BusinessKeyMaintenanceAppService businessKeyMaintenanceAppService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning_item WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "')");
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id LIKE 'CR065_%'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + vin + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_security_constant WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + vin + "'");

        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) "
                + "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", vin);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update("INSERT INTO tb_part_security_constant (part_code, sn, chip_uid, constant_type, preset_state) "
                + "VALUES (?, ?, ?, 'ROOT', 'PRESET')", PART_CODE, DEVICE_SN, HSM_UID);
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                vin, DEVICE_SN);
        insertCertificate(CERT_SN_F1);

        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures
                    .derivedWrappedKey("CR065-KEY-" + purpose.name(), purpose, context.businessKeyVersion(),
                            1L, "v1", operationId);
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

    private void insertCertificate(String certSn) {
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_DEVICE_ECDH_ES_P256_V1', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                "CR065_CERT_REQ_" + certSn, certSn, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
    }

    /**
     * 证书 F1 → F2 切换（同一 device_sn+profile 仅一条 ACTIVE：F1 置 SUPERSEDED 后 F2 置 ACTIVE）。
     */
    private void switchToF2() {
        jdbcTemplate.update("UPDATE tb_veh_certificate SET cert_status = 'SUPERSEDED' "
                + "WHERE device_sn = ? AND cert_status = 'ACTIVE'", DEVICE_SN);
        insertCertificate(CERT_SN_F2);
    }

    /**
     * 直接构造“证书 F1 时代滞留 PREPARING 的批次 + 两个 PENDING 目录版本 + 两个 item”：
     * operation 未开始（operation_id 为 null，framework 调用前失败，VMD-DSN-CR-065 §4.2 情形1）。
     */
    private void insertStuckPreparingBatch() {
        String hsmDigest = sha256(HSM_UID.getBytes(StandardCharsets.UTF_8));
        String certDigestF1 = sha256(CERT_SN_F1.getBytes(StandardCharsets.UTF_8));
        LocalDateTime stale = LocalDateTime.now().minusHours(2);
        jdbcTemplate.update("INSERT INTO tb_business_key_provisioning "
                        + "(request_id, request_digest, ack_digest, device_sn, binding_id, part_id, hsm_uid_digest, "
                        + " certificate_sn_digest, business_domain, status, fail_reason, started_at, modify_time) "
                        + "SELECT ?, 'digest-under-F1', NULL, ?, vp.id, pi.id, ?, ?, ?, 'PREPARING', "
                        + " 'CERTIFICATE_CHANGED_DIGEST_CONFLICT|op=cr065', ?, ? "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                STUCK_REQUEST_ID, DEVICE_SN, hsmDigest, certDigestF1, DOMAIN, stale, stale, vin, DEVICE_SN);
        Long provisioningId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_business_key_provisioning WHERE request_id = ?", Long.class, STUCK_REQUEST_ID);

        Long bindingId = jdbcTemplate.queryForObject(
                "SELECT binding_id FROM tb_business_key_provisioning WHERE id = ?", Long.class, provisioningId);
        Long partId = jdbcTemplate.queryForObject(
                "SELECT part_id FROM tb_business_key_provisioning WHERE id = ?", Long.class, provisioningId);
        String[] purposes = {"UPLINK_AEAD", "DOWNLINK_AEAD"};
        String[] staleKeys = {"CR065-STALE-UP", "CR065-STALE-DL"};
        for (int i = 0; i < purposes.length; i++) {
            jdbcTemplate.update("INSERT INTO tb_device_business_key "
                            + "(device_sn, binding_id, part_id, hsm_uid, business_domain, purpose, material_model, key_id, "
                            + " business_key_version, key_state, wrap_mode, request_id, request_digest, policy_version) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 'DERIVED_ROOT', ?, 1, 'PENDING', 'DEVICE_CERT_PUBLIC_KEY', ?, 'digest-under-F1', 'v1')",
                    DEVICE_SN, bindingId, partId, HSM_UID, DOMAIN, purposes[i], staleKeys[i], STUCK_REQUEST_ID);
        }
        List<Long> dirIds = jdbcTemplate.queryForList(
                "SELECT id FROM tb_device_business_key WHERE request_id = ? ORDER BY purpose", Long.class, STUCK_REQUEST_ID);
        assertEquals(2, dirIds.size());
        for (int i = 0; i < purposes.length; i++) {
            jdbcTemplate.update("INSERT INTO tb_business_key_provisioning_item "
                            + "(provisioning_id, purpose, key_id, business_key_version, directory_key_id, device_result) "
                            + "VALUES (?, ?, ?, 1, ?, 'PENDING')",
                    provisioningId, purposes[i], staleKeys[i], dirIds.get(i));
        }
    }

    private KeyprovAuthenticatedContext ctx() {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", HSM_UID, "client-1",
                CERT_SN_F2, null, 1, "session-1", System.currentTimeMillis());
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

    private KeyProvisioningRequest request(String requestId) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .setTimestampMs(System.currentTimeMillis())
                .build();
    }

    private KeyProvisioningAck ack(String requestId, KeyProvisioningResponse response) {
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            builder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(KeyImportResult.KEY_IMPORT_RESULT_IMPORTED)
                    .setImportProof(ByteString.copyFrom(new byte[64]))
                    .setProofAlgorithm("")
                    .setImportProofAlgorithm(
                            net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                                    .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363)
                    .setEnvelopeDigest(item.getEnvelopeDigest())
                    .setKeyCheckValue(item.getKeyCheckValue())
                    .setEnvelopeFormat(item.getEnvelopeFormat())
                    .setRecipientCertificateDigest(ByteString.copyFrom(
                            net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures.RECIPIENT_CERT_DIGEST))
                    .build());
        }
        return builder.build();
    }

    private KeyProvisioningOperationResultHolder requestOp(String requestId) {
        byte[] payload = request(requestId).toByteArray();
        return new KeyProvisioningOperationResultHolder(keyProvisioningApplicationFacade.request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, sha256(payload),
                "test-release-v1", "test-descriptor-set-hash"));
    }

    private KeyProvisioningOperationResultHolder ackOp(String requestId, KeyProvisioningAck ack) {
        byte[] payload = ack.toByteArray();
        return new KeyProvisioningOperationResultHolder(keyProvisioningApplicationFacade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", payload, sha256(payload)));
    }

    /**
     * 结果 holder：outcome + errorCode + downlink payload（简化断言）。
     */
    private record KeyProvisioningOperationResultHolder(
            VmdKeyProvisioningOutcome outcome,
            String errorCode,
            byte[] downlink) {
        KeyProvisioningOperationResultHolder(net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult r) {
            this(r.getOutcome(), r.getErrorCode() != null ? r.getErrorCode().getCode() : null, r.getDownlinkPayload());
        }
    }

    private KeyProvisioningResponse requestResponse(String requestId) {
        KeyProvisioningOperationResultHolder result = requestOp(requestId);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.outcome(), "request 应成功: " + result.errorCode());
        try {
            return KeyProvisioningResponse.parseFrom(result.downlink());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TerminateStaleProvisioningCommand terminateCmd(String requestId, BusinessKeyProvisioningState expectedState) {
        // 与领域读取同一 MyBatis 路径取 modifyTime（TIMESTAMP 以 UTC 语义经 MyBatis 返回，避免 JdbcTemplate 时区换算不一致）
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        LocalDateTime modifyTime = batch != null ? batch.getModifyTime() : null;
        return TerminateStaleProvisioningCommand.builder()
                .maintenanceRequestId("op-cr065-001")
                .requestId(requestId)
                .expectedState(expectedState)
                .expectedModifyTime(modifyTime)
                .reasonCode(TerminationReasonCode.CERTIFICATE_CHANGED_DIGEST_CONFLICT)
                .reason("证书F1升级F2，旧批次摘要不可继续")
                .ticketNo("T-CR065-001")
                .build();
    }

    private OperatorContext operator() {
        return OperatorContext.builder().operatorId("1001").operatorName("sec-ops").build();
    }

    private int count(String sql, Object... args) {
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return c != null ? c : 0;
    }

    // ================= §9.3 MySQL 闭环 =================

    @Test
    @DisplayName("F1→F2 阻塞闭环：终止旧批次 → 新 requestId+F2 重开 → ACK → COMPLETED")
    void stuckByCertUpgrade_terminateThenReopen_completes() {
        insertStuckPreparingBatch();

        // 1) ACTIVE 证书切换到 F2
        switchToF2();

        // 2) 旧 requestId + F2 → 幂等冲突（请求摘要变化），批次不被推进
        KeyProvisioningOperationResultHolder oldReq = requestOp(STUCK_REQUEST_ID);
        assertEquals("806095", oldReq.errorCode(), "旧 requestId+F2 应返回幂等冲突");
        assertEquals("PREPARING", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, STUCK_REQUEST_ID),
                "旧批次不得被推进");

        // 3) 新 requestId 被 uk_device_domain_open 阻断（存在未终结批次）
        KeyProvisioningOperationResultHolder newReqBlocked = requestOp(NEW_REQUEST_ID);
        assertNotEquals(VmdKeyProvisioningOutcome.SUCCESS, newReqBlocked.outcome(), "新 requestId 应被唯一约束阻断");
        assertEquals("806094", newReqBlocked.errorCode(), "新 requestId 应返回状态不允许");

        // 4) 预检诊断：可清理
        ProvisioningBlockingDiagnosisResult diag = maintenanceAppService.inspectTermination(STUCK_REQUEST_ID);
        assertTrue(diag.getClearable(), "预检应判定可清理: " + diag.getBlockingReasons());
        assertTrue(Boolean.TRUE.equals(diag.getCertificateChanged()), "证书应判定为已升级");
        assertEquals("CLEARABLE", diag.getSuggestedAction());

        // 5) 一键终止
        ProvisioningTerminationResult terminated = maintenanceAppService.terminateStale(
                terminateCmd(STUCK_REQUEST_ID, BusinessKeyProvisioningState.PREPARING), operator());
        assertTrue(terminated.isChanged());
        assertTrue(terminated.isBlockingCleared());
        assertTrue(terminated.isReopenAllowed());
        assertEquals(2, terminated.getFailedPendingKeyCount());

        // 6) 断言：批次 FAILED、两 PENDING 目录 FAILED、item/摘要保留、无成功 Outbox
        assertEquals("FAILED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, STUCK_REQUEST_ID));
        assertEquals(2, count("SELECT COUNT(*) FROM tb_device_business_key WHERE request_id = ? AND key_state = 'FAILED'",
                STUCK_REQUEST_ID));
        assertEquals(0, count("SELECT COUNT(*) FROM tb_device_business_key WHERE request_id = ? AND key_state = 'PENDING'",
                STUCK_REQUEST_ID));
        assertEquals(2, count("SELECT COUNT(*) FROM tb_business_key_provisioning_item i "
                + "JOIN tb_business_key_provisioning p ON p.id = i.provisioning_id WHERE p.request_id = ?", STUCK_REQUEST_ID),
                "item 应保留（不删除证据）");
        // 摘要证据保留
        Integer digests = count("SELECT COUNT(*) FROM tb_device_business_key "
                + "WHERE request_id = ? AND request_digest = 'digest-under-F1'", STUCK_REQUEST_ID);
        assertEquals(2, digests, "request_digest 证据应保留不清空");
        assertEquals(0, count("SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' "
                + "AND source_ref_id = ?", STUCK_REQUEST_ID), "终止不得产生成功事件");
        // 唯一约束已释放
        assertEquals(0, count("SELECT COUNT(*) FROM tb_business_key_provisioning "
                + "WHERE device_sn = ? AND business_domain = ? AND open_flag IS NOT NULL", DEVICE_SN, DOMAIN),
                "终止后不应存在未终结批次");

        // 7) 新 requestId + F2 重开 → 新批次 → WAIT_DEVICE_ACK → ACK → COMPLETED
        KeyProvisioningOperationResultHolder reopen = requestOp(NEW_REQUEST_ID);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, reopen.outcome(), "新 requestId 应可创建新批次");
        KeyProvisioningResponse response = requestResponse(NEW_REQUEST_ID);
        assertEquals(2, response.getItemsCount(), "V2C 应下发两个用途");
        assertEquals("WAIT_DEVICE_ACK", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, NEW_REQUEST_ID));

        KeyProvisioningAckResult ackResult = ackParse(ackOp(NEW_REQUEST_ID, ack(NEW_REQUEST_ID, response)));
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, ackResult.getResult());

        assertEquals("COMPLETED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, NEW_REQUEST_ID));
        assertEquals(2, count("SELECT COUNT(*) FROM tb_device_business_key "
                + "WHERE device_sn = ? AND business_domain = ? AND key_state = 'ACTIVE'", DEVICE_SN, DOMAIN));
        assertEquals(1, count("SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' "
                + "AND source_ref_id = ?", NEW_REQUEST_ID));
        // 新批次证书摘要绑定 F2（CR-063 一致性）
        String newCertDigest = jdbcTemplate.queryForObject(
                "SELECT certificate_sn_digest FROM tb_business_key_provisioning WHERE request_id = ?",
                String.class, NEW_REQUEST_ID);
        assertEquals(sha256(CERT_SN_F2.getBytes(StandardCharsets.UTF_8)), newCertDigest,
                "新批次应冻结当前 ACTIVE 证书 F2 摘要");
    }

    private KeyProvisioningAckResult ackParse(KeyProvisioningOperationResultHolder holder) {
        try {
            return KeyProvisioningAckResult.parseFrom(holder.downlink());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ================= 补充回归（§9.3 末段） =================

    @Test
    @DisplayName("RECONCILE_REQUIRED 且 operation 结果未知 → 终止拒绝（RECONCILE_FIRST）")
    void reconcileUnknown_reconcileFirst() {
        insertStuckPreparingBatch();
        // 改为 RECONCILE_REQUIRED + 单个 item 带未知 operation（uk_provisioning_item_operation_id 唯一）
        jdbcTemplate.update("UPDATE tb_business_key_provisioning SET status = 'RECONCILE_REQUIRED' "
                + "WHERE request_id = ?", STUCK_REQUEST_ID);
        jdbcTemplate.update("UPDATE tb_business_key_provisioning_item SET operation_id = 'op-cr065-unknown' "
                + "WHERE purpose = 'UPLINK_AEAD' "
                + "AND provisioning_id = (SELECT id FROM tb_business_key_provisioning WHERE request_id = ?)",
                STUCK_REQUEST_ID);
        when(kmsClient.getOperation("op-cr065-unknown"))
                .thenReturn(new KmsOperationResult("op-cr065-unknown", KmsOperationState.UNKNOWN, null, "unknown"));

        ProvisioningBlockingDiagnosisResult diag = maintenanceAppService.inspectTermination(STUCK_REQUEST_ID);
        assertFalse(diag.getClearable());
        assertEquals("RECONCILE_FIRST", diag.getSuggestedAction());

        assertThrows(BusinessKeyProvisioningTerminateReconcileFirstException.class,
                () -> maintenanceAppService.terminateStale(
                        terminateCmd(STUCK_REQUEST_ID, BusinessKeyProvisioningState.RECONCILE_REQUIRED), operator()));
        // 零数据变更
        assertEquals("RECONCILE_REQUIRED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, STUCK_REQUEST_ID));
    }

    @Test
    @DisplayName("WAIT_DEVICE_ACK → 终止拒绝，仅由 expireStaleAckBatches 治理转 EXPIRED")
    void waitDeviceAck_onlyExpiryTask() {
        insertStuckPreparingBatch();
        jdbcTemplate.update("UPDATE tb_business_key_provisioning SET status = 'WAIT_DEVICE_ACK' "
                + "WHERE request_id = ?", STUCK_REQUEST_ID);

        assertThrows(BusinessKeyProvisioningTerminateNotAllowedException.class,
                () -> maintenanceAppService.terminateStale(
                        terminateCmd(STUCK_REQUEST_ID, BusinessKeyProvisioningState.WAIT_DEVICE_ACK), operator()));

        int expired = businessKeyMaintenanceAppService.expireStaleAckBatches(
                LocalDateTime.now().minusSeconds(1), 100);
        assertEquals(1, expired);
        assertEquals("EXPIRED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, STUCK_REQUEST_ID));
    }

    @Test
    @DisplayName("终止后晚到 ACK → 拒绝且不激活目录、不发成功事件")
    void lateAckAfterTerminate_rejectedNoActivation() {
        insertStuckPreparingBatch();
        maintenanceAppService.terminateStale(
                terminateCmd(STUCK_REQUEST_ID, BusinessKeyProvisioningState.PREPARING), operator());

        // 构造一份对旧批次的 ACK（keyId 使用陈旧占位），应被拒绝
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(STUCK_REQUEST_ID);
        builder.addItems(KeyImportItemResult.newBuilder()
                .setPurpose(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .setKeyId("CR065-STALE-UP")
                .setBusinessKeyVersion(1L)
                .setResult(KeyImportResult.KEY_IMPORT_RESULT_IMPORTED)
                .setImportProof(ByteString.copyFrom(new byte[64]))
                .setImportProofAlgorithm(
                        net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                                .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363));
        KeyProvisioningOperationResultHolder late = ackOp(STUCK_REQUEST_ID, builder.build());
        assertNotEquals(VmdKeyProvisioningOutcome.SUCCESS, late.outcome(), "终止后晚到 ACK 应被拒绝");
        assertEquals(0, count("SELECT COUNT(*) FROM tb_device_business_key WHERE request_id = ? AND key_state = 'ACTIVE'",
                STUCK_REQUEST_ID), "晚到 ACK 不得激活目录");
        assertEquals(0, count("SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' "
                + "AND source_ref_id = ?", STUCK_REQUEST_ID), "晚到 ACK 不得发成功事件");
    }
}
