package net.hwyz.iov.cloud.edd.vmd.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VagwAuthenticatedDeviceContext;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningError;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 官方 Keyprov Service API artifact 契约测试（VMD-DSN-CR-059 §9.1）。
 * <p>
 * 校验 interface、contextId、service 名、method/path 与 DTO 序列化/错误结构稳定，
 * 保障 VAGW 只依赖 edd-vmd-api 发布制品即可注入 Feign Bean。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
class VmdKeyProvisioningServiceContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Feign 契约：contextId/service/path 单点发布")
    void feignClientContract() {
        FeignClient feignClient = VmdKeyProvisioningService.class.getAnnotation(FeignClient.class);
        assertNotNull(feignClient, "必须声明 @FeignClient");
        assertEquals("vmdKeyProvisioningService", feignClient.contextId());
        assertEquals(ServiceNameConstants.EDD_VMD, feignClient.value());
        assertEquals("/api/service/keyProvisioning/v1", feignClient.path());
        assertEquals(VmdKeyProvisioningService.BASE_PATH, feignClient.path());
        assertTrue(feignClient.fallbackFactory() == void.class || feignClient.fallbackFactory() == Void.class,
                "官方契约不得携带 fallback");
    }

    @Test
    @DisplayName("三类入口 method/path 稳定")
    void endpointContract() throws Exception {
        assertPostPath("request", VmdKeyProvisioningRequest.class, "/request");
        assertPostPath("ack", VmdKeyProvisioningAckRequest.class, "/ack");
        assertPostPath("status", net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusRequest.class, "/status");
    }

    private void assertPostPath(String method, Class<?> requestType, String expectedPath) throws Exception {
        Method declared = VmdKeyProvisioningService.class.getMethod(method, requestType);
        PostMapping postMapping = declared.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, method + " 必须为 POST");
        assertArrayEquals(new String[]{expectedPath}, postMapping.value());
    }

    @Test
    @DisplayName("请求 DTO 序列化为 base64 payload，toString 不含敏感内容")
    void requestDtoSerialization() throws Exception {
        byte[] payload = new byte[]{10, 20, 30, 40, 50};
        VmdKeyProvisioningRequest request = VmdKeyProvisioningRequest.builder()
                .deviceContext(VagwAuthenticatedDeviceContext.builder()
                        .deviceSn("TBOX-1").hsmUid("HSM-1").brokerSessionId("S-1")
                        .connectionGeneration(3).certificateSerial("CERT-1").mqttReceivedAt(1000L).build())
                .payloadType("security.keyprov.v1.KeyProvisioningRequest")
                .schemaVersion("keyprov-v1")
                .payloadBytes(payload)
                .payloadDigest("digest-1")
                .build();

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains(Base64.getEncoder().encodeToString(payload)), "payloadBytes 应以 base64 承载");
        assertTrue(json.contains("\"payloadType\""));

        VmdKeyProvisioningRequest roundTrip = objectMapper.readValue(json, VmdKeyProvisioningRequest.class);
        assertArrayEquals(payload, roundTrip.getPayloadBytes());
        assertEquals("HSM-1", roundTrip.getDeviceContext().getHsmUid());
        assertFalse(request.toString().contains(Base64.getEncoder().encodeToString(payload)),
                "toString 不得输出 payloadBytes");
    }

    @Test
    @DisplayName("响应 DTO 错误结构稳定")
    void responseErrorStructure() throws Exception {
        VmdKeyProvisioningResponse response = VmdKeyProvisioningResponse.builder()
                .requestId("REQ-1")
                .outcome(VmdKeyProvisioningOutcome.REJECTED)
                .error(VmdKeyProvisioningError.builder()
                        .code("806094").message("payload非法").retryable(false).action("DO_NOT_RETRY").build())
                .build();
        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("806094"));
        assertTrue(json.contains("REJECTED"));
        assertTrue(json.contains("DO_NOT_RETRY"));
        assertFalse(json.contains("kmsKeyRef"), "错误响应不得含 KMS 引用");
        assertFalse(json.contains("wrapped"), "错误响应不得含 Wrapped Key");
    }
}
