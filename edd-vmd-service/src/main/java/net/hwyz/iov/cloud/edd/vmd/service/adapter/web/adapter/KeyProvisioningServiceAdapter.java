package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VagwAuthenticatedDeviceContext;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckResult;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningError;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 官方 Keyprov Service wire/context 适配器（VMD-DSN-CR-059 §2.2/§4）。
 * <p>
 * 只做 wire 与可信上下文映射：解析调用方身份、将 {@link VagwAuthenticatedDeviceContext}
 * 映射为服务层上下文、提取 canonical payload 字段并委托 {@link KeyProvisioningApplicationFacade}，
 * 再把应用层结果映射为各端点响应 DTO。不含业务校验、幂等与编排逻辑。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyProvisioningServiceAdapter {

    private final KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;
    private final KeyProvisioningCallerResolver callerResolver;

    /**
     * canonical 协议版本（用于回填响应）
     */
    @Value("${vmd.business-key.provisioning.keyprov.schema-version:keyprov-v1}")
    private String schemaVersion;

    public VmdKeyProvisioningResponse request(VmdKeyProvisioningRequest request) {
        KeyProvisioningOperationResult result = keyProvisioningApplicationFacade.request(
                callerResolver.resolveCaller(),
                toContext(request == null ? null : request.getDeviceContext()),
                request == null ? null : request.getPayloadType(),
                request == null ? null : request.getSchemaVersion(),
                request == null ? null : request.getPayloadBytes(),
                request == null ? null : request.getPayloadDigest(),
                request == null ? null : request.getProtocolRelease(),
                request == null ? null : request.getDescriptorSetHash());
        return VmdKeyProvisioningResponse.builder()
                .requestId(result.getRequestId())
                .payloadType(result.getDownlinkPayloadType())
                .schemaVersion(schemaVersion)
                .payloadBytes(result.getDownlinkPayload())
                .payloadDigest(result.getDownlinkPayloadDigest())
                .expireAtMs(result.getExpireAtMs())
                .outcome(result.getOutcome())
                .error(toError(result))
                .build();
    }

    public VmdKeyProvisioningAckResult ack(VmdKeyProvisioningAckRequest request) {
        KeyProvisioningOperationResult result = keyProvisioningApplicationFacade.ack(
                callerResolver.resolveCaller(),
                toContext(request == null ? null : request.getDeviceContext()),
                request == null ? null : request.getPayloadType(),
                request == null ? null : request.getSchemaVersion(),
                request == null ? null : request.getPayloadBytes(),
                request == null ? null : request.getPayloadDigest());
        return VmdKeyProvisioningAckResult.builder()
                .requestId(result.getRequestId())
                .payloadType(result.getDownlinkPayloadType())
                .schemaVersion(schemaVersion)
                .payloadBytes(result.getDownlinkPayload())
                .payloadDigest(result.getDownlinkPayloadDigest())
                .outcome(result.getOutcome())
                .error(toError(result))
                .build();
    }

    public VmdKeyProvisioningStatusResponse status(VmdKeyProvisioningStatusRequest request) {
        KeyProvisioningOperationResult result = keyProvisioningApplicationFacade.status(
                callerResolver.resolveCaller(),
                toContext(request == null ? null : request.getDeviceContext()),
                request == null ? null : request.getPayloadType(),
                request == null ? null : request.getSchemaVersion(),
                request == null ? null : request.getPayloadBytes(),
                request == null ? null : request.getPayloadDigest());
        return VmdKeyProvisioningStatusResponse.builder()
                .requestId(result.getRequestId())
                .payloadType(result.getDownlinkPayloadType())
                .schemaVersion(schemaVersion)
                .payloadBytes(result.getDownlinkPayload())
                .payloadDigest(result.getDownlinkPayloadDigest())
                .outcome(result.getOutcome())
                .error(toError(result))
                .build();
    }

    private VmdKeyProvisioningError toError(KeyProvisioningOperationResult result) {
        if (result.getErrorCode() == null) {
            return null;
        }
        return VmdKeyProvisioningError.builder()
                .code(result.getErrorCode().getCode())
                .message(result.getErrorMessage())
                .retryable(result.isRetryable())
                .action(result.getAction())
                .build();
    }

    private KeyprovAuthenticatedContext toContext(VagwAuthenticatedDeviceContext context) {
        if (context == null) {
            return null;
        }
        String identity = firstNonBlank(context.getHsmUid(), context.getEcuUid());
        return new KeyprovAuthenticatedContext(
                "VAGW_AUTHENTICATED_CONTEXT",
                identity,
                context.getBrokerSessionId(),
                context.getCertificateSerial(),
                context.getCertificateFingerprint(),
                context.getConnectionGeneration(),
                context.getBrokerSessionId(),
                context.getMqttReceivedAt());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
