package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateMaterialUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusConfigurationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatus;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * KeyProvisioningApplicationFacade 单元测试（VMD-DSN-CR-059）。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@ExtendWith(MockitoExtension.class)
class KeyProvisioningApplicationFacadeTest {

    private static final String CALLER = "VAGW";
    private static final String HSM_UID = "HSM-UID-0001";
    private static final String RELEASE = "test-release-v1";
    private static final String DESCRIPTOR_SET_HASH = "test-descriptor-set-hash";

    @Mock
    private BusinessKeyProvisioningBatchAppService batchAppService;
    @Mock
    private BusinessKeyProvisioningAckAppService ackAppService;
    @Mock
    private BusinessKeyProvisioningStatusAppService statusAppService;
    @Mock
    private BusinessKeyProvisioningMetrics metrics;

    private KeyProvisioningApplicationFacade facade;

    @BeforeEach
    void setUp() {
        facade = new KeyProvisioningApplicationFacade(batchAppService, ackAppService, statusAppService, metrics);
        ReflectionTestUtils.setField(facade, "keyprovEnabled", true);
        ReflectionTestUtils.setField(facade, "allowedCallers", "VAGW");
        ReflectionTestUtils.setField(facade, "expectedSchemaVersion", "keyprov-v1");
        ReflectionTestUtils.setField(facade, "maxPayloadBytes", 16384);
        ReflectionTestUtils.setField(facade, "pinnedProtocolRelease", RELEASE);
        ReflectionTestUtils.setField(facade, "pinnedDescriptorSetHash", DESCRIPTOR_SET_HASH);
    }

    /**
     * 以测试固定的 release/DescriptorSet hash 调用 /request 端点（CR-062 §4.1 精确 allowlist 门禁）。
     */
    private KeyProvisioningOperationResult request(String caller, KeyprovAuthenticatedContext ctx,
                                                    String payloadType, String schemaVersion,
                                                    byte[] payloadBytes, String payloadDigest) {
        return facade.request(caller, ctx, payloadType, schemaVersion, payloadBytes, payloadDigest,
                RELEASE, DESCRIPTOR_SET_HASH);
    }

    private KeyprovAuthenticatedContext ctx() {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", HSM_UID, "client-1",
                "CERT-1", null, 1, "session-1", System.currentTimeMillis());
    }

    private byte[] requestPayload(String requestId) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setBusinessDomain("V2C_COMM")
                .build().toByteArray();
    }

    private String digest(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyProvisioningResponse rejectedResponse(String requestId, String code) {
        return KeyProvisioningResponse.newBuilder()
                .setRequestId(requestId)
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED)
                .setError(ProtocolError.newBuilder().setCode(code).setMessage("x").build())
                .build();
    }

    private KeyProvisioningResponse retryableResponse(String requestId, String code) {
        return KeyProvisioningResponse.newBuilder()
                .setRequestId(requestId)
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE)
                .setError(ProtocolError.newBuilder().setCode(code).setRetryable(true).setMessage("x").build())
                .build();
    }

    @Test
    @DisplayName("capability 关闭 → UNAVAILABLE 且不调用业务服务")
    void disabledFailsClosed() {
        ReflectionTestUtils.setField(facade, "keyprovEnabled", false);
        byte[] payload = requestPayload("R1");
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE, result.getErrorCode());
    }

    @Test
    @DisplayName("调用方非 VAGW → 806090，不调用 framework")
    void unauthorizedCaller() {
        byte[] payload = requestPayload("R1");
        KeyProvisioningOperationResult result = request("UNKNOWN", ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, result.getOutcome());
        assertEquals(VmdErrorCode.KEYPROV_CALLER_UNAUTHORIZED, result.getErrorCode());
    }

    @Test
    @DisplayName("缺少可信设备上下文 → 806091")
    void missingContext() {
        byte[] payload = requestPayload("R1");
        KeyProvisioningOperationResult result = request(CALLER, null,
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DEVICE_CONTEXT_MISMATCH, result.getErrorCode());
    }

    @Test
    @DisplayName("payloadType 与端点不匹配 → 806094")
    void payloadTypeMismatch() {
        byte[] payload = requestPayload("R1");
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID, result.getErrorCode());
    }

    @Test
    @DisplayName("payload 摘要不匹配 → 806094")
    void digestMismatch() {
        byte[] payload = requestPayload("R1");
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, "deadbeef");
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID, result.getErrorCode());
    }

    @Test
    @DisplayName("成功路径返回 canonical Response bytes 与摘要")
    void successPath() {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningResponse.newBuilder()
                        .setRequestId("R1")
                        .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                        .setExpireAtMs(9999L)
                        .build());
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome());
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_RESPONSE, result.getDownlinkPayloadType());
        assertNotNull(result.getDownlinkPayloadDigest());
        assertEquals(9999L, result.getExpireAtMs());
    }

    @Test
    @DisplayName("业务拒绝错误码映射：幂等冲突/批次不存在/待对账")
    void businessErrorMapping() {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT));
        assertEquals(VmdErrorCode.KEYPROV_IDEMPOTENCY_CONFLICT, request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload)).getErrorCode());

        when(statusAppService.queryStatus(any(KeyProvisioningStatusRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningStatusResponse.newBuilder()
                        .setRequestId("R1")
                        .setError(ProtocolError.newBuilder().setCode(KeyprovConstants.ERR_NOT_FOUND).setMessage("x").build())
                        .build());
        byte[] statusPayload = KeyProvisioningStatusRequest.newBuilder().setRequestId("R1").build().toByteArray();
        assertEquals(VmdErrorCode.KEYPROV_BATCH_NOT_FOUND, facade.status(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, "keyprov-v1", statusPayload, digest(statusPayload)).getErrorCode());
    }

    @Test
    @DisplayName("CR-061 根派生错误码映射：VIN非法/Provider未就绪/派生不支持 → 806106～806108")
    void derivedRootErrorMapping() {
        byte[] payload = requestPayload("R1");
        // 806106 VIN 绑定非法 → REJECTED 不可重试
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_VIN_BINDING_INVALID));
        KeyProvisioningOperationResult vinInvalid = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_VIN_BINDING_INVALID, vinInvalid.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, vinInvalid.getOutcome());
        assertFalse(vinInvalid.isRetryable());

        // 806107 根派生 Provider 未就绪 → UNAVAILABLE 可重试（同 requestId），非结果未知
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY));
        KeyProvisioningOperationResult notReady = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY, notReady.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, notReady.getOutcome());
        assertTrue(notReady.isRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", notReady.getAction());

        // 806108 派生/材料模型不受支持 → REJECTED 不可重试
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_DERIVATION_UNSUPPORTED));
        KeyProvisioningOperationResult unsupported = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DERIVATION_UNSUPPORTED, unsupported.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, unsupported.getOutcome());
        assertFalse(unsupported.isRetryable());
    }

    @Test
    @DisplayName("CR-066 逻辑身份/版本代际冲突映射：806130/806131 → REJECTED 不可重试（防 806100 死循环）")
    void logicalIdentityErrorMapping() {
        byte[] payload = requestPayload("R1");
        // 806130 同 keyId 不可变元数据冲突 → REJECTED 不可重试（DO_NOT_RETRY）
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_LOGICAL_KEY_IDENTITY_CONFLICT));
        KeyProvisioningOperationResult identityConflict = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_LOGICAL_IDENTITY_CONFLICT, identityConflict.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, identityConflict.getOutcome());
        assertFalse(identityConflict.isRetryable());
        assertEquals("DO_NOT_RETRY", identityConflict.getAction());

        // 806131 声明版本与实际代际不一致 → REJECTED 不可重试
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_VERSION_GENERATION_MISMATCH));
        KeyProvisioningOperationResult genMismatch = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_VERSION_GENERATION_MISMATCH, genMismatch.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, genMismatch.getOutcome());
        assertFalse(genMismatch.isRetryable());
    }

    @Test
    @DisplayName("framework 依赖不可用 → 806096；结果未知 → 806098")
    void exceptionMapping() {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenThrow(new CryptoDependencyUnavailableException("kms down"));
        KeyProvisioningOperationResult unavailable = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE, unavailable.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, unavailable.getOutcome());

        when(ackAppService.confirmImport(any(KeyProvisioningAck.class), any(KeyprovAuthenticatedContext.class)))
                .thenThrow(new CryptoOperationOutcomeUnknownException("unknown"));
        byte[] ackPayload = KeyProvisioningAck.newBuilder().setRequestId("R1").build().toByteArray();
        KeyProvisioningOperationResult unknown = facade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", ackPayload, digest(ackPayload));
        assertEquals(VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN, unknown.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, unknown.getOutcome());
        assertEquals("QUERY_STATUS_LATER", unknown.getAction());
    }

    @Test
    @DisplayName("ACK 成功路径返回 AckResult bytes")
    void ackSuccess() {
        byte[] ackPayload = KeyProvisioningAck.newBuilder().setRequestId("R1").build().toByteArray();
        when(ackAppService.confirmImport(any(KeyProvisioningAck.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningAckResult.newBuilder()
                        .setRequestId("R1")
                        .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                        .build());
        KeyProvisioningOperationResult result = facade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", ackPayload, digest(ackPayload));
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, result.getOutcome());
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT, result.getDownlinkPayloadType());
    }

    @Test
    @DisplayName("CR-060 证书解析/Provider 异常 → 806101～806105 与 retryable 语义")
    void certificateResolutionErrorMapping() {
        byte[] payload = requestPayload("R1");
        // 806101 材料暂不可取/locator 缺失 → UNAVAILABLE 可重试
        assertCertificateMapping(payload, new CertificateMaterialUnavailableException("no material"),
                VmdErrorCode.KEYPROV_CERTIFICATE_MATERIAL_UNAVAILABLE, VmdKeyProvisioningOutcome.UNAVAILABLE, true);
        // 806102 证据冲突 → REJECTED 不可重试
        assertCertificateMapping(payload, new CertificateEvidenceMismatchException("evidence mismatch"),
                VmdErrorCode.KEYPROV_CERTIFICATE_EVIDENCE_MISMATCH, VmdKeyProvisioningOutcome.REJECTED, false);
        // 806103 目录多命中 → REJECTED 不可重试（数据治理）
        assertCertificateMapping(payload, new CertificateReferenceAmbiguousException("ambiguous"),
                VmdErrorCode.KEYPROV_CERTIFICATE_REFERENCE_AMBIGUOUS, VmdKeyProvisioningOutcome.REJECTED, false);
        // 806104 受信链/有效期/KU-EKU/Profile 策略拒绝 → REJECTED 不可重试
        assertCertificateMapping(payload, new CertificateProfileNotAllowedException("profile not allowed"),
                VmdErrorCode.KEYPROV_CERTIFICATE_POLICY_REJECTED, VmdKeyProvisioningOutcome.REJECTED, false);
        // 806105 状态能力配置错误/未就绪 → UNAVAILABLE 可重试
        assertCertificateMapping(payload, new CertificateStatusConfigurationException("status misconfig"),
                VmdErrorCode.KEYPROV_PROVIDER_NOT_READY, VmdKeyProvisioningOutcome.UNAVAILABLE, true);
        // 806105 business-key Provider 未就绪 → UNAVAILABLE 可重试
        assertCertificateMapping(payload, new BusinessKeyKmsUnavailableException("provider not ready"),
                VmdErrorCode.KEYPROV_PROVIDER_NOT_READY, VmdKeyProvisioningOutcome.UNAVAILABLE, true);
    }

    @Test
    @DisplayName("门禁拒绝(806094) 仍返回 canonical Response：requestId 最佳努力回填，车端可据 requestId 收敛")
    void gateRejectionCarriesCanonicalPayload() throws Exception {
        byte[] payload = requestPayload("R1");
        // 摘要不匹配：payloadType/schemaVersion 合法、payload 本身可解析 → 可回填 requestId
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, "deadbeef");
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID, result.getErrorCode());
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_RESPONSE, result.getDownlinkPayloadType());
        KeyProvisioningResponse response = decodeResponse(result);
        assertEquals("R1", response.getRequestId());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertTrue(response.hasError());
        assertEquals(KeyprovConstants.ERR_PROTOCOL_INVALID, response.getError().getCode());
        assertFalse(response.getError().getRetryable());
        assertEquals("DO_NOT_RETRY", response.getError().getAction());
    }

    @Test
    @DisplayName("业务拒绝透传精确协议错误码与 result，不降级为通用 806094 语义")
    void businessRejectionKeepsPreciseProtocolSemantics() throws Exception {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_ITEM_MISMATCH));
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        // VAGW 侧稳定码：ITEM_MISMATCH 归入 806094
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID, result.getErrorCode());
        KeyProvisioningResponse response = decodeResponse(result);
        assertEquals("R1", response.getRequestId());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        // 车端侧保持业务精确码，不被降级
        assertEquals(KeyprovConstants.ERR_ITEM_MISMATCH, response.getError().getCode());
    }

    @Test
    @DisplayName("依赖类失败返回可重试 canonical payload（result=RETRYABLE_FAILURE, action=RETRY_SAME_REQUEST_ID）")
    void dependencyFailureCarriesRetryableCanonicalPayload() throws Exception {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenThrow(new CryptoDependencyUnavailableException("kms down"));
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, result.getOutcome());
        KeyProvisioningResponse response = decodeResponse(result);
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, response.getResult());
        assertTrue(response.getError().getRetryable());
        assertEquals(KeyprovConstants.ERR_UNAVAILABLE, response.getError().getCode());
        assertEquals("RETRY_SAME_REQUEST_ID", response.getError().getAction());
    }

    @Test
    @DisplayName("ACK/Status 拒绝分别返回 AckResult/StatusResponse canonical payload")
    void ackAndStatusRejectionsCarryCanonicalPayload() throws Exception {
        byte[] ackPayload = KeyProvisioningAck.newBuilder().setRequestId("R1").build().toByteArray();
        when(ackAppService.confirmImport(any(KeyProvisioningAck.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningAckResult.newBuilder()
                        .setRequestId("R1")
                        .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED)
                        .setError(ProtocolError.newBuilder()
                                .setCode(KeyprovConstants.ERR_ACK_CONFLICT).setMessage("x").build())
                        .build());
        KeyProvisioningOperationResult ackResult = facade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", ackPayload, digest(ackPayload));
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT, ackResult.getDownlinkPayloadType());
        KeyProvisioningAckResult ack = KeyProvisioningAckResult.parseFrom(ackResult.getDownlinkPayload());
        assertEquals("R1", ack.getRequestId());
        assertEquals(KeyprovConstants.ERR_ACK_CONFLICT, ack.getError().getCode());

        byte[] statusPayload = KeyProvisioningStatusRequest.newBuilder().setRequestId("R1").build().toByteArray();
        when(statusAppService.queryStatus(any(KeyProvisioningStatusRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningStatusResponse.newBuilder()
                        .setRequestId("R1")
                        .setError(ProtocolError.newBuilder()
                                .setCode(KeyprovConstants.ERR_NOT_FOUND).setMessage("x").build())
                        .build());
        KeyProvisioningOperationResult statusResult = facade.status(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, "keyprov-v1", statusPayload, digest(statusPayload));
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_STATUS_RESPONSE, statusResult.getDownlinkPayloadType());
        KeyProvisioningStatusResponse status = KeyProvisioningStatusResponse.parseFrom(statusResult.getDownlinkPayload());
        assertEquals("R1", status.getRequestId());
        assertEquals(KeyprovConstants.ERR_NOT_FOUND, status.getError().getCode());
        // StatusResponse 无 result 字段
        assertEquals(VmdErrorCode.KEYPROV_BATCH_NOT_FOUND, statusResult.getErrorCode());
    }

    @Test
    @DisplayName("无法回填 requestId 时仍返回 canonical payload（request_id 为空，由 VAGW envelope 关联）")
    void unparseablePayloadStillCarriesCanonicalPayload() throws Exception {
        byte[] payload = new byte[] {(byte) 0xFF, (byte) 0xFE, 0x01};
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_REQUEST_INVALID, result.getErrorCode());
        KeyProvisioningResponse response = decodeResponse(result);
        assertEquals("", response.getRequestId());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_PROTOCOL_INVALID, response.getError().getCode());
    }

    @Test
    @DisplayName("CR-067 §4.2/§4.4/§5：按 protocol code 分流——806096 重发原 Request、806098 仅 Status 对账、806099 DO_NOT_RETRY")
    void cr067RecoveryTupleMapping() throws Exception {
        byte[] payload = requestPayload("R1");
        // 1) 批次前 gate/目录/依赖暂不可用（ERR_UNAVAILABLE + RETRYABLE_FAILURE）→ UNAVAILABLE/RETRY_SAME_REQUEST_ID/806096
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(retryableResponse("R1", KeyprovConstants.ERR_UNAVAILABLE));
        KeyProvisioningOperationResult unavailable = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE, unavailable.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, unavailable.getOutcome());
        assertTrue(unavailable.isRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", unavailable.getAction());
        KeyProvisioningResponse canonUnavailable = decodeResponse(unavailable);
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, canonUnavailable.getResult());
        assertEquals(KeyprovConstants.ERR_UNAVAILABLE, canonUnavailable.getError().getCode());
        assertTrue(canonUnavailable.getError().getRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", canonUnavailable.getError().getAction());

        // 2) 批次已落库结果未知（ERR_RECONCILE_REQUIRED + RETRYABLE_FAILURE）→ OUTCOME_UNKNOWN/QUERY_STATUS_LATER/806098
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(retryableResponse("R1", KeyprovConstants.ERR_RECONCILE_REQUIRED));
        KeyProvisioningOperationResult unknown = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN, unknown.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, unknown.getOutcome());
        assertTrue(unknown.isRetryable());
        assertEquals("QUERY_STATUS_LATER", unknown.getAction());
        KeyProvisioningResponse canonUnknown = decodeResponse(unknown);
        assertEquals(KeyprovConstants.ERR_RECONCILE_REQUIRED, canonUnknown.getError().getCode());
        assertTrue(canonUnknown.getError().getRetryable());
        assertEquals("QUERY_STATUS_LATER", canonUnknown.getError().getAction());

        // 3) Status 批次不存在（ERR_NOT_FOUND）→ REJECTED/DO_NOT_RETRY/806099，且 canonical 四元组一致
        byte[] statusPayload = KeyProvisioningStatusRequest.newBuilder().setRequestId("R1").build().toByteArray();
        when(statusAppService.queryStatus(any(KeyProvisioningStatusRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(KeyProvisioningStatusResponse.newBuilder()
                        .setRequestId("R1")
                        .setStatus(KeyProvisioningStatus.KEY_PROVISIONING_STATUS_UNKNOWN)
                        .setError(ProtocolError.newBuilder()
                                .setCode(KeyprovConstants.ERR_NOT_FOUND).setMessage("不存在").build())
                        .build());
        KeyProvisioningOperationResult notFound = facade.status(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, "keyprov-v1", statusPayload, digest(statusPayload));
        assertEquals(VmdErrorCode.KEYPROV_BATCH_NOT_FOUND, notFound.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, notFound.getOutcome());
        assertFalse(notFound.isRetryable());
        assertEquals("DO_NOT_RETRY", notFound.getAction());
        KeyProvisioningStatusResponse canonNotFound =
                KeyProvisioningStatusResponse.parseFrom(notFound.getDownlinkPayload());
        assertEquals(KeyprovConstants.ERR_NOT_FOUND, canonNotFound.getError().getCode());
        assertFalse(canonNotFound.getError().getRetryable());
        assertEquals("DO_NOT_RETRY", canonNotFound.getError().getAction());

        // 4) §4.4 双向映射一致性：806096↔ERR_UNAVAILABLE、806098↔ERR_RECONCILE_REQUIRED、806099↔ERR_NOT_FOUND
        assertEquals(VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE,
                ReflectionTestUtils.invokeMethod(facade, "mapProtocolCode", KeyprovConstants.ERR_UNAVAILABLE));
        assertEquals(VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                ReflectionTestUtils.invokeMethod(facade, "mapProtocolCode", KeyprovConstants.ERR_RECONCILE_REQUIRED));
        assertEquals(VmdErrorCode.KEYPROV_BATCH_NOT_FOUND,
                ReflectionTestUtils.invokeMethod(facade, "mapProtocolCode", KeyprovConstants.ERR_NOT_FOUND));
        assertEquals(KeyprovConstants.ERR_UNAVAILABLE,
                ReflectionTestUtils.invokeMethod(facade, "mapErrorCodeToProtocolCode",
                        VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE));
        assertEquals(KeyprovConstants.ERR_RECONCILE_REQUIRED,
                ReflectionTestUtils.invokeMethod(facade, "mapErrorCodeToProtocolCode",
                        VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN));
        assertEquals(KeyprovConstants.ERR_NOT_FOUND,
                ReflectionTestUtils.invokeMethod(facade, "mapErrorCodeToProtocolCode",
                        VmdErrorCode.KEYPROV_BATCH_NOT_FOUND));
    }

    @Test
    @DisplayName("CR-067 §8.1：确定性 Profile/身份拒绝不被 retryable fallback 覆盖（仍 DO_NOT_RETRY）")
    void cr067DeterministicRejectNotOverriddenByRetryableFallback() throws Exception {
        byte[] payload = requestPayload("R1");
        // 确定性策略拒绝（ERR_CERTIFICATE_INVALID / ERR_DERIVATION_UNSUPPORTED）即使结果带 REJECTED 也不落入可重试分支
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(rejectedResponse("R1", KeyprovConstants.ERR_CERTIFICATE_INVALID));
        KeyProvisioningOperationResult rejected = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdKeyProvisioningOutcome.REJECTED, rejected.getOutcome());
        assertFalse(rejected.isRetryable());
        assertEquals("DO_NOT_RETRY", rejected.getAction());
        assertEquals(VmdErrorCode.KEYPROV_CERTIFICATE_INVALID, rejected.getErrorCode());
    }

    @Test
    @DisplayName("CR-067 §8.1：确定性依赖不可用（ERR_DERIVED_PROVIDER_NOT_READY）保持 UNAVAILABLE/RETRY_SAME_REQUEST_ID")
    void cr067DerivedProviderNotReadyStaysRetryRequest() throws Exception {
        byte[] payload = requestPayload("R1");
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenReturn(retryableResponse("R1", KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY));
        KeyProvisioningOperationResult notReady = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        assertEquals(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY, notReady.getErrorCode());
        assertEquals(VmdKeyProvisioningOutcome.UNAVAILABLE, notReady.getOutcome());
        assertTrue(notReady.isRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", notReady.getAction());
    }

    @Test
    @DisplayName("CR-067 §8.1：恢复元组不自洽时 fail-fast（error() 构造后校验）")
    void cr067InvalidRecoveryTupleFailsFast() {
        // error() 不允许出现 outcome 与 action 矛盾的四元组（如 OUTCOME_UNKNOWN + RETRY_SAME_REQUEST_ID）
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(facade, "error", "R1",
                        VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN, VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN,
                        false, "x"));
    }

    private KeyProvisioningResponse decodeResponse(KeyProvisioningOperationResult result) throws Exception {
        assertNotNull(result.getDownlinkPayload(), "非成功结果必须携带 canonical 下行 payload");
        assertEquals(digest(result.getDownlinkPayload()), result.getDownlinkPayloadDigest());
        return KeyProvisioningResponse.parseFrom(result.getDownlinkPayload());
    }

    private void assertCertificateMapping(byte[] payload, RuntimeException ex, VmdErrorCode expectedCode,
                                          VmdKeyProvisioningOutcome expectedOutcome, boolean expectedRetryable) {
        when(batchAppService.provision(any(KeyProvisioningRequest.class), any(KeyprovAuthenticatedContext.class)))
                .thenThrow(ex);
        KeyProvisioningOperationResult result = request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", payload, digest(payload));
        String label = ex.getClass().getSimpleName();
        assertEquals(expectedCode, result.getErrorCode(), label);
        assertEquals(expectedOutcome, result.getOutcome(), label);
        assertEquals(expectedRetryable, result.isRetryable(), label);
        assertEquals("R1", result.getRequestId(), "原 requestId 必须透传: " + label);
        assertNotNull(result.getDownlinkPayload(), "证书/Provider 失败必须返回 canonical 下行 payload: " + label);
        assertEquals(KeyprovConstants.PAYLOAD_TYPE_RESPONSE, result.getDownlinkPayloadType(), label);
    }
}
