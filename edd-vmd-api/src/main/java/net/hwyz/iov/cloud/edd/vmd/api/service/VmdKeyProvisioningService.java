package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningAckResult;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningStatusResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 官方 Keyprov Service Feign 契约（VMD-DSN-CR-059 / VMD-REQ-CR-059 US-063）。
 * <p>
 * VMD 在 edd-vmd-api 单点发布并唯一维护本契约，VAGW 只升级并引用已发布 artifact，
 * 不得依赖 edd-vmd-service，也不得复制第二套 @FeignClient、URL 或 wire DTO。
 * <p>
 * 三类入口分别对应 canonical keyprov 的 Request、Ack、StatusRequest，端到端进入 VMD
 * 既有业务密钥开通/ACK/状态查询应用服务；wire 只承载可信设备上下文与 canonical payload
 * bytes，不引入私有 JSON codec。本契约不提供 fallback：依赖不可用时由 VAGW 侧专属
 * 熔断/超时治理，并以原 requestId 通过 /status 对账，避免误建第二批次。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@FeignClient(contextId = "vmdKeyProvisioningService", value = ServiceNameConstants.EDD_VMD,
        path = VmdKeyProvisioningService.BASE_PATH)
public interface VmdKeyProvisioningService {

    /**
     * 官方 Keyprov Service 基础路径（单点发布，实现方与调用方均引用本常量）
     */
    String BASE_PATH = "/api/service/keyProvisioning/v1";

    /**
     * 业务密钥开通申请。
     *
     * @param request 可信设备上下文 + canonical KeyProvisioningRequest bytes
     * @return canonical KeyProvisioningResponse 或稳定 806xxx 错误
     */
    @PostMapping("/request")
    VmdKeyProvisioningResponse request(@RequestBody VmdKeyProvisioningRequest request);

    /**
     * 设备导入 ACK。
     *
     * @param request 可信设备上下文 + canonical KeyProvisioningAck bytes
     * @return canonical KeyProvisioningAckResult 或稳定 806xxx 错误
     */
    @PostMapping("/ack")
    VmdKeyProvisioningAckResult ack(@RequestBody VmdKeyProvisioningAckRequest request);

    /**
     * 按原 requestId 查询开通状态（只读，不创建版本、不重放请求）。
     *
     * @param request 可信设备上下文 + canonical KeyProvisioningStatusRequest bytes
     * @return canonical KeyProvisioningStatusResponse 或稳定 806xxx 错误
     */
    @PostMapping("/status")
    VmdKeyProvisioningStatusResponse status(@RequestBody VmdKeyProvisioningStatusRequest request);
}
