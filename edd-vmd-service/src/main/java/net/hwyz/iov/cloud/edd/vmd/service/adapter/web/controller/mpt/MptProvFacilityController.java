package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ProvFacilityRegisterRequest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ProvFacilityPresetAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 安全灌注机注册管理接口
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/provFacility/v1")
public class MptProvFacilityController extends BaseController {

    private final ProvFacilityPresetAppService provFacilityPresetAppService;

    /**
     * 注册安全灌注机并预置设备根
     *
     * @param request 注册请求
     * @return 结果
     */
    @Log(title = "安全灌注机注册", businessType = BusinessType.INSERT)
    @RequiresPermissions("vmd:security:provFacility:register")
    @PostMapping
    public ApiResponse<Void> register(@RequestBody @Validated ProvFacilityRegisterRequest request) {
        log.info("管理后台用户[{}]注册安全灌注机[{}]", SecurityContextHolder.getUserName(), request.getFacilityUid());
        provFacilityPresetAppService.register(request.getFacilityUid());
        return ApiResponse.ok();
    }
}
