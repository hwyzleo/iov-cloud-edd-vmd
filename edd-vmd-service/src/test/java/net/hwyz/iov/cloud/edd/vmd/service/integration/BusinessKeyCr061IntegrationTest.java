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
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
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
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatus;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-061 集成测试（真实 dev MySQL + {@code @MockBean KeyProvisioningTemplate}）。
 * <p>
 * 覆盖 V2C keyprov 由随机材料改为 Transit/HSM 根派生的关键行为：首次开通两用途
 * {@code deriveAndWrap} 并落非敏感字段、同 requestId 幂等复用、purpose 域隔离、
 * VIN 非法在调用 framework 前 fail-closed（{@code deriveAndWrap} 调用 0 次、不落库）、
 * ACK 后原子 ACTIVE、Status 对账，并校验落库/事件不含 VIN 原文与派生材料。
 * <p>
 * OpenBao Transit {@code v2c-comm-root} 真实端到端不可用（部署阻断项），故对
 * {@link KeyProvisioningTemplate} 采用 {@code @MockBean} 口径。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Rollback
class BusinessKeyCr061IntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";

    private static final String HSM_UID = "CR061-UID-000001";
    private static final String DEVICE_SN = "CR061-TBOX-0001";
    private static final String PART_CODE = "CR061_TBOX";
    private static final String CERT_SN = "CR061-CERT-0001";

    /** 17 位但含非法字符 'I' 的 VIN：通过 DB 长度约束，须在 VIN 规范化处 fail-closed */
    private static final String INVALID_VIN = "HWYZTEST00000000I";
    private static final String INVALID_HSM_UID = "CR061-UID-000002";
    private static final String INVALID_DEVICE_SN = "CR061-TBOX-0002";
    private static final String INVALID_PART_CODE = "CR061_TBOX_BAD";
    private static final String INVALID_CERT_SN = "CR061-CERT-0002";

    private static final int ROOT_KEY_VERSION = 7;
    private static final String DERIVATION_VERSION = "v1";
    private static final String MATERIAL_MODEL = "DERIVED_ROOT";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @MockBean
    private ImportProofVerifier importProofVerifier;

    /**
     * CR-063：新 ECDH-ES Request 证书前置门禁 mock 放行（门禁逻辑由 TboxEcdhCertificateEligibilityGateTest 单测覆盖）
     */
    @MockBean
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger deriveCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        clean(DEVICE_SN, vin);
        clean(INVALID_DEVICE_SN, INVALID_VIN);
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent'");
        jdbcTemplate.execute("DELETE FROM tb_vmd_outbox WHERE event_type = 'BusinessKeyChangedEvent' AND source_ref_id LIKE 'CR061_%'");

        insertFixture(vin, DEVICE_SN, HSM_UID, PART_CODE, CERT_SN);
        insertFixture(INVALID_VIN, INVALID_DEVICE_SN, INVALID_HSM_UID, INVALID_PART_CODE, INVALID_CERT_SN);

        // 根派生：logicalKeyId 按 purpose 稳定（满足 uk_key_id + 幂等重放字节一致），Wrapped Key 仅瞬时存在
        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            deriveCount.incrementAndGet();
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey("CR061-KEY-" + purpose.name(), purpose,
                    context.businessKeyVersion(), ROOT_KEY_VERSION, DERIVATION_VERSION, operationId);
        });
        // CR-062：ImportProofVerifier 在集成测试中以 mock 驱动编排；真实验签由 framework 单测/E2E 覆盖
        when(importProofVerifier.verify(any())).thenAnswer(inv -> {
            net.hwyz.iov.cloud.framework.security.crypto.model.importproof.ImportProofVerificationRequest req =
                    inv.getArgument(0);
            return new VerifiedImportProof(ImportProofAlgorithm.ECDSA_P256_SHA256_P1363,
                    req.expectedCertificateDigest(), req.purpose(), new byte[32], java.time.Instant.now());
        });
        when(tboxEcdhCertificateEligibilityGate.evaluate(any()))
                .thenReturn(new TboxEcdhCertificateEligibilityGate.GateResult(true, false, "ok"));
    }

    private void clean(String deviceSn, String vinValue) {
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning_item WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + deviceSn + "')");
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning WHERE device_sn = '" + deviceSn + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + deviceSn + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE device_sn = '" + deviceSn + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + vinValue + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_security_constant WHERE sn = '" + deviceSn + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + deviceSn + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + vinValue + "'");
    }

    private void insertFixture(String vinValue, String deviceSn, String hsmUid, String partCode, String certSn) {
        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) "
                + "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", vinValue);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                partCode, deviceSn, "{\"hsm\":\"" + hsmUid + "\"}");
        jdbcTemplate.update("INSERT INTO tb_part_security_constant (part_code, sn, chip_uid, constant_type, preset_state) "
                + "VALUES (?, ?, ?, 'ROOT', 'PRESET')", partCode, deviceSn, hsmUid);
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                vinValue, deviceSn);
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_DEVICE_ECDH_ES_P256_V1', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                "CR061_CERT_REQ_" + deviceSn, certSn, vinValue, deviceSn, hsmUid, vinValue, deviceSn);
    }

    private KeyprovAuthenticatedContext ctx(String hsmUid) {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", hsmUid, "client-1",
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

    private KeyProvisioningAck ack(String requestId, KeyProvisioningResponse response, KeyImportResult result) {
        KeyProvisioningAck.Builder builder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            builder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(result)
                    .setImportProof(ByteString.copyFrom(new byte[64]))
                    .setProofAlgorithm("")
                    .setImportProofAlgorithm(
                            net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                                    .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363)
                    .setEnvelopeDigest(item.getEnvelopeDigest())
                    .setKeyCheckValue(item.getKeyCheckValue())
                    .setEnvelopeFormat(item.getEnvelopeFormat())
                    .setRecipientCertificateDigest(
                            ByteString.copyFrom(KeyprovEnvelopeTestFixtures.RECIPIENT_CERT_DIGEST))
                    .build());
        }
        return builder.build();
    }

    private KeyProvisioningResponse requestResponse(String requestId, String hsmUid) {
        KeyProvisioningOperationResult result = requestOperation(requestId, hsmUid);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "request 应成功");
        return parse(result, KeyProvisioningResponse::parseFrom);
    }

    private KeyProvisioningOperationResult requestOperation(String requestId, String hsmUid) {
        byte[] payload = request(requestId).toByteArray();
        return keyProvisioningApplicationFacade.request(CALLER, ctx(hsmUid),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, sha256(payload),
                "test-release-v1", "test-descriptor-set-hash");
    }

    private KeyProvisioningAckResult ackResponse(String requestId, KeyProvisioningAck ack) {
        byte[] payload = ack.toByteArray();
        KeyProvisioningOperationResult result = keyProvisioningApplicationFacade.ack(CALLER, ctx(HSM_UID),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", payload, sha256(payload));
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "ack 应成功");
        return parse(result, KeyProvisioningAckResult::parseFrom);
    }

    private KeyProvisioningStatusResponse statusResponse(String requestId) {
        byte[] payload = KeyProvisioningStatusRequest.newBuilder()
                .setRequestId(requestId)
                .setTimestampMs(System.currentTimeMillis())
                .build().toByteArray();
        KeyProvisioningOperationResult result = keyProvisioningApplicationFacade.status(CALLER, ctx(HSM_UID),
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, "keyprov-v1", payload, sha256(payload));
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome(), "status 应成功");
        return parse(result, KeyProvisioningStatusResponse::parseFrom);
    }

    private <T> T parse(KeyProvisioningOperationResult result, ProtoParser<T> parser) {
        try {
            return parser.parse(result.getDownlinkPayload());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("首次开通：两用途根派生，落 DERIVED_ROOT/根版本/派生版本/operationId 非敏感字段且 purpose 域隔离")
    void provision_twoPurposes_derivesAndPersistsNonSensitiveFields() {
        KeyProvisioningResponse response = requestResponse("CR061_REQ_001", HSM_UID);

        assertEquals(2, response.getItemsCount(), "V2C 应下发两个用途");
        Set<String> keyIds = new HashSet<>();
        response.getItemsList().forEach(item -> {
            assertFalse(item.getKeyId().isBlank(), "应返回 logicalKeyId");
            assertTrue(item.getWrappedKey().size() > 0, "应返回一次性 Wrapped Key");
            keyIds.add(item.getKeyId());
        });
        assertEquals(2, keyIds.size(), "两用途根派生逻辑 keyId 必须隔离（uk_key_id）");

        for (String purpose : KeyprovConstants.V2C_PURPOSES) {
            var row = jdbcTemplate.queryForMap(
                    "SELECT key_id, kms_key_ref, material_model, root_key_version, derivation_version, operation_id, key_state "
                            + "FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND purpose = ?",
                    DEVICE_SN, DOMAIN, purpose);
            assertEquals(MATERIAL_MODEL, row.get("material_model"), "材料模型应为 DERIVED_ROOT: " + purpose);
            assertEquals(ROOT_KEY_VERSION, ((Number) row.get("root_key_version")).intValue(), "应落根版本: " + purpose);
            assertEquals(DERIVATION_VERSION, row.get("derivation_version"), "应落派生版本: " + purpose);
            assertNotNull(row.get("operation_id"), "应落 operationId: " + purpose);
            assertNotNull(row.get("key_id"));
            assertNotNull(row.get("kms_key_ref"));
            assertEquals("PENDING", row.get("key_state"), "ACK 前目标版本保持 PENDING: " + purpose);

            String keyId = (String) row.get("key_id");
            String kmsRef = (String) row.get("kms_key_ref");
            String operationId = (String) row.get("operation_id");
            assertFalse(keyId.contains(vin), "keyId 不得含 VIN 原文");
            assertFalse(kmsRef.contains(vin), "kmsKeyRef 不得含 VIN 原文");
            assertFalse(operationId.contains(vin), "operationId 不得含 VIN 原文");
        }

        verify(keyProvisioningTemplate, org.mockito.Mockito.times(2))
                .deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        // 申请阶段不发布完成事件
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                Integer.class, "CR061_REQ_001");
        assertEquals(0, events, "ACK 前不得发布完成事件");
    }

    @Test
    @DisplayName("同 requestId 重复请求幂等复用同一逻辑材料，不新建目录版本")
    void provision_sameRequestId_idempotent() {
        KeyProvisioningResponse first = requestResponse("CR061_REQ_002", HSM_UID);
        KeyProvisioningResponse second = requestResponse("CR061_REQ_002", HSM_UID);

        Set<String> firstKeyIds = new HashSet<>();
        first.getItemsList().forEach(i -> firstKeyIds.add(i.getKeyId()));
        Set<String> secondKeyIds = new HashSet<>();
        second.getItemsList().forEach(i -> secondKeyIds.add(i.getKeyId()));
        assertEquals(firstKeyIds, secondKeyIds, "同 requestId 应复用同一逻辑材料 keyId");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ?",
                Integer.class, DEVICE_SN, DOMAIN);
        assertEquals(2, rows, "同 requestId 只应有两用途各一行");
    }

    @Test
    @DisplayName("根派生依赖不可用 → 806107 可重试：批次保持 PREPARING、版本保持 PENDING，同 requestId 重试复用同版本成功")
    void provision_dependencyUnavailable_retrySameRequestId_reusesReservedVersion() {
        String requestId = "CR061_REQ_005";
        doThrow(new CryptoDependencyUnavailableException(
                "derive and wrap derived key unavailable (request not sent)"))
                .when(keyProvisioningTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        KeyProvisioningOperationResult first = requestOperation(requestId, HSM_UID);
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, first.getOutcome(), "依赖不可用应为 UNAVAILABLE（非结果未知）");
        assertEquals(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY, first.getErrorCode(), "应映射为 806107");
        assertTrue(first.isRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", first.getAction());

        var batchRow = jdbcTemplate.queryForMap(
                "SELECT status, fail_reason FROM tb_business_key_provisioning WHERE request_id = ?", requestId);
        assertEquals("PREPARING", batchRow.get("status"), "可重试依赖失败不得终态化批次（否则同 requestId 重试被拒 806094）");
        assertNotNull(batchRow.get("fail_reason"), "应记录依赖不可用原因供诊断");
        assertEquals(0, countKeys("FAILED"), "可重试依赖失败不得把目标版本置 FAILED");
        Long reservedVersion = jdbcTemplate.queryForObject(
                "SELECT business_key_version FROM tb_device_business_key WHERE device_sn = ? AND purpose = 'UPLINK_AEAD'",
                Long.class, DEVICE_SN);
        assertNotNull(reservedVersion, "应保留本次请求占位的目标版本");

        // 依赖恢复：以原 requestId 重试（当前桩会抛异常，须用 doAnswer 改写行为）
        doAnswer(inv -> {
            deriveCount.incrementAndGet();
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey("CR061-KEY-" + purpose.name(), purpose,
                    context.businessKeyVersion(), ROOT_KEY_VERSION, DERIVATION_VERSION, operationId);
        }).when(keyProvisioningTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
        KeyProvisioningResponse retry = requestResponse(requestId, HSM_UID);

        assertEquals(2, retry.getItemsCount(), "重试应完成两用途下发");
        assertEquals(2, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ?",
                        Integer.class, DEVICE_SN, DOMAIN),
                "重试应复用预留版本，不新建版本、不遗留孤儿 PENDING");
        assertEquals(reservedVersion, jdbcTemplate.queryForObject(
                        "SELECT business_key_version FROM tb_device_business_key WHERE device_sn = ? AND purpose = 'UPLINK_AEAD'",
                        Long.class, DEVICE_SN),
                "同 requestId 重试应复用同一 businessKeyVersion（operationId/幂等键稳定）");
        assertEquals("WAIT_DEVICE_ACK", jdbcTemplate.queryForObject(
                        "SELECT status FROM tb_business_key_provisioning WHERE request_id = ?", String.class, requestId),
                "重试成功应推进批次");
        assertEquals(0, countKeys("FAILED"), "重试成功后不遗留 FAILED 目标版本");
        assertEquals(KeyProvisioningStatus.KEY_PROVISIONING_STATUS_ACK_PENDING,
                statusResponse(requestId).getStatus(), "Status 应返回 ACK_PENDING");
    }

    private int countKeys(String keyState) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = ?",
                Integer.class, DEVICE_SN, DOMAIN, keyState);
    }

    @Test
    @DisplayName("VIN 非法：调用 framework 前 fail-closed，deriveAndWrap 0 次且不落库")
    void provision_invalidVin_failClosedBeforeFramework() {
        KeyProvisioningOperationResult result = requestOperation("CR061_REQ_003", INVALID_HSM_UID);

        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome(), "VIN 非法应为安全拒绝");
        assertEquals(VmdErrorCode.KEYPROV_VIN_BINDING_INVALID, result.getErrorCode(),
                "应映射为 806106 VIN/绑定无效");
        verify(keyProvisioningTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ?", Integer.class, INVALID_DEVICE_SN);
        assertEquals(0, rows, "VIN 非法不得落任何业务密钥行");
        Integer batches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_business_key_provisioning WHERE device_sn = ?", Integer.class, INVALID_DEVICE_SN);
        assertEquals(0, batches, "VIN 非法不得创建开通批次");
    }

    @Test
    @DisplayName("Request → 完整 ACK → 两版本原子 ACTIVE + 专用事件（保留派生非敏感字段、无敏感数据）")
    void provisionThenAck_atomicActive() {
        KeyProvisioningResponse response = requestResponse("CR061_REQ_004", HSM_UID);
        KeyProvisioningAckResult ackResult = ackResponse("CR061_REQ_004",
                ack("CR061_REQ_004", response, KeyImportResult.KEY_IMPORT_RESULT_IMPORTED));
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, ackResult.getResult(), "ACK 应为 SUCCEEDED");

        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = 'ACTIVE'",
                Integer.class, DEVICE_SN, DOMAIN);
        assertEquals(2, active, "完成事务应切换两版本 ACTIVE");
        Integer preserved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? "
                        + "AND material_model = ? AND root_key_version = ? AND derivation_version = ? AND operation_id IS NOT NULL",
                Integer.class, DEVICE_SN, DOMAIN, MATERIAL_MODEL, ROOT_KEY_VERSION, DERIVATION_VERSION);
        assertEquals(2, preserved, "ACTIVE 后应保留派生非敏感字段");

        List<String> payloads = jdbcTemplate.queryForList(
                "SELECT payload FROM tb_vmd_outbox WHERE event_type = 'TboxV2cKeyProvisionedEvent' AND source_ref_id = ?",
                String.class, "CR061_REQ_004");
        assertEquals(1, payloads.size(), "完整成功应恰发一条专用事件");
        String payload = payloads.get(0);
        assertTrue(payload.contains("UPLINK_AEAD") && payload.contains("DOWNLINK_AEAD"), "事件应含两用途: " + payload);
        assertFalse(payload.contains("kmsKeyRef"), "事件不得含 kmsKeyRef");
        assertFalse(payload.contains("wrapped"), "事件不得含 Wrapped Key");
        assertFalse(payload.contains("VIN"), "事件不得含 VIN");

        KeyProvisioningStatusResponse status = statusResponse("CR061_REQ_004");
        assertEquals("CR061_REQ_004", status.getRequestId());
        assertEquals(KeyProvisioningStatus.KEY_PROVISIONING_STATUS_ACTIVE, status.getStatus(), "Status 应返回 ACTIVE");
        assertEquals(2, status.getItemsCount(), "Status 应返回两用途非敏感元数据");
    }

    @FunctionalInterface
    private interface ProtoParser<T> {
        T parse(byte[] bytes) throws Exception;
    }
}
