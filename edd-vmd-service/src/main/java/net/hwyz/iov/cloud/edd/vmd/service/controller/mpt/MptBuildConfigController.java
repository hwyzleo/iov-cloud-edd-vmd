package net.hwyz.iov.cloud.edd.vmd.service.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.BuildConfigAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.BuildConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBuildConfigDo;
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
 * 生产配置相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/buildConfig/v1")
public class MptBuildConfigController extends BaseController {

    private final BuildConfigAppService buildConfigAppService;

    /**
     * 分页查询生产配置信息
     *
     * @param buildConfig 生产配置信息
     * @return 生产配置信息列表
     */
    @RequiresPermissions("completeVehicle:product:buildConfig:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BuildConfigVo>> list(BuildConfigVo buildConfig) {
        log.info("管理后台用户[{}]分页查询生产配置信息", SecurityUtils.getUsername());
        startPage();
        List<BuildConfigVo> buildConfigVoList = buildConfigAppService.search(buildConfig.getPlatformCode(), buildConfig.getSeriesCode(),
                buildConfig.getModelCode(), buildConfig.getCode(), buildConfig.getName(), getBeginTime(buildConfig), getEndTime(buildConfig));
        return ApiResponse.ok(getPageResult(buildConfigVoList));
    }

    /**
     * 导出生产配置信息
     *
     * @param response    响应
     * @param buildConfig 生产配置信息
     */
    @Log(title = "生产配置管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:buildConfig:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, BuildConfigVo buildConfig) {
        log.info("管理后台用户[{}]导出生产配置信息", SecurityUtils.getUsername());
    }

    /**
     * 根据生产配置ID获取生产配置信息
     *
     * @param buildConfigId 生产配置ID
     * @return 生产配置信息
     */
    @RequiresPermissions("completeVehicle:product:buildConfig:query")
    @GetMapping(value = "/{buildConfigId}")
    public ApiResponse<BuildConfigVo> getInfo(@PathVariable Long buildConfigId) {
        log.info("管理后台用户[{}]根据生产配置ID[{}]获取生产配置信息", SecurityUtils.getUsername(), buildConfigId);
        VmdVehBuildConfigDo buildConfigPo = buildConfigAppService.getBuildConfigById(buildConfigId);
        return ApiResponse.ok(BuildConfigMapper.INSTANCE.fromDo(buildConfigPo));
    }

    /**
     * 新增生产配置信息
     *
     * @param buildConfig 生产配置信息
     * @return 结果
     */
    @Log(title = "生产配置管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:buildConfig:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody BuildConfigVo buildConfig) {
        log.info("管理后台用户[{}]新增生产配置信息[{}]", SecurityUtils.getUsername(), buildConfig.getCode());
        if (!buildConfigAppService.checkCodeUnique(buildConfig.getId(), buildConfig.getCode())) {
            return ApiResponse.fail("新增生产配置'" + buildConfig.getCode() + "'失败，生产配置代码已存在");
        }
        VmdVehBuildConfigDo buildConfigPo = BuildConfigMapper.INSTANCE.toDo(buildConfig);
        buildConfigPo.setCreateBy(SecurityUtils.getUserId().toString());
        return buildConfigAppService.createBuildConfig(buildConfigPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
    }

    /**
     * 修改保存生产配置信息
     *
     * @param buildConfig 生产配置信息
     * @return 结果
     */
    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:buildConfig:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody BuildConfigVo buildConfig) {
        log.info("管理后台用户[{}]修改保存生产配置信息[{}]", SecurityUtils.getUsername(), buildConfig.getCode());
        if (!buildConfigAppService.checkCodeUnique(buildConfig.getId(), buildConfig.getCode())) {
            return ApiResponse.fail("修改保存生产配置'" + buildConfig.getCode() + "'失败，生产配置代码已存在");
        }
        VmdVehBuildConfigDo buildConfigPo = BuildConfigMapper.INSTANCE.toDo(buildConfig);
        buildConfigPo.setModifyBy(SecurityUtils.getUserId().toString());
        return buildConfigAppService.modifyBuildConfig(buildConfigPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
    }

    /**
     * 删除生产配置信息
     *
     * @param buildConfigIds 生产配置ID数组
     * @return 结果
     */
    @Log(title = "生产配置管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:buildConfig:remove")
    @DeleteMapping("/{buildConfigIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] buildConfigIds) {
        log.info("管理后台用户[{}]删除生产配置信息[{}]", SecurityUtils.getUsername(), buildConfigIds);
        for (Long buildConfigId : buildConfigIds) {
            if (buildConfigAppService.checkBuildConfigVehicleExist(buildConfigId)) {
                return ApiResponse.fail("删除生产配置'" + buildConfigId + "'失败，该生产配置下存在车辆");
            }
        }
        return buildConfigAppService.deleteBuildConfigByIds(buildConfigIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
