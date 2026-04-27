package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptPlatformAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PlatformAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆平台相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/platform/v1")
public class MptPlatformController extends BaseController {

    private final PlatformAppService platformAppService;

    /**
     * 分页查询车辆平台信息
     *
     * @param platform 车辆平台信息
     * @return 车辆平台信息列表
     */
    @RequiresPermissions("completeVehicle:product:platform:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PlatformVo>> list(PlatformVo platform) {
        log.info("管理后台用户[{}]分页查询车辆平台信息", SecurityUtils.getUsername());
        startPage();
        PlatformQuery query = PlatformQuery.builder()
                .code(platform.getCode())
                .name(platform.getName())
                .beginTime(getBeginTime(platform))
                .endTime(getEndTime(platform))
                .build();
        List<PlatformDto> platformDtoList = platformAppService.search(query);
        return ApiResponse.ok(getPageResult(MptPlatformAssembler.INSTANCE.fromDtoList(platformDtoList)));
    }

    /**
     * 导出车辆平台信息
     *
     * @param response 响应
     * @param platform 车辆平台信息
     */
    @Log(title = "车辆平台管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:platform:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PlatformVo platform) {
        log.info("管理后台用户[{}]导出车辆平台信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆平台ID获取车辆平台信息
     *
     * @param platformId 车辆平台ID
     * @return 车辆平台信息
     */
    @RequiresPermissions("completeVehicle:product:platform:query")
    @GetMapping(value = "/{platformId}")
    public ApiResponse<PlatformVo> getInfo(@PathVariable Long platformId) {
        log.info("管理后台用户[{}]根据车辆平台ID[{}]获取车辆平台信息", SecurityUtils.getUsername(), platformId);
        return ApiResponse.ok(MptPlatformAssembler.INSTANCE.fromDto(platformAppService.getPlatformById(platformId)));
    }

    /**
     * 新增车辆平台信息
     *
     * @param platform 车辆平台信息
     * @return 结果
     */
    @Log(title = "车辆平台管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:platform:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody PlatformVo platform) {
        log.info("管理后台用户[{}]新增车辆平台信息[{}]", SecurityUtils.getUsername(), platform.getCode());
        if (!platformAppService.checkCodeUnique(platform.getId(), platform.getCode())) {
            return ApiResponse.fail("新增车辆平台'" + platform.getCode() + "'失败，车辆平台代码已存在");
        }
        platformAppService.createPlatform(MptPlatformAssembler.INSTANCE.toDto(platform), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车辆平台信息
     *
     * @param platform 车辆平台信息
     * @return 结果
     */
    @Log(title = "车辆平台管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:platform:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody PlatformVo platform) {
        log.info("管理后台用户[{}]修改保存车辆平台信息[{}]", SecurityUtils.getUsername(), platform.getCode());
        if (!platformAppService.checkCodeUnique(platform.getId(), platform.getCode())) {
            return ApiResponse.fail("修改保存车辆平台'" + platform.getCode() + "'失败，车辆平台代码已存在");
        }
        platformAppService.modifyPlatform(MptPlatformAssembler.INSTANCE.toDto(platform), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车辆平台信息
     *
     * @param platformIds 车辆平台ID数组
     * @return 结果
     */
    @Log(title = "车辆平台管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:platform:remove")
    @DeleteMapping("/{platformIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] platformIds) {
        log.info("管理后台用户[{}]删除车辆平台信息[{}]", SecurityUtils.getUsername(), platformIds);
        for (Long platformId : platformIds) {
            if (platformAppService.checkPlatformSeriesExist(platformId)) {
                return ApiResponse.fail("删除车辆平台'" + platformId + "'失败，该车辆平台下存在车系");
            }
            if (platformAppService.checkPlatformVehicleExist(platformId)) {
                return ApiResponse.fail("删除车辆平台'" + platformId + "'失败，该车辆平台下存在车辆");
            }
        }
        return platformAppService.deletePlatformByIds(platformIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
