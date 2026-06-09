package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigurationQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigurationAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/configuration/v1")
public class MptConfigurationController extends BaseController {

    private final ConfigurationAppService configurationAppService;

    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ConfigurationResponse>> list(ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]分页查询生产配置信息", SecurityContextHolder.getUserName());
        startPage();
        ConfigurationQuery query = ConfigurationQuery.builder()
                .platformCode(configuration.getPlatformCode())
                .carLineCode(configuration.getCarLineCode())
                .modelCode(configuration.getModelCode())
                .variantCode(configuration.getVariantCode())
                .baseModelCode(configuration.getBaseModelCode())
                .code(configuration.getCode())
                .name(configuration.getName())
                .beginTime(getBeginTime(configuration))
                .endTime(getEndTime(configuration))
                .build();
        List<ConfigurationDto> configurationDtoList = configurationAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(configurationDtoList, MptConfigurationAssembler.INSTANCE::fromDto)));
    }

    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/listByVariantCode/{variantCode}")
    public ApiResponse<List<ConfigurationResponse>> listByVariantCode(@PathVariable String variantCode) {
        log.info("管理后台用户[{}]根据版本代码[{}]查询生产配置列表", SecurityContextHolder.getUserName(), variantCode);
        List<ConfigurationDto> configurationDtoList = configurationAppService.getConfigurationListByVariantCode(variantCode);
        return ApiResponse.ok(PageUtil.convert(configurationDtoList, MptConfigurationAssembler.INSTANCE::fromDto));
    }

    @Deprecated
    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/listByBaseModelCode/{baseModelCode}")
    public ApiResponse<List<ConfigurationResponse>> listByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("管理后台用户[{}]根据基础车型代码[{}]查询生产配置列表", SecurityContextHolder.getUserName(), baseModelCode);
        List<ConfigurationDto> configurationDtoList = configurationAppService.getConfigurationListByBaseModelCode(baseModelCode);
        return ApiResponse.ok(PageUtil.convert(configurationDtoList, MptConfigurationAssembler.INSTANCE::fromDto));
    }

    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/{configurationCode}/featureCode/list")
    public ApiResponse<List<ConfigurationFeatureCodeResponse>> listFeatureCode(@PathVariable String configurationCode, ConfigurationFeatureCodeRequest configurationFeatureCode) {
        log.info("管理后台用户[{}]分页查询生产配置下特征值", SecurityContextHolder.getUserName());
        List<ConfigurationFeatureCodeDto> dtoList = configurationAppService.searchFeatureCode(configurationCode, configurationFeatureCode.getFamilyCode());
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromFeatureCodeDtoList(dtoList));
    }

    @Log(title = "生产配置管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:configuration:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]导出生产配置信息", SecurityContextHolder.getUserName());
    }

    @RequiresPermissions("completeVehicle:product:configuration:query")
    @GetMapping(value = "/{configurationId}")
    public ApiResponse<ConfigurationResponse> getInfo(@PathVariable Long configurationId) {
        log.info("管理后台用户[{}]根据生产配置ID[{}]获取生产配置信息", SecurityContextHolder.getUserName(), configurationId);
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromDto(configurationAppService.getConfigurationById(configurationId)));
    }

    @RequiresPermissions("completeVehicle:product:configuration:query")
    @GetMapping(value = "/{configurationCode}/featureCode/{configurationFeatureCodeId}")
    public ApiResponse<ConfigurationFeatureCodeResponse> getFeatureCodeInfo(@PathVariable String configurationCode, @PathVariable Long configurationFeatureCodeId) {
        log.info("管理后台用户[{}]根据生产配置[{}]特征值ID[{}]获取生产配置特征值信息", SecurityContextHolder.getUserName(), configurationCode, configurationFeatureCodeId);
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromFeatureCodeDto(configurationAppService.getConfigurationFeatureCodeById(configurationFeatureCodeId)));
    }

    @Log(title = "生产配置管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:configuration:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]新增生产配置信息[{}]", SecurityContextHolder.getUserName(), configuration.getCode());
        if (!configurationAppService.checkCodeUnique(configuration.getId(), configuration.getCode())) {
            return ApiResponse.fail("新增生产配置'" + configuration.getCode() + "'失败，生产配置代码已存在");
        }
        configurationAppService.createConfiguration(MptConfigurationAssembler.INSTANCE.toCmd(configuration), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PostMapping("/{configurationCode}/featureCode")
    public ApiResponse<Void> addFeatureCode(@PathVariable String configurationCode, @Validated @RequestBody ConfigurationFeatureCodeRequest configurationFeatureCode) {
        log.info("管理后台用户[{}]新增生产配置[{}]特征值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationFeatureCode.getFamilyCode());
        if (!configurationAppService.checkFeatureCodeUnique(configurationFeatureCode.getId(), configurationCode, configurationFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("新增生产配置特征值'" + configurationFeatureCode.getFamilyCode() + "'失败，生产配置特征值已存在");
        }
        configurationAppService.createConfigurationFeatureCode(MptConfigurationAssembler.INSTANCE.toFeatureCodeCmd(configurationFeatureCode));
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]修改保存生产配置信息[{}]", SecurityContextHolder.getUserName(), configuration.getCode());
        if (!configurationAppService.checkCodeUnique(configuration.getId(), configuration.getCode())) {
            return ApiResponse.fail("修改保存生产配置'" + configuration.getCode() + "'失败，生产配置代码已存在");
        }
        configurationAppService.modifyConfiguration(MptConfigurationAssembler.INSTANCE.toCmd(configuration), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PutMapping("/{configurationCode}/featureCode")
    public ApiResponse<Void> editFeatureCode(@PathVariable String configurationCode, @Validated @RequestBody ConfigurationFeatureCodeRequest configurationFeatureCode) {
        log.info("管理后台用户[{}]修改保存生产配置[{}]特征值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationFeatureCode.getFamilyCode());
        if (!configurationAppService.checkFeatureCodeUnique(configurationFeatureCode.getId(), configurationCode, configurationFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("修改保存生产配置特征值'" + configurationFeatureCode.getFamilyCode() + "'失败，生产配置特征值已存在");
        }
        configurationAppService.modifyConfigurationFeatureCode(MptConfigurationAssembler.INSTANCE.toFeatureCodeCmd(configurationFeatureCode));
        return ApiResponse.ok();
    }

    @Log(title = "生产配置管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:configuration:remove")
    @DeleteMapping("/{configurationIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] configurationIds) {
        log.info("管理后台用户[{}]删除生产配置信息[{}]", SecurityContextHolder.getUserName(), configurationIds);
        for (Long configurationId : configurationIds) {
            if (configurationAppService.checkConfigurationVehicleExist(configurationId)) {
                return ApiResponse.fail("删除生产配置'" + configurationId + "'失败，该生产配置下存在车辆");
            }
        }
        return configurationAppService.deleteConfigurationByIds(configurationIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    @Log(title = "生产配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @DeleteMapping("/{configurationCode}/featureCode/{configurationFeatureCodeIds}")
    public ApiResponse<Void> removeFeatureCode(@PathVariable String configurationCode, @PathVariable Long[] configurationFeatureCodeIds) {
        log.info("管理后台用户[{}]删除生产配置[{}]特征值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationFeatureCodeIds);
        return configurationAppService.deleteConfigurationFeatureCodeByIds(configurationFeatureCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
