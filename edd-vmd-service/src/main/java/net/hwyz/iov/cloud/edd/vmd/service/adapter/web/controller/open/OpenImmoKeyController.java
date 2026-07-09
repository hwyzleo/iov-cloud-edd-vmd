package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ImmoKeyDeriveRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ImmoKeyDeriveResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImmoKeyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ImmoKeyDeliveryAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * 防盗根下发开放平台接口
 * <p>
 * 供总装安全工位（诊断仪 / MES）经 API Gateway 触发防盗根下发。
 * VMD 经进程内 framework-security wrapFor 派生并封装到灌注机，明文不出 KMS。
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/open/immoKey/v1")
public class OpenImmoKeyController extends BaseController {

    private final ImmoKeyDeliveryAppService immoKeyDeliveryAppService;

    /**
     * 派生防盗根并封装到指定灌注机
     *
     * @param request 下发请求
     * @return 封装结果
     */
    @RequiresPermissions("vmd:security:immoKey:derive")
    @PostMapping("/derive")
    public ApiResponse<ImmoKeyDeriveResponse> derive(@RequestBody @Validated ImmoKeyDeriveRequest request) {
        log.info("开放平台请求下发防盗根, vin={}, facilityUid={}", request.getVin(), request.getFacilityUid());
        ImmoKeyResult result = immoKeyDeliveryAppService.deriveImmoKeyForFacility(
                request.getVin(), request.getFacilityUid());
        ImmoKeyDeriveResponse response = ImmoKeyDeriveResponse.builder()
                .kmsKeyRef(result.getKmsKeyRef())
                .kcv(result.getKcv())
                .wrapped(result.getWrapped() != null
                        ? Base64.getEncoder().encodeToString(result.getWrapped())
                        : null)
                .build();
        return ApiResponse.ok(response);
    }
}
