package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.TboxEcdhCertificateEligibilityGate;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofAlgorithm;
import net.hwyz.iov.cloud.framework.security.crypto.model.importproof.VerifiedImportProof;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-068 集成测试（真实 dev MySQL + {@code @MockBean KeyProvisioningTemplate/ImportProofVerifier}）。
 * <p>
 * 覆盖：Request → 每用途 core/wire 双摘要落库（core 仅审计、wire 权威）+ deterministic parameters/
 * wrappedKey digest/profile；批次级 Response 发送前持久化；同 requestId 字节级重放（不重新 derive，
 * 覆盖发送前故障/发送后丢失/Adapter 升级）；ACK 使用 core digest / 其他 wire digest / 其他 KCV 均拒绝
 * 不激活；合法 ACK（wire digest）→ 两用途原子 ACTIVE + 单次事件。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Rollback
class BusinessKeyCr068IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";
    private static final String HSM_UID = "CR068-UID-000001";
    private static final String DEVICE_SN = "CR068-TBOX-0001";
    private static final String PART_CODE = "CR068_TBOX";
    private static final String CERT_SN = "CR068-CERT-0001";
    private static final String RELEASE = "test-release-v1";
    private static final String DESCRIPTOR_SET_HASH = "test-descriptor-set-hash";
    private static final String vin = "HWYZTEST068000001";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

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
        insertFixture();
        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            deriveCount.incrementAndGet();
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures.derivedWrappedKey(
                    "CR068-KEY-" + purpose.name(), purpose, context.businessKeyVersion(), 7L, "v1", operationId);
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
        // CR-068 集成测试：Outbox 表不随批次清理，历史运行会残留完成事件，需一并清除
        // （否则事件计数断言随运行次数累积失败；注意 tb_vmd_outbox 与批次表 collation 不一致，
        // 不能跨表子查询，按本测试固定 requestId 字面量删除）
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE source_ref_id = 'CR068_REQ_001'");
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
                "CR068_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
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

    private KeyProvisioningOperationResult requestOperation(String requestId) {
        byte[] payload = request(requestId).toByteArray();
        return keyProvisioningApplicationFacade.request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, sha256(payload), RELEASE, DESCRIPTOR_SET_HASH);
    }

    private KeyProvisioningResponse requestResponse(String requestId) {
        KeyProvisioningOperationResult result = requestOperation(requestId);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "request 应成功");
        try {
            return KeyProvisioningResponse.parseFrom(result.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyProvisioningAck ackWith(String requestId, KeyProvisioningResponse response,
                                       byte[] digestOverride, byte[] kcvOverride) {
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            builder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(KeyImportResult.KEY_IMPORT_RESULT_IMPORTED)
                    .setImportProof(ByteString.copyFrom(new byte[64]))
                    .setProofAlgorithm("")
                    .setImportProofAlgorithm(net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                            .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363)
                    .setEnvelopeDigest(digestOverride != null ? ByteString.copyFrom(digestOverride)
                            : item.getEnvelopeDigest())
                    .setKeyCheckValue(kcvOverride != null ? ByteString.copyFrom(kcvOverride)
                            : item.getKeyCheckValue())
                    .setEnvelopeFormat(item.getEnvelopeFormat())
                    .setRecipientCertificateDigest(ByteString.copyFrom(
                            net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures.RECIPIENT_CERT_DIGEST))
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
    @DisplayName("Request → 每用途 core/wire 双摘要 + parameters/wrappedKey digest + profile 落库 item（core 仅审计）")
    void requestPersistsDualDigests() {
        KeyProvisioningResponse response = requestResponse("CR068_REQ_001");
        assertEquals(2, response.getItemsCount());
        Integer items = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id WHERE b.request_id = ?",
                Integer.class, "CR068_REQ_001");
        assertEquals(2, items);
        for (String purpose : new String[]{"UPLINK_AEAD", "DOWNLINK_AEAD"}) {
            Integer wireLen = jdbcTemplate.queryForObject(
                    "SELECT LENGTH(wire_envelope_digest) FROM tb_business_key_provisioning_item i "
                            + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                            + "WHERE b.request_id = ? AND i.purpose = ?",
                    Integer.class, "CR068_REQ_001", purpose);
            Integer coreLen = jdbcTemplate.queryForObject(
                    "SELECT LENGTH(core_envelope_digest) FROM tb_business_key_provisioning_item i "
                            + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                            + "WHERE b.request_id = ? AND i.purpose = ?",
                    Integer.class, "CR068_REQ_001", purpose);
            Integer paramsLen = jdbcTemplate.queryForObject(
                    "SELECT LENGTH(parameters_digest) FROM tb_business_key_provisioning_item i "
                            + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                            + "WHERE b.request_id = ? AND i.purpose = ?",
                    Integer.class, "CR068_REQ_001", purpose);
            Integer wrappedLen = jdbcTemplate.queryForObject(
                    "SELECT LENGTH(wrapped_key_digest) FROM tb_business_key_provisioning_item i "
                            + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                            + "WHERE b.request_id = ? AND i.purpose = ?",
                    Integer.class, "CR068_REQ_001", purpose);
            assertEquals(32, wireLen, "wire digest BINARY(32)");
            assertEquals(32, coreLen, "core digest BINARY(32)");
            assertEquals(32, paramsLen, "parameters digest BINARY(32)");
            assertEquals(32, wrappedLen, "wrappedKey digest BINARY(32)");
        }
        // core 与 wire 必须不同（语义分离）
        byte[] uplinkCore = jdbcTemplate.queryForObject(
                "SELECT core_envelope_digest FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                        + "WHERE b.request_id = ? AND i.purpose = 'UPLINK_AEAD'", byte[].class, "CR068_REQ_001");
        byte[] uplinkWire = jdbcTemplate.queryForObject(
                "SELECT wire_envelope_digest FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                        + "WHERE b.request_id = ? AND i.purpose = 'UPLINK_AEAD'", byte[].class, "CR068_REQ_001");
        assertFalse(Arrays.equals(uplinkCore, uplinkWire), "core/wire 摘要必须语义分离");
    }

    @Test
    @DisplayName("Response 发送前持久化（批次表 response_payload/digest/codec）+ 同 requestId 重放返回原 bytes 且不重新 derive")
    void responsePersistedBeforeSendAndReplayedByteIdentical() {
        KeyProvisioningResponse first = requestResponse("CR068_REQ_001");
        byte[] firstBytes = first.toByteArray();
        int deriveAfterFirst = deriveCount.get();

        // 发送前已持久化（批次表受控保存最终响应事实）
        Integer payloadLen = jdbcTemplate.queryForObject(
                "SELECT LENGTH(response_payload) FROM tb_business_key_provisioning WHERE request_id = ?",
                Integer.class, "CR068_REQ_001");
        assertTrue(payloadLen > 0, "Response 必须在发送前持久化");
        String codec = jdbcTemplate.queryForObject(
                "SELECT response_codec_version FROM tb_business_key_provisioning WHERE request_id = ?",
                String.class, "CR068_REQ_001");
        assertEquals("PAR_PROTO_KEYPROV_AAD_V1", codec);

        // 发送后响应丢失 → 同 requestId 重发 Request → 原 bytes（字节级重放，不重新 derive/map）
        KeyProvisioningResponse replay = requestResponse("CR068_REQ_001");
        assertArrayEquals(firstBytes, replay.toByteArray(), "同 requestId 重放必须返回完全相同 bytes");
        assertEquals(deriveAfterFirst, deriveCount.get(), "重放不得重新 derive（Adapter 升级不改变存量响应）");
    }

    @Test
    @DisplayName("ACK 使用 core digest → 整批拒绝，不激活且不调用验签（core 不进 ACK）")
    void ackWithCoreDigest_rejected() {
        KeyProvisioningResponse response = requestResponse("CR068_REQ_001");
        byte[] uplinkCore = jdbcTemplate.queryForObject(
                "SELECT core_envelope_digest FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                        + "WHERE b.request_id = ? AND i.purpose = 'UPLINK_AEAD'", byte[].class, "CR068_REQ_001");
        KeyProvisioningOperationResult result = ackOperation("CR068_REQ_001",
                ackWith("CR068_REQ_001", response, uplinkCore, null));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        verify(importProofVerifier, never()).verify(any());
        assertEquals(0, countActive(), "core digest ACK 不得激活");
    }

    @Test
    @DisplayName("ACK 使用其他 wire digest → 整批拒绝，不激活")
    void ackWithOtherWireDigest_rejected() {
        KeyProvisioningResponse response = requestResponse("CR068_REQ_001");
        byte[] other = response.getItems(0).getEnvelopeDigest().toByteArray();
        other[0] ^= 0x01;
        KeyProvisioningOperationResult result = ackOperation("CR068_REQ_001",
                ackWith("CR068_REQ_001", response, other, null));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        verify(importProofVerifier, never()).verify(any());
        assertEquals(0, countActive());
    }

    @Test
    @DisplayName("ACK 使用其他 KCV → 整批拒绝，不激活")
    void ackWithOtherKcv_rejected() {
        KeyProvisioningResponse response = requestResponse("CR068_REQ_001");
        byte[] otherKcv = {1, 1, 1};
        KeyProvisioningOperationResult result = ackOperation("CR068_REQ_001",
                ackWith("CR068_REQ_001", response, null, otherKcv));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        verify(importProofVerifier, never()).verify(any());
        assertEquals(0, countActive());
    }

    @Test
    @DisplayName("合法 ACK（wire digest）→ 两用途各验签一次 + 原子 ACTIVE + 单次事件")
    void validAckWireDigest_activates() {
        KeyProvisioningResponse response = requestResponse("CR068_REQ_001");
        KeyProvisioningOperationResult result = ackOperation("CR068_REQ_001",
                ackWith("CR068_REQ_001", response, null, null));
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome());
        verify(importProofVerifier, times(2)).verify(any());
        assertEquals(2, countActive(), "两用途应原子激活");
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                Integer.class, "CR068_REQ_001");
        assertEquals(1, events, "完整成功仅发一次完成事件");
    }
}
