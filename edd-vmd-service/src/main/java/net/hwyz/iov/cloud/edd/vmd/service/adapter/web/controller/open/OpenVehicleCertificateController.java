package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CertificateApplyRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CertificateConfirmRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateApplyResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateStatusResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateProvisioningAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆设备证书开放平台接口
 * <p>
 * 供产线MES/OAPI经API Gateway触发设备证书申请、查询和安装确认。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/open/vehicleCertificate/v1")
public class OpenVehicleCertificateController extends BaseController {

    private final CertificateProvisioningAppService certificateProvisioningAppService;

    /**
     * 申请设备证书
     *
     * @param request 申请请求
     * @return 申请结果
     */
    @RequiresPermissions("vmd:security:certificate:apply")
    @PostMapping("/apply")
    public ApiResponse<CertificateApplyResponse> apply(@RequestBody @Validated CertificateApplyRequest request) {
        log.info("开放平台请求申请设备证书, requestId={}, vin={}, deviceSn={}", 
                request.getRequestId(), request.getVin(), request.getDeviceSn());

        CertificateApplyCmd cmd = CertificateApplyCmd.builder()
                .requestId(request.getRequestId())
                .vin(request.getVin())
                .deviceCategory(request.getDeviceCategory())
                .deviceSn(request.getDeviceSn())
                .certificateProfile(request.getCertificateProfile())
                .csrDerBase64(request.getCsrDerBase64())
                .sourceSystem(request.getSourceSystem())
                .facilityNo(request.getFacilityNo())
                .lineCode(request.getLineCode())
                .build();

        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(cmd);

        CertificateApplyResponse response = CertificateApplyResponse.builder()
                .requestId(result.getRequestId())
                .status(result.getStatus())
                .certSn(result.getCertSn())
                .certificateDerBase64(result.getCertificateDerBase64())
                .chainDerBase64(result.getChainDerBase64())
                .issuer(result.getIssuer())
                .fingerprint(result.getFingerprint())
                .notBefore(result.getNotBefore())
                .notAfter(result.getNotAfter())
                .failReason(result.getFailReason())
                .build();

        return ApiResponse.ok(response);
    }

    /**
     * 查询证书申请状态
     *
     * @param requestId 业务请求ID
     * @return 状态结果
     */
    @RequiresPermissions("vmd:security:certificate:query")
    @GetMapping("/request/{requestId}")
    public ApiResponse<CertificateStatusResponse> queryStatus(@PathVariable String requestId) {
        log.info("开放平台查询证书状态, requestId={}", requestId);

        CertificateStatusResult result = certificateProvisioningAppService.queryCertificateStatus(requestId);

        CertificateStatusResponse response = CertificateStatusResponse.builder()
                .requestId(result.getRequestId())
                .status(result.getStatus())
                .certSn(result.getCertSn())
                .certificateFingerprint(result.getCertificateFingerprint())
                .notBefore(result.getNotBefore())
                .notAfter(result.getNotAfter())
                .issuedAt(result.getIssuedAt())
                .confirmedAt(result.getConfirmedAt())
                .failReason(result.getFailReason())
                .build();

        return ApiResponse.ok(response);
    }

    /**
     * 确认证书安装
     *
     * @param request 确认请求
     * @return 操作结果
     */
    @RequiresPermissions("vmd:security:certificate:confirm")
    @PostMapping("/confirmInstalled")
    public ApiResponse<Void> confirmInstalled(@RequestBody @Validated CertificateConfirmRequest request) {
        log.info("开放平台确认证书安装, requestId={}, result={}", request.getRequestId(), request.getResult());

        CertificateConfirmCmd cmd = CertificateConfirmCmd.builder()
                .requestId(request.getRequestId())
                .result(request.getResult())
                .failReason(request.getFailReason())
                .vin(request.getVin())
                .deviceSn(request.getDeviceSn())
                .sourceSystem(request.getSourceSystem())
                .facilityNo(request.getFacilityNo())
                .lineCode(request.getLineCode())
                .build();

        certificateProvisioningAppService.confirmCertificateInstalled(cmd);

        return ApiResponse.ok();
    }

}
