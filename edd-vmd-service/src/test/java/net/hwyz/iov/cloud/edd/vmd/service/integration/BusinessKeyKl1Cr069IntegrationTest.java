package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.Kl1BatchSweepAppService;
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
import org.springframework.test.context.TestPropertySource;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-069 KL1 集成测试（真实 dev MySQL + {@code @MockBean KeyProvisioningTemplate/ImportProofVerifier}，
 * kl1.enabled=true 关闭 capability 的隔离联调）。
 * <p>
 * 覆盖：首次签发建立 F/B 与 immutable_content_digest、item 无损切片证据；同 rid 同内容续签
 * （新 E、items 原 bytes、Provider 调用=0、新租约）；B 到期 lazy expiry → FAILED(ACK_TIMEOUT)+释放
 * claims、迟到 ACK 拒绝；E-now&lt;20s → EXPIRED 无 items；sweeper 扫批。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Rollback
@TestPropertySource(properties = {
        "vmd.business-key.provisioning.kl1.enabled=true",
        "vmd.business-key.provisioning.kl1.sweeper.enabled=false"
})
class BusinessKeyKl1Cr069IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";
    private static final String HSM_UID = "CR069-UID-000001";
    private static final String DEVICE_SN = "CR069-TBOX-0001";
    private static final String PART_CODE = "CR069_TBOX";
    private static final String CERT_SN = "CR069-CERT-0001";
    private static final String RELEASE = "test-release-v1";
    private static final String DESCRIPTOR_SET_HASH = "test-descriptor-set-hash";
    private static final String vin = "HWYZTEST069000001";
    private static final String REQ = "KL1_REQ_001";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @MockBean
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;

    @MockBean
    private ImportProofVerifier importProofVerifier;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;

    @Autowired
    private Kl1BatchSweepAppService kl1BatchSweepAppService;

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
                    "CR069-KEY-" + purpose.name(), purpose, context.businessKeyVersion(), 7L, "v1", operationId);
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
        jdbcTemplate.execute("DELETE FROM tb_keyprov_delivery_lease WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "')");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE source_ref_id = '" + REQ + "'");
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
                "CR069_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
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

    private String statusOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, requestId);
    }

    private Integer leaseCount(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_keyprov_delivery_lease l "
                        + "JOIN tb_business_key_provisioning b ON b.id = l.provisioning_id WHERE b.request_id = ?",
                Integer.class, requestId);
    }

    private Integer leaseSeqOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT lease_seq FROM tb_business_key_provisioning WHERE request_id = ?", Integer.class, requestId);
    }

    @Test
    @DisplayName("首次签发：F/B 建立、immutable_content_digest、lifecycle_profile、item 无损切片证据")
    void firstIssue_establishesKl1Facts() {
        KeyProvisioningResponse response = requestResponse(REQ);
        assertEquals(2, response.getItemsCount());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertTrue(response.getExpireAtMs() > System.currentTimeMillis(), "首次 E 应指向未来");

        String profile = jdbcTemplate.queryForObject(
                "SELECT lifecycle_profile FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);
        assertEquals("KEYPROV-LIFECYCLE-1", profile);
        LocalDateTime f = jdbcTemplate.queryForObject(
                "SELECT first_issued_at FROM tb_business_key_provisioning WHERE request_id = ?", LocalDateTime.class, REQ);
        LocalDateTime b = jdbcTemplate.queryForObject(
                "SELECT ack_deadline_at FROM tb_business_key_provisioning WHERE request_id = ?", LocalDateTime.class, REQ);
        assertNotNull(f);
        assertNotNull(b);
        assertEquals(Duration.ofMillis(300_000L), Duration.between(f, b), "B = F + 300s 不可滑动");
        String contentDigest = jdbcTemplate.queryForObject(
                "SELECT immutable_content_digest FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);
        assertNotNull(contentDigest);
        assertEquals(64, contentDigest.length(), "SHA-256 hex");

        // item 无损切片证据
        Integer offset = jdbcTemplate.queryForObject(
                "SELECT item_wire_offset FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                        + "WHERE b.request_id = ? AND i.purpose = 'UPLINK_AEAD'", Integer.class, REQ);
        Integer length = jdbcTemplate.queryForObject(
                "SELECT item_wire_length FROM tb_business_key_provisioning_item i "
                        + "JOIN tb_business_key_provisioning b ON b.id = i.provisioning_id "
                        + "WHERE b.request_id = ? AND i.purpose = 'UPLINK_AEAD'", Integer.class, REQ);
        assertNotNull(offset);
        assertNotNull(length);
        assertTrue(length > 0, "切片长度必须为正");
    }

    @Test
    @DisplayName("同 rid 同内容续签：新 E、items 原 bytes、Provider 调用=0、新租约、content digest 不变")
    void replay_sameRid_sameContent_freshLease_noProvider() {
        KeyProvisioningResponse first = requestResponse(REQ);
        int derives = deriveCount.get();
        assertEquals(2, derives, "首次派生两用途");
        String firstItemsDigest = sha256(first.toByteArray());
        String contentDigestBefore = jdbcTemplate.queryForObject(
                "SELECT immutable_content_digest FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);

        // 同 rid 同业务内容（仅 timestamp 变化）→ 续签
        KeyProvisioningResponse replay = requestResponse(REQ);
        assertEquals(deriveCount.get(), derives, "同 rid 续签 Provider 调用必须为 0");
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, replay.getResult());
        assertEquals(first.getItemsCount(), replay.getItemsCount());
        // items bytes 必须原样（内容幂等：除 field4 外一致）
        for (int i = 0; i < first.getItemsCount(); i++) {
            assertArrayEquals(first.getItems(i).toByteArray(), replay.getItems(i).toByteArray(),
                    "item 子消息 bytes 必须原样（禁止重新序列化）");
        }
        assertNotEquals(first.getExpireAtMs(), replay.getExpireAtMs(), "续签应签发新 E");
        // content digest 不变
        String contentDigestAfter = jdbcTemplate.queryForObject(
                "SELECT immutable_content_digest FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);
        assertEquals(contentDigestBefore, contentDigestAfter, "续签后 immutable_content_digest 必须不变");
        // 新租约 + lease_seq 递增
        assertEquals(2, leaseSeqOf(REQ), "第二个租约 lease_seq=2");
        assertEquals(2, leaseCount(REQ));
        // delivery_payload_digest 变化（含本次 E）
        Integer distinctDigests = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT delivery_payload_digest) FROM tb_keyprov_delivery_lease l "
                        + "JOIN tb_business_key_provisioning b ON b.id = l.provisioning_id WHERE b.request_id = ?",
                Integer.class, REQ);
        assertEquals(2, distinctDigests, "每次投递 delivery_payload_digest 不同");
    }

    @Test
    @DisplayName("B 已到：Request 触发 lazy expiry → FAILED(ACK_TIMEOUT)+释放 claims；迟到 ACK 拒绝不激活")
    void lazyExpiry_afterDeadline_ackTimeout() {
        requestResponse(REQ);
        // B = now - 1s（Java 时钟；与 persistKl1 写入同一字面时钟，避免 MySQL NOW() 会话时区偏差）
        jdbcTemplate.update("UPDATE tb_business_key_provisioning SET ack_deadline_at = ? WHERE request_id = ?",
                LocalDateTime.now().minusSeconds(1), REQ);

        // 同 rid request → 惰性到期 → FAILED(ACK_TIMEOUT)
        KeyProvisioningOperationResult result = requestOperation(REQ);
        assertNotEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "B 已过不得交付新 bundle");
        assertEquals("FAILED", statusOf(REQ));
        String terminalReason = jdbcTemplate.queryForObject(
                "SELECT terminal_reason FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);
        assertEquals("ACK_TIMEOUT", terminalReason, "内部审计 reason=ACK_TIMEOUT；公开 Status 仍 FAILED");
        // PENDING 目录 claim 已释放
        Integer pendingLeft = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE request_id = ? AND key_state = 'PENDING'",
                Integer.class, REQ);
        assertEquals(0, pendingLeft, "到期释放本批次 pending claim");

        // 迟到 ACK → 拒绝且不激活
        KeyProvisioningAck ack = KeyProvisioningAck.newBuilder().setRequestId(REQ).build();
        byte[] ackBytes = ack.toByteArray();
        KeyProvisioningOperationResult ackResult = keyProvisioningApplicationFacade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", ackBytes, sha256(ackBytes));
        assertNotEquals(VmdKeyProvisioningOutcome.SUCCESS, ackResult.getOutcome(), "FAILED 迟到 ACK 绝不复活");
        assertEquals("FAILED", statusOf(REQ));
    }

    @Test
    @DisplayName("E-now<20s → EXPIRED 响应无 items，不 SUCCEEDED")
    void expiredNoItems_whenRemainingBelowIssueMin() {
        requestResponse(REQ);
        // B = now + 25s → E=min(now+60s, B-30s)=now-5s → E-now<20s → EXPIRED_NO_ITEMS
        jdbcTemplate.update("UPDATE tb_business_key_provisioning SET ack_deadline_at = ? WHERE request_id = ?",
                LocalDateTime.now().plusSeconds(25), REQ);

        KeyProvisioningOperationResult result = requestOperation(REQ);
        assertNotEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "EXPIRED 非 SUCCESS");
        assertNotNull(result.getDownlinkPayload());
        try {
            KeyProvisioningResponse expired = KeyProvisioningResponse.parseFrom(result.getDownlinkPayload());
            assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_EXPIRED, expired.getResult());
            assertEquals(0, expired.getItemsCount(), "EXPIRED 无 items");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        assertEquals("WAIT_DEVICE_ACK", statusOf(REQ), "EXPIRED 响应不终结批次（由 B 截止治理）");
    }

    @Test
    @DisplayName("sweeper：now≥B 的 pending 批次 CAS FAILED(ACK_TIMEOUT)，重扫幂等")
    void sweeper_terminatesPastDeadline() {
        requestResponse(REQ);
        jdbcTemplate.update("UPDATE tb_business_key_provisioning SET ack_deadline_at = ? WHERE request_id = ?",
                LocalDateTime.now().minusSeconds(1), REQ);

        int processed = kl1BatchSweepAppService.sweep(100);
        assertEquals(1, processed);
        assertEquals("FAILED", statusOf(REQ));
        String terminalReason = jdbcTemplate.queryForObject(
                "SELECT terminal_reason FROM tb_business_key_provisioning WHERE request_id = ?", String.class, REQ);
        assertEquals("ACK_TIMEOUT", terminalReason);
        // 重扫幂等：不再处理
        assertEquals(0, kl1BatchSweepAppService.sweep(100));
        assertEquals("FAILED", statusOf(REQ), "FAILED 终态不可复活");
    }
}
