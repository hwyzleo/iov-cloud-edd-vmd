package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vehicle.common.v1.Envelope;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * KeyprovRelayAppService 单元测试（CR-058 Q3=B）。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@ExtendWith(MockitoExtension.class)
class KeyprovRelayAppServiceTest {

    private static final String HSM_UID = "HSM-UID-0001";

    @Mock
    private BusinessKeyProvisioningBatchAppService batchAppService;
    @Mock
    private BusinessKeyProvisioningAckAppService ackAppService;
    @Mock
    private BusinessKeyProvisioningStatusAppService statusAppService;

    private KeyprovRelayAppService relay;

    @BeforeEach
    void setUp() {
        relay = new KeyprovRelayAppService(batchAppService, ackAppService, statusAppService);
    }

    private byte[] envelope(String payloadType, byte[] payload, String deviceId) {
        return Envelope.VehicleMessageEnvelope.newBuilder()
                .setRequestId("REQ-1")
                .setProtocolVersion("keyprov-v1")
                .setService("security.keyprov")
                .setDeviceId(deviceId)
                .setMessageId("MSG-1")
                .setMessageKind(Envelope.MessageKind.MESSAGE_KIND_REQUEST)
                .setPayloadType(payloadType)
                .setPayload(ByteString.copyFrom(payload))
                .build()
                .toByteArray();
    }

    private KeyprovRelayRequest relayRequest(String payloadType, byte[] payload) {
        return KeyprovRelayRequest.builder()
                .source("ACL_SESSION_CORRELATED")
                .authenticatedHsmUid(HSM_UID)
                .clientId("client-1")
                .connectionGeneration(1)
                .envelope(envelope(payloadType, payload, HSM_UID))
                .canonicalPayload(payload)
                .build();
    }

    @Test
    @DisplayName("功能开关关闭 fail-closed 返回 UNAVAILABLE")
    void disabledFailsClosed() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", false);
        KeyprovRelayResponse response = relay.relay(relayRequest("security.keyprov.v1.KeyProvisioningRequest", new byte[]{1}));
        assertEquals("UNAVAILABLE", response.getOutcome());
    }

    @Test
    @DisplayName("Envelope deviceId 与已认证会话不一致 → REJECTED")
    void identityMismatchRejected() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", true);
        byte[] env = envelope("security.keyprov.v1.KeyProvisioningRequest", new byte[]{1}, "OTHER-UID");
        KeyprovRelayResponse response = relay.relay(KeyprovRelayRequest.builder()
                .authenticatedHsmUid(HSM_UID).envelope(env).build());
        assertEquals("REJECTED", response.getOutcome());
        assertEquals("IDENTITY_MISMATCH", response.getReasonCode());
    }

    @Test
    @DisplayName("canonical Request 分发至批次服务并返回 Response 下行")
    void requestDispatched() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", true);
        KeyProvisioningRequest req = KeyProvisioningRequest.newBuilder().setRequestId("REQ-1")
                .setBusinessDomain("V2C_COMM").build();
        when(batchAppService.provision(any(), any())).thenReturn(KeyProvisioningResponse.newBuilder()
                .setRequestId("REQ-1").setExpireAtMs(9999).build());
        KeyprovRelayResponse response = relay.relay(relayRequest("security.keyprov.v1.KeyProvisioningRequest", req.toByteArray()));
        assertEquals("SUCCESS", response.getOutcome());
        assertEquals("security.keyprov.v1.KeyProvisioningResponse", response.getDownlinkPayloadType());
        assertEquals(HSM_UID, response.getRoutingHsmUid());
        assertEquals(9999L, response.getExpireAtMs());
    }

    @Test
    @DisplayName("canonical Ack 分发至 ACK 服务并返回 AckResult 下行")
    void ackDispatched() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", true);
        KeyProvisioningAck ack = KeyProvisioningAck.newBuilder().setRequestId("REQ-1").build();
        when(ackAppService.confirmImport(any(), any())).thenReturn(KeyProvisioningAckResult.newBuilder()
                .setRequestId("REQ-1").build());
        KeyprovRelayResponse response = relay.relay(relayRequest("security.keyprov.v1.KeyProvisioningAck", ack.toByteArray()));
        assertEquals("SUCCESS", response.getOutcome());
        assertEquals("security.keyprov.v1.KeyProvisioningAckResult", response.getDownlinkPayloadType());
    }

    @Test
    @DisplayName("canonical StatusRequest 分发至状态服务并返回 StatusResponse 下行")
    void statusDispatched() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", true);
        KeyProvisioningStatusRequest req = KeyProvisioningStatusRequest.newBuilder().setRequestId("REQ-1").build();
        when(statusAppService.queryStatus(any(), any())).thenReturn(KeyProvisioningStatusResponse.newBuilder()
                .setRequestId("REQ-1").build());
        KeyprovRelayResponse response = relay.relay(relayRequest("security.keyprov.v1.KeyProvisioningStatusRequest", req.toByteArray()));
        assertEquals("SUCCESS", response.getOutcome());
        assertEquals("security.keyprov.v1.KeyProvisioningStatusResponse", response.getDownlinkPayloadType());
    }

    @Test
    @DisplayName("非登记 payload_type → REJECTED")
    void unsupportedPayloadTypeRejected() {
        ReflectionTestUtils.setField(relay, "keyprovEnabled", true);
        KeyprovRelayResponse response = relay.relay(relayRequest("security.keyprov.v1.Unknown", new byte[]{1}));
        assertEquals("REJECTED", response.getOutcome());
        assertEquals("KEYPROV_PROTOCOL_UNSUPPORTED", response.getReasonCode());
    }
}
