package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdKeyProvisioningService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckResult;
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
        return keyProvisioningServiceAdapter.request(request);
    }

    @Override
    public VmdKeyProvisioningAckResult ack(VmdKeyProvisioningAckRequest request) {
        log.info("Keyprov官方服务ACK: hasContext={}", request != null && request.getDeviceContext() != null);
        return keyProvisioningServiceAdapter.ack(request);
    }

    @Override
    public VmdKeyProvisioningStatusResponse status(VmdKeyProvisioningStatusRequest request) {
        log.info("Keyprov官方服务状态查询: hasContext={}", request != null && request.getDeviceContext() != null);
        return keyProvisioningServiceAdapter.status(request);
    }
}
