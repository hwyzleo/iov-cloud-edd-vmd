package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vehicle.common.v1.Envelope;

import java.nio.charset.StandardCharsets;

/**
 * keyprov canonical relay 编排服务（CR-058 Q3=B / EDD-VAGW-DSN-CR-011 §3.5）。
 * <p>
 * 校验 canonical Envelope 与认证上下文一致性，按 payload_type 分发至
 * Request / ACK / StatusRequest 应用服务，并返回对应 canonical 下行 payload。
 * 功能开关默认关闭（fail-closed），仅当 VAGW/VMD/EMQX/TBOX release-set 全部就绪后开启。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyprovRelayAppService {

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_REJECTED = "REJECTED";
    private static final String OUTCOME_UNAVAILABLE = "UNAVAILABLE";

    private final BusinessKeyProvisioningBatchAppService provisioningBatchAppService;
    private final BusinessKeyProvisioningAckAppService provisioningAckAppService;
    private final BusinessKeyProvisioningStatusAppService provisioningStatusAppService;

    /**
     * keyprov capability 功能开关（默认关闭 fail-closed）
     */
    @Value("${vmd.business-key.provisioning.keyprov.enabled:false}")
    private boolean keyprovEnabled;

    /**
     * relay 单条 canonical 上行。
     */
    public KeyprovRelayResponse relay(KeyprovRelayRequest request) {
        if (!keyprovEnabled) {
            log.warn("keyprov relay 未启用（vmd.business-key.provisioning.keyprov.enabled=false），fail-closed");
            return KeyprovRelayResponse.builder().outcome(OUTCOME_UNAVAILABLE)
                    .reasonCode("VMD_KEYPROV_DISABLED").build();
        }
        if (request == null || request.getEnvelope() == null || request.getEnvelope().length == 0) {
            return reject(null, "KEYPROV_PAYLOAD_INVALID");
        }

        Envelope.VehicleMessageEnvelope envelope;
        try {
            envelope = Envelope.VehicleMessageEnvelope.parseFrom(request.getEnvelope());
        } catch (InvalidProtocolBufferException e) {
            log.warn("keyprov relay Envelope 解析失败");
            return reject(null, "KEYPROV_PAYLOAD_INVALID");
        }

        KeyprovAuthenticatedContext ctx = new KeyprovAuthenticatedContext(
                request.getSource(), request.getAuthenticatedHsmUid(), request.getClientId(),
                request.getCertSerial(), request.getCertFingerprint(), request.getConnectionGeneration(),
                request.getBrokerSessionId(), request.getReceivedAtMs());

        String invalidReason = validateEnvelope(envelope, ctx);
        if (invalidReason != null) {
            log.warn("keyprov relay Envelope 校验失败: reason={}, requestId={}", invalidReason, envelope.getRequestId());
            return reject(envelope.getRequestId(), invalidReason);
        }

        byte[] payload = envelope.getPayload().toByteArray();
        String payloadType = envelope.getPayloadType();
        try {
            if (KeyprovConstants.PAYLOAD_TYPE_REQUEST.equals(payloadType)) {
                KeyProvisioningResponse response = provisioningBatchAppService.provision(
                        KeyProvisioningRequest.parseFrom(payload), ctx);
                return success(envelope.getRequestId(), ctx,
                        KeyprovConstants.PAYLOAD_TYPE_RESPONSE, response.toByteArray(),
                        response.getExpireAtMs() > 0 ? response.getExpireAtMs() : null);
            }
            if (KeyprovConstants.PAYLOAD_TYPE_ACK.equals(payloadType)) {
                KeyProvisioningAckResult result = provisioningAckAppService.confirmImport(
                        KeyProvisioningAck.parseFrom(payload), ctx);
                return success(envelope.getRequestId(), ctx,
                        KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT, result.toByteArray(), null);
            }
            if (KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST.equals(payloadType)) {
                KeyProvisioningStatusResponse status = provisioningStatusAppService.queryStatus(
                        KeyProvisioningStatusRequest.parseFrom(payload), ctx);
                return success(envelope.getRequestId(), ctx,
                        KeyprovConstants.PAYLOAD_TYPE_STATUS_RESPONSE, status.toByteArray(), null);
            }
            return reject(envelope.getRequestId(), "KEYPROV_PROTOCOL_UNSUPPORTED");
        } catch (InvalidProtocolBufferException e) {
            log.warn("keyprov relay canonical payload 解析失败: payloadType={}", payloadType);
            return reject(envelope.getRequestId(), "KEYPROV_PAYLOAD_INVALID");
        } catch (RuntimeException e) {
            log.error("keyprov relay 业务处理异常: payloadType={}, requestId={}", payloadType, envelope.getRequestId(), e);
            return KeyprovRelayResponse.builder()
                    .requestId(envelope.getRequestId())
                    .outcome(OUTCOME_UNAVAILABLE)
                    .reasonCode("VMD_KEYPROV_UNAVAILABLE")
                    .build();
        }
    }

    private String validateEnvelope(Envelope.VehicleMessageEnvelope envelope, KeyprovAuthenticatedContext ctx) {
        if (!ctx.hasIdentity()) {
            return "KEYPROV_SESSION_CONTEXT_MISSING";
        }
        if (!KeyprovConstants.ENVELOPE_SERVICE.equals(envelope.getService())) {
            return "KEYPROV_PROTOCOL_UNSUPPORTED";
        }
        if (!KeyprovConstants.PROTOCOL_VERSION.equals(envelope.getProtocolVersion())) {
            return "KEYPROV_PROTOCOL_UNSUPPORTED";
        }
        if (envelope.getMessageKind() != Envelope.MessageKind.MESSAGE_KIND_REQUEST) {
            return "KEYPROV_PROTOCOL_UNSUPPORTED";
        }
        if (envelope.getDeviceId() == null || !envelope.getDeviceId().equalsIgnoreCase(ctx.hsmUid())) {
            return "IDENTITY_MISMATCH";
        }
        if (envelope.getRequestId().isBlank()) {
            return "KEYPROV_PAYLOAD_INVALID";
        }
        if (envelope.getRequestId().getBytes(StandardCharsets.UTF_8).length > 128) {
            return "KEYPROV_PAYLOAD_INVALID";
        }
        if (envelope.getPayload().size() > 16384) {
            return "KEYPROV_PAYLOAD_TOO_LARGE";
        }
        return null;
    }

    private KeyprovRelayResponse success(String requestId, KeyprovAuthenticatedContext ctx,
                                         String downlinkPayloadType, byte[] payload, Long expireAtMs) {
        return KeyprovRelayResponse.builder()
                .requestId(requestId)
                .downlinkPayloadType(downlinkPayloadType)
                .downlinkPayload(payload)
                .expireAtMs(expireAtMs)
                .routingHsmUid(ctx.hsmUid())
                .outcome(OUTCOME_SUCCESS)
                .build();
    }

    private KeyprovRelayResponse reject(String requestId, String reasonCode) {
        return KeyprovRelayResponse.builder()
                .requestId(requestId)
                .outcome(OUTCOME_REJECTED)
                .reasonCode(reasonCode)
                .build();
    }
}
