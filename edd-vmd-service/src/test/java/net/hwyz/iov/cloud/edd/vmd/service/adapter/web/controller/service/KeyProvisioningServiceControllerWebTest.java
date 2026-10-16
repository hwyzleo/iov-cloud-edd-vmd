package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VagwAuthenticatedDeviceContext;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckResult;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningError;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter.KeyProvisioningServiceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 官方 Keyprov Service 三端点 Web/MockMvc 契约测试（VMD-DSN-CR-059 §9.1/§9.3）。
 * <p>
 * 以 standalone MockMvc 校验 interface 继承的 method/path、DTO JSON 序列化与错误结构，
 * 不含身份/幂等业务编排（由门面单元测试与集成测试覆盖）。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
class KeyProvisioningServiceControllerWebTest {

    private static final String BASE = "/api/service/keyProvisioning/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KeyProvisioningServiceAdapter adapter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adapter = mock(KeyProvisioningServiceAdapter.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new KeyProvisioningServiceController(adapter)).build();
    }

    private VmdKeyProvisioningRequest request(String payloadType, byte[] bytes) {
        return VmdKeyProvisioningRequest.builder()
                .deviceContext(VagwAuthenticatedDeviceContext.builder()
                        .deviceSn("TBOX-1").hsmUid("HSM-1").brokerSessionId("S-1")
                        .connectionGeneration(1).mqttReceivedAt(1L).build())
                .payloadType(payloadType)
                .schemaVersion("keyprov-v1")
                .payloadBytes(bytes)
                .payloadDigest("digest")
                .build();
    }

    @Test
    @DisplayName("/request 返回 canonical Response base64 与 SUCCESS")
    void requestEndpoint() throws Exception {
        byte[] downlink = new byte[]{9, 8, 7};
        when(adapter.request(any())).thenReturn(VmdKeyProvisioningResponse.builder()
                .requestId("R1").payloadType("security.keyprov.v1.KeyProvisioningResponse")
                .schemaVersion("keyprov-v1").payloadBytes(downlink).payloadDigest("d")
                .expireAtMs(1000L).outcome(VmdKeyProvisioningOutcome.SUCCESS).build());

        mockMvc.perform(post(BASE + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request("security.keyprov.v1.KeyProvisioningRequest", new byte[]{1, 2, 3}))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("R1"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.payloadBytes").value(Base64.getEncoder().encodeToString(downlink)))
                .andExpect(jsonPath("$.payloadType").value("security.keyprov.v1.KeyProvisioningResponse"));
    }

    @Test
    @DisplayName("/ack 返回 AckResult 与拒绝错误结构（806xxx）")
    void ackEndpointErrorStructure() throws Exception {
        when(adapter.ack(any())).thenReturn(VmdKeyProvisioningAckResult.builder()
                .requestId("R2").outcome(VmdKeyProvisioningOutcome.REJECTED)
                .error(VmdKeyProvisioningError.builder()
                        .code("806095").message("幂等冲突").retryable(false).action("DO_NOT_RETRY").build())
                .build());

        VmdKeyProvisioningAckRequest request = VmdKeyProvisioningAckRequest.builder()
                .deviceContext(request(null, null).getDeviceContext())
                .payloadType("security.keyprov.v1.KeyProvisioningAck")
                .schemaVersion("keyprov-v1")
                .payloadBytes(new byte[]{1})
                .payloadDigest("digest")
                .build();

        mockMvc.perform(post(BASE + "/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECTED"))
                .andExpect(jsonPath("$.error.code").value("806095"))
                .andExpect(jsonPath("$.error.action").value("DO_NOT_RETRY"))
                .andExpect(jsonPath("$.error.retryable").value(false));
    }

    @Test
    @DisplayName("/status 返回非敏感状态（无 kmsKeyRef/Wrapped Key）")
    void statusEndpoint() throws Exception {
        when(adapter.status(any())).thenReturn(VmdKeyProvisioningStatusResponse.builder()
                .requestId("R3").payloadType("security.keyprov.v1.KeyProvisioningStatusResponse")
                .schemaVersion("keyprov-v1").payloadBytes(new byte[]{4, 5})
                .payloadDigest("d").outcome(VmdKeyProvisioningOutcome.SUCCESS).build());

        VmdKeyProvisioningStatusRequest request = VmdKeyProvisioningStatusRequest.builder()
                .deviceContext(request(null, null).getDeviceContext())
                .payloadType("security.keyprov.v1.KeyProvisioningStatusRequest")
                .schemaVersion("keyprov-v1")
                .payloadBytes(new byte[]{1}).payloadDigest("digest").build();

        String json = mockMvc.perform(post(BASE + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("R3"))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("kmsKeyRef"));
        org.junit.jupiter.api.Assertions.assertFalse(json.toLowerCase().contains("wrapped"));
    }
}
