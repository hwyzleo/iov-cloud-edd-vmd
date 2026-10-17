package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdKeyProvisioningService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckResult;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningError;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter.KeyProvisioningServiceAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 官方 Keyprov Service 端点实现（VMD-DSN-CR-059 §2.2/§5）。
 * <p>
 * 实现 edd-vmd-api 单点发布的 {@link VmdKeyProvisioningService}，仅做委派，不写业务规则、
 * 不直接访问 Repository 或 framework-security。功能开关默认关闭；关闭时不装配本 Controller
 * 与 Keyprov 调用链（capability DISABLED，不视为故障）。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(VmdKeyProvisioningService.BASE_PATH)
@ConditionalOnProperty(name = "vmd.business-key.provisioning.keyprov.enabled", havingValue = "true")
public class KeyProvisioningServiceController implements VmdKeyProvisioningService {

    private final KeyProvisioningServiceAdapter keyProvisioningServiceAdapter;

    @Override
    public VmdKeyProvisioningResponse request(VmdKeyProvisioningRequest request) {
        log.info("Keyprov官方服务请求: hasContext={}", request != null && request.getDeviceContext() != null);
        VmdKeyProvisioningResponse response = keyProvisioningServiceAdapter.request(request);
        logOutcome("request", response == null ? null : response.getRequestId(),
                response == null ? null : response.getOutcome(),
                response == null ? null : response.getError());
        return response;
    }

    @Override
    public VmdKeyProvisioningAckResult ack(VmdKeyProvisioningAckRequest request) {
        log.info("Keyprov官方服务ACK: hasContext={}", request != null && request.getDeviceContext() != null);
        VmdKeyProvisioningAckResult result = keyProvisioningServiceAdapter.ack(request);
        logOutcome("ack", result == null ? null : result.getRequestId(),
                result == null ? null : result.getOutcome(),
                result == null ? null : result.getError());
        return result;
    }

    @Override
    public VmdKeyProvisioningStatusResponse status(VmdKeyProvisioningStatusRequest request) {
        log.info("Keyprov官方服务状态查询: hasContext={}", request != null && request.getDeviceContext() != null);
        VmdKeyProvisioningStatusResponse response = keyProvisioningServiceAdapter.status(request);
        logOutcome("status", response == null ? null : response.getRequestId(),
                response == null ? null : response.getOutcome(),
                response == null ? null : response.getError());
        return response;
    }

    /**
     * 输出官方端点的稳定结果（806xxx），用于与 VAGW 侧 reason 按 requestId 关联定位。
     * <p>
     * 只输出登记后的稳定错误码与已脱敏展示信息，不含 payload / Wrapped Key / 设备身份明文。
     */
    private static void logOutcome(String endpoint, String requestId,
                                   VmdKeyProvisioningOutcome outcome, VmdKeyProvisioningError error) {
        if (outcome == VmdKeyProvisioningOutcome.SUCCESS) {
            log.info("Keyprov官方服务 {} 成功: requestId={}", endpoint, requestId);
            return;
        }
        log.warn("Keyprov官方服务 {} 未成功: requestId={}, outcome={}, code={}, retryable={}, action={}, message={}",
                endpoint, requestId, outcome,
                error == null ? null : error.getCode(),
                error == null ? null : error.isRetryable(),
                error == null ? null : error.getAction(),
                error == null ? null : error.getMessage());
    }
}
