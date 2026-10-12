package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdBusinessKeyProvisioningService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 业务密钥 keyprov relay 服务降级处理（CR-058）。
 * <p>
 * VMD 不可用时 fail-closed：返回 UNAVAILABLE，VAGW/车端以原 requestId 对账，不换键重试。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Component
public class VmdBusinessKeyProvisioningServiceFallbackFactory
        implements FallbackFactory<VmdBusinessKeyProvisioningService> {

    @Override
    public VmdBusinessKeyProvisioningService create(Throwable throwable) {
        return new VmdBusinessKeyProvisioningService() {
            @Override
            public KeyprovRelayResponse relay(KeyprovRelayRequest request) {
                log.error("业务密钥keyprov relay调用失败: requestId={}", request == null ? null : request.getAuthenticatedHsmUid(), throwable);
                return KeyprovRelayResponse.builder()
                        .outcome("UNAVAILABLE")
                        .reasonCode("VMD_KEYPROV_UNAVAILABLE")
                        .build();
            }
        };
    }
}
