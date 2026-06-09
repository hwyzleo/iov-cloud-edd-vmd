package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationOptionCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationOptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
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
        log.info("管理后台用户[{}]分页查询配置信息", SecurityContextHolder.getUserName());
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
        log.info("管理后台用户[{}]根据版本代码[{}]查询配置列表", SecurityContextHolder.getUserName(), variantCode);
        List<ConfigurationDto> configurationDtoList = configurationAppService.getConfigurationListByVariantCode(variantCode);
        return ApiResponse.ok(PageUtil.convert(configurationDtoList, MptConfigurationAssembler.INSTANCE::fromDto));
    }

    @Deprecated
    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/listByBaseModelCode/{baseModelCode}")
    public ApiResponse<List<ConfigurationResponse>> listByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("管理后台用户[{}]根据基础车型代码[{}]查询配置列表", SecurityContextHolder.getUserName(), baseModelCode);
        List<ConfigurationDto> configurationDtoList = configurationAppService.getConfigurationListByBaseModelCode(baseModelCode);
        return ApiResponse.ok(PageUtil.convert(configurationDtoList, MptConfigurationAssembler.INSTANCE::fromDto));
    }

    @RequiresPermissions("completeVehicle:product:configuration:list")
    @GetMapping(value = "/{configurationCode}/optionCode/list")
    public ApiResponse<List<ConfigurationOptionCodeResponse>> listOptionCode(@PathVariable String configurationCode, ConfigurationOptionCodeRequest configurationOptionCode) {
        log.info("管理后台用户[{}]分页查询配置下选项值", SecurityContextHolder.getUserName());
        List<ConfigurationOptionCodeDto> dtoList = configurationAppService.searchOptionCode(configurationCode, configurationOptionCode.getOptionFamilyCode());
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromOptionCodeDtoList(dtoList));
    }

    @Log(title = "配置管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:configuration:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]导出配置信息", SecurityContextHolder.getUserName());
    }

    @RequiresPermissions("completeVehicle:product:configuration:query")
    @GetMapping(value = "/{configurationId}")
    public ApiResponse<ConfigurationResponse> getInfo(@PathVariable Long configurationId) {
        log.info("管理后台用户[{}]根据配置ID[{}]获取配置信息", SecurityContextHolder.getUserName(), configurationId);
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromDto(configurationAppService.getConfigurationById(configurationId)));
    }

    @RequiresPermissions("completeVehicle:product:configuration:query")
    @GetMapping(value = "/{configurationCode}/optionCode/{configurationOptionCodeId}")
    public ApiResponse<ConfigurationOptionCodeResponse> getOptionCodeInfo(@PathVariable String configurationCode, @PathVariable Long configurationOptionCodeId) {
        log.info("管理后台用户[{}]根据配置[{}]选项值ID[{}]获取配置选项值信息", SecurityContextHolder.getUserName(), configurationCode, configurationOptionCodeId);
        return ApiResponse.ok(MptConfigurationAssembler.INSTANCE.fromOptionCodeDto(configurationAppService.getConfigurationOptionCodeById(configurationOptionCodeId)));
    }

    @Log(title = "配置管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:configuration:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]新增配置信息[{}]", SecurityContextHolder.getUserName(), configuration.getCode());
        if (!configurationAppService.checkCodeUnique(configuration.getId(), configuration.getCode())) {
            return ApiResponse.fail("新增配置'" + configuration.getCode() + "'失败，配置代码已存在");
        }
        configurationAppService.createConfiguration(MptConfigurationAssembler.INSTANCE.toCmd(configuration), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PostMapping("/{configurationCode}/optionCode")
    public ApiResponse<Void> addOptionCode(@PathVariable String configurationCode, @Validated @RequestBody ConfigurationOptionCodeRequest configurationOptionCode) {
        log.info("管理后台用户[{}]新增配置[{}]选项值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationOptionCode.getOptionFamilyCode());
        if (!configurationAppService.checkOptionCodeUnique(configurationOptionCode.getId(), configurationCode, configurationOptionCode.getOptionFamilyCode())) {
            return ApiResponse.fail("新增配置选项值'" + configurationOptionCode.getOptionFamilyCode() + "'失败，配置选项值已存在");
        }
        configurationAppService.createConfigurationOptionCode(MptConfigurationAssembler.INSTANCE.toOptionCodeCmd(configurationOptionCode));
        return ApiResponse.ok();
    }

    @Log(title = "配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ConfigurationRequest configuration) {
        log.info("管理后台用户[{}]修改保存配置信息[{}]", SecurityContextHolder.getUserName(), configuration.getCode());
        if (!configurationAppService.checkCodeUnique(configuration.getId(), configuration.getCode())) {
            return ApiResponse.fail("修改保存配置'" + configuration.getCode() + "'失败，配置代码已存在");
        }
        configurationAppService.modifyConfiguration(MptConfigurationAssembler.INSTANCE.toCmd(configuration), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    @Log(title = "配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @PutMapping("/{configurationCode}/optionCode")
    public ApiResponse<Void> editOptionCode(@PathVariable String configurationCode, @Validated @RequestBody ConfigurationOptionCodeRequest configurationOptionCode) {
        log.info("管理后台用户[{}]修改保存配置[{}]选项值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationOptionCode.getOptionFamilyCode());
        if (!configurationAppService.checkOptionCodeUnique(configurationOptionCode.getId(), configurationCode, configurationOptionCode.getOptionFamilyCode())) {
            return ApiResponse.fail("修改保存配置选项值'" + configurationOptionCode.getOptionFamilyCode() + "'失败，配置选项值已存在");
        }
        configurationAppService.modifyConfigurationOptionCode(MptConfigurationAssembler.INSTANCE.toOptionCodeCmd(configurationOptionCode));
        return ApiResponse.ok();
    }

    @Log(title = "配置管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:configuration:remove")
    @DeleteMapping("/{configurationIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] configurationIds) {
        log.info("管理后台用户[{}]删除配置信息[{}]", SecurityContextHolder.getUserName(), configurationIds);
        for (Long configurationId : configurationIds) {
            if (configurationAppService.checkConfigurationVehicleExist(configurationId)) {
                return ApiResponse.fail("删除配置'" + configurationId + "'失败，该配置下存在车辆");
            }
        }
        return configurationAppService.deleteConfigurationByIds(configurationIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    @Log(title = "配置管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configuration:edit")
    @DeleteMapping("/{configurationCode}/optionCode/{configurationOptionCodeIds}")
    public ApiResponse<Void> removeOptionCode(@PathVariable String configurationCode, @PathVariable Long[] configurationOptionCodeIds) {
        log.info("管理后台用户[{}]删除配置[{}]选项值[{}]", SecurityContextHolder.getUserName(), configurationCode, configurationOptionCodeIds);
        return configurationAppService.deleteConfigurationOptionCodeByIds(configurationOptionCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
