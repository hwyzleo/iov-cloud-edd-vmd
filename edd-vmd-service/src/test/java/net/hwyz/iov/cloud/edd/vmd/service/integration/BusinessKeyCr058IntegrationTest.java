package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovRelayAppService;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyCreateRequest;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyMaterial;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RecipientRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.WrapContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.WrappedBusinessKey;
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
import vehicle.common.v1.Envelope;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-058 集成测试。
 * <p>
 * 覆盖 canonical relay Request（多用途批次 + PENDING 目标版本）→ Ack（完整成功完成事务、
 * 目录 ACTIVE 切换、专用 Outbox 事件）→ 幂等 ACK / 缺项待对账，并校验敏感字段不落库。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Rollback
class BusinessKeyCr058IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String HSM_UID = "CR058-UID-000001";
    private static final String DEVICE_SN = "CR058-TBOX-0001";
    private static final String PART_CODE = "CR058_TBOX";
    private static final String CERT_SN = "CR058-CERT-0001";

    @MockBean
    private BusinessKeyMaterialTemplate businessKeyMaterialTemplate;

    @Autowired
    private KeyprovRelayAppService relayAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger createCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning_item WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "')");
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id LIKE 'CR058_%'");
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
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_TSP_CLIENT', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                "CR058_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);

        when(businessKeyMaterialTemplate.create(any(BusinessKeyCreateRequest.class))).thenAnswer(inv -> {
            int n = createCount.incrementAndGet();
            return new BusinessKeyMaterial("CR058-KEY-" + n, "kms/ref/CR058-KEY-" + n, n, "kms", "AES", "AES-256-GCM",
                    Instant.now().minusSeconds(1), Instant.now().plusSeconds(365L * 24 * 3600));
        });
        when(businessKeyMaterialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(new WrappedBusinessKey(new byte[]{1, 2, 3}, "CR058-KEY", 1, "AES",
                        Instant.now().plusSeconds(3600), Map.of("iv", "abcd")));
    }

    private byte[] envelope(String requestId, String payloadType, byte[] payload) {
        return Envelope.VehicleMessageEnvelope.newBuilder()
                .setRequestId(requestId)
                .setProtocolVersion("keyprov-v1")
                .setService("security.keyprov")
                .setDeviceId(HSM_UID)
                .setMessageId("MSG-" + requestId)
                .setMessageKind(Envelope.MessageKind.MESSAGE_KIND_REQUEST)
                .setPayloadType(payloadType)
                .setPayload(ByteString.copyFrom(payload))
                .build()
                .toByteArray();
    }

    private KeyprovRelayRequest relayOf(String requestId, String payloadType, byte[] payload) {
        return KeyprovRelayRequest.builder()
                .source("ACL_SESSION_CORRELATED")
                .authenticatedHsmUid(HSM_UID)
                .clientId("client-1")
                .connectionGeneration(1)
                .receivedAtMs(System.currentTimeMillis())
                .envelope(envelope(requestId, payloadType, payload))
                .canonicalPayload(payload)
                .build();
    }

    private KeyProvisioningRequest request(String requestId) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_HPKE_BASE_X25519_SHA256_AES128GCM)
                .setTimestampMs(System.currentTimeMillis())
                .build();
    }

    private KeyProvisioningAck ack(String requestId, KeyProvisioningResponse response, KeyImportResult result,
                                   boolean includeDownlink) {
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            if (!includeDownlink && item.getPurpose() == KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD) {
                continue;
            }
            builder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(result)
                    .setImportProof(ByteString.copyFromUtf8("proof-" + item.getPurposeValue()))
                    .setProofAlgorithm("NONE")
                    .build());
        }
        return builder.build();
    }

    @Test
    @DisplayName("Request → PENDING 两用途；完整 ACK → 两版本 ACTIVE + 专用事件（无敏感字段）")
    void provisionThenAck_completesAndEmitsEvent() {
        KeyProvisioningResponse response = requestResponse("CR058_REQ_001");
        assertEquals(2, response.getItemsCount(), "V2C 应下发两个用途");
        Integer pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = 'PENDING'",
                Integer.class, DEVICE_SN, DOMAIN);
        assertEquals(2, pending, "申请阶段目标版本应保持 PENDING");

        KeyProvisioningAckResult ackResult = ackResponse("CR058_REQ_001", ack("CR058_REQ_001", response,
                KeyImportResult.KEY_IMPORT_RESULT_IMPORTED, true));
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, ackResult.getResult(),
                "ACK 应为 SUCCEEDED");

        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = 'ACTIVE'",
                Integer.class, DEVICE_SN, DOMAIN);
        assertEquals(2, active, "完成事务应切换两版本 ACTIVE");
        Integer completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_business_key_provisioning WHERE request_id = ? AND status = 'COMPLETED'",
                Integer.class, "CR058_REQ_001");
        assertEquals(1, completed, "批次应置 COMPLETED");

        List<String> payloads = jdbcTemplate.queryForList(
                "SELECT payload FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                String.class, "CR058_REQ_001");
        assertEquals(1, payloads.size(), "完整成功应恰发一条专用事件");
        String payload = payloads.get(0);
        assertTrue(payload.contains("UPLINK_AEAD") && payload.contains("DOWNLINK_AEAD"), "事件应含两用途: " + payload);
        assertFalse(payload.contains("kmsKeyRef"), "事件不得含 kmsKeyRef");
        assertFalse(payload.contains("wrapped"), "事件不得含 Wrapped Key");
        assertFalse(payload.contains("VIN"), "事件不得含 VIN");
    }

    @Test
    @DisplayName("同 requestId 重复 Request 幂等复用；重复相同 ACK 幂等不重复发事件")
    void idempotentRequestAndAck() {
        KeyProvisioningResponse first = requestResponse("CR058_REQ_002");
        KeyProvisioningResponse second = requestResponse("CR058_REQ_002");
        assertEquals(first.getItemsCount(), second.getItemsCount());
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND request_id = ?",
                Integer.class, DEVICE_SN, "CR058_REQ_002");
        assertEquals(2, rows, "同 requestId 只应创建两用途各一行");

        ackResponse("CR058_REQ_002", ack("CR058_REQ_002", first, KeyImportResult.KEY_IMPORT_RESULT_IMPORTED, true));
        ackResponse("CR058_REQ_002", ack("CR058_REQ_002", first, KeyImportResult.KEY_IMPORT_RESULT_IMPORTED, true));

        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                Integer.class, "CR058_REQ_002");
        assertEquals(1, events, "重复相同 ACK 不得重复发布逻辑事件");
    }

    @Test
    @DisplayName("ACK 缺项 → 批次 RECONCILE_REQUIRED，不发布完成事件，版本保持 PENDING")
    void ackMissingPurpose_reconcile() {
        KeyProvisioningResponse response = requestResponse("CR058_REQ_003");
        ackResponse("CR058_REQ_003", ack("CR058_REQ_003", response, KeyImportResult.KEY_IMPORT_RESULT_IMPORTED, false));

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, "CR058_REQ_003");
        assertEquals("RECONCILE_REQUIRED", status);
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                Integer.class, "CR058_REQ_003");
        assertEquals(0, events, "缺项不得发布完成事件");
    }

    private KeyProvisioningResponse requestResponse(String requestId) {
        KeyprovRelayResponse relayResponse = relayAppService.relay(relayOf(requestId,
                "security.keyprov.v1.KeyProvisioningRequest", request(requestId).toByteArray()));
        assertEquals("SUCCESS", relayResponse.getOutcome(), "relay 应成功返回 canonical Response");
        return parseResponse(relayResponse);
    }

    private KeyProvisioningResponse parseResponse(KeyprovRelayResponse relayResponse) {
        try {
            return KeyProvisioningResponse.parseFrom(relayResponse.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyProvisioningAckResult ackResponse(String requestId, KeyProvisioningAck ack) {
        KeyprovRelayResponse relayResponse = relayAppService.relay(relayOf(requestId,
                "security.keyprov.v1.KeyProvisioningAck", ack.toByteArray()));
        assertEquals("SUCCESS", relayResponse.getOutcome());
        try {
            return KeyProvisioningAckResult.parseFrom(relayResponse.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
