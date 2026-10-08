package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BusinessKeyProvisionRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BusinessKeyProvisionResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyProvisionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyProvisionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyProvisioningAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务密钥在线申请开放平台接口（CR-055 §7.1）
 * <p>
 * 供 VAGW（协议转发，携带认证会话身份）或受信产线系统经 API Gateway 触发业务密钥
 * 在线申请与设备证书公钥封装下发。会话身份与 payload deviceSn 双重校验在 AppService 内完成；
 * 本接口不接收/信任请求体自报 caller。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/open/businessKey/v1")
public class OpenBusinessKeyController extends BaseController {

    private final BusinessKeyProvisioningAppService businessKeyProvisioningAppService;

    /**
     * 在线申请并下发业务密钥（信封加密到设备证书公钥）
     *
     * @param request 申请请求
     * @return 下发结果（含一次性 Wrapped Key）
     */
    @RequiresPermissions("vmd:security:businessKey:provision")
    @PostMapping("/provision")
    public ApiResponse<BusinessKeyProvisionResponse> provision(@RequestBody @Validated BusinessKeyProvisionRequest request) {
        log.info("开放平台请求业务密钥在线申请: requestId={}, deviceSn={}, domain={}, purpose={}",
                request.getRequestId(), request.getDeviceSn(), request.getBusinessDomain(), request.getPurpose());

        BusinessKeyProvisionCmd cmd = BusinessKeyProvisionCmd.builder()
                .requestId(request.getRequestId())
                .deviceSn(request.getDeviceSn())
                .deviceCategory(request.getDeviceCategory())
                .businessDomain(request.getBusinessDomain())
                .purpose(request.getPurpose())
                .build();

        BusinessKeyProvisionResult result = businessKeyProvisioningAppService.provision(cmd);

        BusinessKeyProvisionResponse response = BusinessKeyProvisionResponse.builder()
                .requestId(result.getRequestId())
                .keyId(result.getKeyId())
                .businessKeyVersion(result.getBusinessKeyVersion())
                .wrappedKeyBase64(result.getWrappedKeyBase64())
                .algorithm(result.getAlgorithm())
                .keySpec(result.getKeySpec())
                .validFrom(result.getValidFrom())
                .validTo(result.getValidTo())
                .parameters(result.getParameters())
                .state(result.getState())
                .reused(result.isReused())
                .build();
        return ApiResponse.ok(response);
    }
}
