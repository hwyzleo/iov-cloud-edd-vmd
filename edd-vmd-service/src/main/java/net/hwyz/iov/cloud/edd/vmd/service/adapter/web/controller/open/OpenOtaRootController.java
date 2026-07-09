package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.OtaRootDeriveRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.OtaRootDeriveResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OtaRootResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.OtaRootDeliveryAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * OTA根下发开放平台接口
 * <p>
 * 供总装安全工位（诊断仪 / MES）经 API Gateway 触发OTA根下发。
 * VMD 经进程内 framework-security wrapFor 派生并封装到灌注机，明文不出 KMS。
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/open/otaRoot/v1")
public class OpenOtaRootController extends BaseController {

    private final OtaRootDeliveryAppService otaRootDeliveryAppService;

    /**
     * 派生OTA根并封装到指定灌注机
     *
     * @param request 下发请求
     * @return 封装结果
     */
    @RequiresPermissions("vmd:security:otaRoot:derive")
    @PostMapping("/derive")
    public ApiResponse<OtaRootDeriveResponse> derive(@RequestBody @Validated OtaRootDeriveRequest request) {
        log.info("开放平台请求下发OTA根, vin={}, facilityUid={}", request.getVin(), request.getFacilityUid());
        OtaRootResult result = otaRootDeliveryAppService.deriveOtaRootForFacility(
                request.getVin(), request.getFacilityUid());
        OtaRootDeriveResponse response = OtaRootDeriveResponse.builder()
                .kmsKeyRef(result.getKmsKeyRef())
                .kcv(result.getKcv())
                .wrapped(result.getWrapped() != null
                        ? Base64.getEncoder().encodeToString(result.getWrapped())
                        : null)
                .build();
        return ApiResponse.ok(response);
    }
}
