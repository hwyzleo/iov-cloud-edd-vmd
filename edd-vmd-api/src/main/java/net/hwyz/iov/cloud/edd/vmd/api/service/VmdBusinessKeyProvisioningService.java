package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdBusinessKeyProvisioningServiceFallbackFactory;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 业务密钥 keyprov canonical relay 服务接口（CR-058）。
 * <p>
 * 供 VAGW 将 security.keyprov.v1 三类 canonical 上行（Request/Ack/StatusRequest）连同
 * 已认证会话上下文与 Envelope 转发 VMD；VMD 返回对应 canonical 下行
 * （Response/AckResult/StatusResponse）与非敏感路由结果。
 * <p>
 * payload 必须使用同一 proto-keyprov 生成的 canonical 类型/bytes，不得转换为私有 JSON wire；
 * 服务认证身份来自调用方 workload identity，VMD 不以请求体自报 caller 授权。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@FeignClient(contextId = "businessKeyProvisioningService", value = ServiceNameConstants.EDD_VMD,
        path = "/api/service/businessKeyProvisioning/v1",
        fallbackFactory = VmdBusinessKeyProvisioningServiceFallbackFactory.class)
public interface VmdBusinessKeyProvisioningService {

    /**
     * 转发一条 canonical keyprov 上行并返回 VMD 结果。
     *
     * @param request 认证上下文 + canonical Envelope/payload
     * @return canonical 下行 payload 与非敏感路由结果
     */
    @PostMapping("/relay")
    KeyprovRelayResponse relay(@RequestBody KeyprovRelayRequest request);
}
