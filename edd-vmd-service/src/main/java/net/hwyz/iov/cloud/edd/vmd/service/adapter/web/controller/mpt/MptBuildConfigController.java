package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptBuildConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.BuildConfigQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BuildConfigAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/buildConfig/v1")
public class MptBuildConfigController extends BaseController {

    private final BuildConfigAppService buildConfigAppService;

    @RequiresPermissions("completeVehicle:product:buildConfig:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BuildConfigResponse>> list(BuildConfigRequest buildConfig) {
        log.info("管理后台用户[{}]分页查询生产配置信息", SecurityUtils.getUsername());
        startPage();
        BuildConfigQuery query = BuildConfigQuery.builder()
                .platformCode(buildConfig.getPlatformCode())
                .seriesCode(buildConfig.getSeriesCode())
                .modelCode(buildConfig.getModelCode())
                .baseModelCode(buildConfig.getBaseModelCode())
                .code(buildConfig.getCode())
                .name(buildConfig.getName())
                .beginTime(getBeginTime(buildConfig))
                .endTime(getEndTime(buildConfig))
                .build();
        List<BuildConfigDto> buildConfigDtoList = buildConfigAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(buildConfigDtoList, MptBuildConfigAssembler.INSTANCE::fromDto)));
    }

    @RequiresPermissions("completeVehicle:product:buildConfig:list")
    @GetMapping(value = "/listByBaseModelCode/{baseModelCode}")
    public ApiResponse<List<BuildConfigResponse>> listByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("管理后台用户[{}]根据基础车型代码[{}]查询生产配置列表", SecurityUtils.getUsername(), baseModelCode);
        List<BuildConfigDto> buildConfigDtoList = buildConfigAppService.getBuildConfigListByBaseModelCode(baseModelCode);
        return ApiResponse.ok(PageUtil.convert(buildConfigDtoList, MptBuildConfigAssembler.INSTANCE::fromDto));
    }

    @RequiresPermissions("completeVehicle:product:buildConfig:list")
    @GetMapping(value = "/{buildConfigCode}/featureCode/list")
    public ApiResponse<List<BuildConfigFeatureCodeResponse>> listFeatureCode(@PathVariable String buildConfigCode, BuildConfigFeatureCodeRequest buildConfigFeatureCode) {
        log.info("管理后台用户[{}]分页查询生产配置下特征值", SecurityUtils.getUsername());
        List<BuildConfigFeatureCodeDto> dtoList = buildConfigAppService.searchFeatureCode(buildConfigCode, buildConfigFeatureCode.getFamilyCode());
        return ApiResponse.ok(MptBuildConfigAssembler.INSTANCE.fromFeatureCodeDtoList(dtoList));
    }

    @Log(title = "生产配置管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:buildConfig:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, BuildConfigRequest buildConfig) {
        log.info("管理后台用户[{}]导出生产配置信息", SecurityUtils.getUsername());
    }

    @RequiresPermissions("completeVehicle:product:buildConfig:query")
    @GetMapping(value = "/{buildConfigId}")
    public ApiResponse<BuildConfigResponse> getInfo(@PathVariable Long buildConfigId) {
        log.info("管理后台用户[{}]根据生产配置ID[{}]获取生产配置信息", SecurityUtils.getUsername(), buildConfigId);
        return ApiResponse.ok(MptBuildConfigAssembler.INSTANCE.fromDto(buildConfigAppService.getBuildConfigById(buildConfigId)));
    }

    @RequiresPermissions("completeVehicle:product:buildConfig:query")
    @GetMapping(value = "/{buildConfigCode}/featureCode/{buildConfigFeatureCodeId}")
    public ApiResponse<BuildConfigFeatureCodeResponse> getFeatureCodeInfo(@PathVariable String buildConfigCode, @PathVariable Long buildConfigFeatureCodeId) {
        log.info("管理后台用户[{}]根据生产配置[{}]特征值ID[{}]获取生产配置特征值信息", SecurityUtils.getUsername(), buildConfigCode, buildConfigFeatureCodeId);
        return ApiResponse.ok(MptBuildConfigAssembler.INSTANCE.fromFeatureCodeDto(buildConfigAppService.getBuildConfigFeatureCodeById(buildConfigFeatureCodeId)));
    }

    @Log(title = "生产配置管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:buildConfig:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody BuildConfigRequest buildConfig) {
        log.info("管理后台用户[{}]新增生产配置信息[{}]", SecurityUtils.getUsername(), buildConfig.getCode());
        if (!buildConfigAppService.checkCodeUnique(buildConfig.getId(), buildConfig.getCode())) {
            return ApiResponse.fail("新增生产配置'" + buildConfig.getCode() + "'失败，生产配置代码已存在");
        }
        buildConfigAppService.createBuildConfig(MptBuildConfigAssembler.INSTANCE.toCmd(buildConfig), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:buildConfig:edit")
    @PostMapping("/{buildConfigCode}/featureCode")
    public ApiResponse<Void> addFeatureCode(@PathVariable String buildConfigCode, @Validated @RequestBody BuildConfigFeatureCodeRequest buildConfigFeatureCode) {
        log.info("管理后台用户[{}]新增生产配置[{}]特征值[{}]", SecurityUtils.getUsername(), buildConfigCode, buildConfigFeatureCode.getFamilyCode());
        if (!buildConfigAppService.checkFeatureCodeUnique(buildConfigFeatureCode.getId(), buildConfigCode, buildConfigFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("新增生产配置特征值'" + buildConfigFeatureCode.getFamilyCode() + "'失败，生产配置特征值已存在");
        }
        buildConfigAppService.createBuildConfigFeatureCode(MptBuildConfigAssembler.INSTANCE.toFeatureCodeCmd(buildConfigFeatureCode));
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:buildConfig:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody BuildConfigRequest buildConfig) {
        log.info("管理后台用户[{}]修改保存生产配置信息[{}]", SecurityUtils.getUsername(), buildConfig.getCode());
        if (!buildConfigAppService.checkCodeUnique(buildConfig.getId(), buildConfig.getCode())) {
            return ApiResponse.fail("修改保存生产配置'" + buildConfig.getCode() + "'失败，生产配置代码已存在");
        }
        buildConfigAppService.modifyBuildConfig(MptBuildConfigAssembler.INSTANCE.toCmd(buildConfig), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:buildConfig:edit")
    @PutMapping("/{buildConfigCode}/featureCode")
    public ApiResponse<Void> editFeatureCode(@PathVariable String buildConfigCode, @Validated @RequestBody BuildConfigFeatureCodeRequest buildConfigFeatureCode) {
        log.info("管理后台用户[{}]修改保存生产配置[{}]特征值[{}]", SecurityUtils.getUsername(), buildConfigCode, buildConfigFeatureCode.getFamilyCode());
        if (!buildConfigAppService.checkFeatureCodeUnique(buildConfigFeatureCode.getId(), buildConfigCode, buildConfigFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("修改保存生产配置特征值'" + buildConfigFeatureCode.getFamilyCode() + "'失败，生产配置特征值已存在");
        }
        buildConfigAppService.modifyBuildConfigFeatureCode(MptBuildConfigAssembler.INSTANCE.toFeatureCodeCmd(buildConfigFeatureCode));
        return ApiResponse.ok();
    }

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

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:buildConfig:edit")
    @DeleteMapping("/{buildConfigCode}/featureCode/{buildConfigFeatureCodeIds}")
    public ApiResponse<Void> removeFeatureCode(@PathVariable String buildConfigCode, @PathVariable Long[] buildConfigFeatureCodeIds) {
        log.info("管理后台用户[{}]删除生产配置[{}]特征值[{}]", SecurityUtils.getUsername(), buildConfigCode, buildConfigFeatureCodeIds);
        return buildConfigAppService.deleteBuildConfigFeatureCodeByIds(buildConfigFeatureCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}