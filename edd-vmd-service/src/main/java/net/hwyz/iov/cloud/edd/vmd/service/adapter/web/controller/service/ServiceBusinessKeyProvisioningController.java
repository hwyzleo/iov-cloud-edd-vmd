package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.KeyprovRelayRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.KeyprovRelayResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovRelayAppService;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务密钥 keyprov canonical relay 服务接口实现（CR-058 / EDD-VAGW-DSN-CR-011 §3.5）。
 * <p>
 * 供 VAGW 经受控内部 Service API 转发 security.keyprov.v1 三类 canonical 上行
 * （Request/Ack/StatusRequest）+ 已认证会话上下文 + Envelope。认证身份来自调用方
 * workload identity 与 EMQX 已认证会话，不接受请求体自报 caller。
 * <p>
 * 功能开关 {@code vmd.business-key.provisioning.keyprov.enabled} 默认关闭（fail-closed）。
 * 本接口不下发 keyRef / 明文密钥 / Wrapped Key 到日志或持久化。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/businessKeyProvisioning/v1")
public class ServiceBusinessKeyProvisioningController extends BaseController {

    private final KeyprovRelayAppService keyprovRelayAppService;

    /**
     * 转发一条 canonical keyprov 上行并返回 VMD 结果。
     *
     * @param request 认证上下文 + canonical Envelope/payload
     * @return canonical 下行 payload 与非敏感路由结果
     */
    @PostMapping("/relay")
    public KeyprovRelayResponse relay(@RequestBody KeyprovRelayRequest request) {
        log.info("keyprov relay: hsmUid摘要={}, payloadType={}",
                request == null ? null : mask(request.getAuthenticatedHsmUid()),
                request == null || request.getEnvelope() == null ? null : request.getEnvelope().length);
        return keyprovRelayAppService.relay(request);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 8 ? "***" : value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
}
