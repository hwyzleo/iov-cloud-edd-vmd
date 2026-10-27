package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.TboxEcdhCertificateEligibilityGate;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.ImportProofRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofAlgorithm;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofRejectReason;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.VerifiedImportProof;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
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

import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-062 集成测试（真实 dev MySQL + {@code @MockBean KeyProvisioningTemplate/ImportProofVerifier}）。
 * <p>
 * 覆盖：Request envelope v1 能力协商与 release/hash 门禁；ACK CR-008 字段门禁与原 Response 全字段绑定；
 * 逐用途经 FW-SEC ImportProofVerifier 验签（两用途各一次）；验签拒绝不激活；全通过才原子 ACTIVE + 单次事件。
 * 真实 ECDSA/P1363 密码学由 framework FW-SEC-DSN-CR-016 单测/E2E 覆盖；本测试聚焦 VMD 编排与门禁。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
@Rollback
class BusinessKeyCr062IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";
    private static final String HSM_UID = "CR062-UID-000001";
    private static final String DEVICE_SN = "CR062-TBOX-0001";
    private static final String PART_CODE = "CR062_TBOX";
    private static final String CERT_SN = "CR062-CERT-0001";
    private static final String RELEASE = "test-release-v1";
    private static final String DESCRIPTOR_SET_HASH = "test-descriptor-set-hash";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

    /**
     * CR-063：新 ECDH-ES Request 证书前置门禁——本测试聚焦 CR-062 ACK/验签编排，
     * 门禁自身逻辑由 TboxEcdhCertificateEligibilityGateTest 单测覆盖；此处 mock 为可放行。
     */
    @MockBean
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;
    @MockBean
    private ImportProofVerifier importProofVerifier;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger deriveCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        clean();
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = 'CR062_REQ_001'");
        insertFixture();

        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            deriveCount.incrementAndGet();
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey("CR062-KEY-" + purpose.name(), purpose,
                    context.businessKeyVersion(), 7L, "v1", operationId);
        });
        when(tboxEcdhCertificateEligibilityGate.evaluate(any()))
                .thenReturn(new TboxEcdhCertificateEligibilityGate.GateResult(true, false, "ok"));
        when(importProofVerifier.verify(any())).thenAnswer(inv -> {
            net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofVerificationRequest req =
                    inv.getArgument(0);
            return new VerifiedImportProof(ImportProofAlgorithm.ECDSA_P256_SHA256_P1363,
                    req.expectedCertificateDigest(), req.purpose(), new byte[32], java.time.Instant.now());
        });
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
                "CR062_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
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

    private KeyProvisioningRequest request(String requestId, boolean declareFormat) {
        KeyProvisioningRequest.Builder builder = KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .setTimestampMs(System.currentTimeMillis());
        if (declareFormat) {
            builder.addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1);
        }
        return builder.build();
    }

    private KeyProvisioningOperationResult requestOperation(String requestId, boolean declareFormat,
                                                            String release, String hash) {
        byte[] payload = request(requestId, declareFormat).toByteArray();
        return keyProvisioningApplicationFacade.request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, sha256(payload), release, hash);
    }

    private KeyProvisioningResponse requestResponse(String requestId) {
        KeyProvisioningOperationResult result = requestOperation(requestId, true, RELEASE, DESCRIPTOR_SET_HASH);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "request 应成功");
        try {
            return KeyProvisioningResponse.parseFrom(result.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyProvisioningAck ack(String requestId, KeyProvisioningResponse response, boolean legacyAlgorithm,
                                   boolean tamperDigest) {
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            ByteString digest = item.getEnvelopeDigest();
            if (tamperDigest) {
                byte[] tampered = digest.toByteArray();
                tampered[0] ^= 0x01;
                digest = ByteString.copyFrom(tampered);
            }
            builder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(KeyImportResult.KEY_IMPORT_RESULT_IMPORTED)
                    .setImportProof(ByteString.copyFrom(new byte[64]))
                    .setProofAlgorithm(legacyAlgorithm ? "NONE" : "")
                    .setImportProofAlgorithm(net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                            .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363)
                    .setEnvelopeDigest(digest)
                    .setKeyCheckValue(item.getKeyCheckValue())
                    .setEnvelopeFormat(item.getEnvelopeFormat())
                    .setRecipientCertificateDigest(ByteString.copyFrom(KeyprovEnvelopeTestFixtures.RECIPIENT_CERT_DIGEST))
                    .build());
        }
        return builder.build();
    }

    private KeyProvisioningOperationResult ackOperation(String requestId, KeyProvisioningAck ack) {
        byte[] payload = ack.toByteArray();
        return keyProvisioningApplicationFacade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", payload, sha256(payload));
    }

    private int countActive() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = 'ACTIVE'",
                Integer.class, DEVICE_SN, DOMAIN);
    }

    @Test
    @DisplayName("Request 未声明 envelope format → 806109，不下发 v1 且不调用 framework")
    void requestWithoutEnvelopeFormat_rejected() {
        KeyProvisioningOperationResult result = requestOperation("CR062_REQ_001", false, RELEASE, DESCRIPTOR_SET_HASH);
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_ENVELOPE_UNSUPPORTED, result.getErrorCode());
        assertFalse(result.isRetryable());
        verify(keyProvisioningTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("release/DescriptorSet hash 缺失或不匹配 → 806094，fail-closed 不派生")
    void releaseHashMismatch_rejected() {
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID,
                requestOperation("CR062_REQ_001", true, RELEASE, "wrong-hash").getErrorCode());
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID,
                requestOperation("CR062_REQ_001", true, null, null).getErrorCode());
        verify(keyProvisioningTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("ACK envelopeDigest 与原 Response 不匹配 → 整批拒绝，不激活且不调用验签")
    void ackDigestMismatch_rejected() {
        KeyProvisioningResponse response = requestResponse("CR062_REQ_001");
        KeyProvisioningOperationResult result = ackOperation("CR062_REQ_001",
                ack("CR062_REQ_001", response, false, true));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        verify(importProofVerifier, never()).verify(any());
        assertEquals(0, countActive(), "绑定不匹配不得激活任一用途");
    }

    @Test
    @DisplayName("ACK 遗留 proof_algorithm 非空 → 806111 确定性拒绝，不激活")
    void ackLegacyAlgorithm_rejected() {
        KeyProvisioningResponse response = requestResponse("CR062_REQ_001");
        KeyProvisioningOperationResult result = ackOperation("CR062_REQ_001",
                ack("CR062_REQ_001", response, true, false));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        verify(importProofVerifier, never()).verify(any());
        assertEquals(0, countActive());
    }

    @Test
    @DisplayName("验签拒绝 → 806111，整批不激活")
    void importProofRejected_notActivated() {
        KeyProvisioningResponse response = requestResponse("CR062_REQ_001");
        doThrow(new ImportProofRejectedException(ImportProofRejectReason.SIGNATURE_INVALID, "bad proof"))
                .when(importProofVerifier).verify(any());

        KeyProvisioningOperationResult result = ackOperation("CR062_REQ_001",
                ack("CR062_REQ_001", response, false, false));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_IMPORT_PROOF_REJECTED, result.getErrorCode());
        assertEquals(0, countActive(), "验签失败不得激活");
    }

    @Test
    @DisplayName("合法 ACK → 两用途各验签一次 + 原子 ACTIVE + 单次事件")
    void validAck_verifiesBothAndActivates() {
        KeyProvisioningResponse response = requestResponse("CR062_REQ_001");
        KeyProvisioningOperationResult result = ackOperation("CR062_REQ_001",
                ack("CR062_REQ_001", response, false, false));

        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome());
        verify(importProofVerifier, times(2)).verify(any());
        assertEquals(2, countActive(), "两用途应原子激活");

        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                Integer.class, "CR062_REQ_001");
        assertEquals(1, events, "完整成功应恰发一次完成事件");
    }
}
