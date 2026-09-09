package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.ServiceConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigurationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleModelConfigAppService;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 车辆模型配置对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleModelConfig/v1")
public class ServiceVehicleModelConfigController extends BaseController {

    private final VehicleModelConfigAppService vehicleModelConfigAppService;
    private final ConfigurationAppService configurationAppService;

    /**
     * 根据特征族特征值组合得到匹配的生产配置代码
     *
     * @param featureCodeMap 特征族代码-特征值代码映射
     * @return 生产配置代码
     */
    @Deprecated
    @GetMapping("/buildConfigCode")
    public String getVehicleBuildConfigCode(@RequestParam Map<String, String> featureCodeMap) {
        log.info("内部服务请求根据特征族特征值[{}]得到匹配的生产配置代码", featureCodeMap);
        return vehicleModelConfigAppService.getVehicleBuildConfigCode(featureCodeMap);
    }

    /**
     * 按选项族-选项值组合反查配置代码
     * CR-018: 原按特征族特征值反查, 入参键改名
     *
     * @param optionCodes 选项族代码-选项值代码映射
     * @return 配置代码
     */
    @GetMapping("/configurationCode/byOptionCodeMap")
    public String getConfigurationCodeByOptionCodeMap(@RequestParam Map<String, String> optionCodes) {
        log.info("内部服务请求根据选项族选项值[{}]得到匹配的配置代码", optionCodes);
        return vehicleModelConfigAppService.getVehicleBuildConfigCode(optionCodes);
    }

    /**
     * 根据版本代码获取配置列表
     * CR-047：直接按 variant_code 查询并批量补全产品树层级（禁 N+1）
     *
     * @param variantCode 版本代码
     * @return 配置列表
     */
    @GetMapping("/configuration/list/{variantCode}")
    public List<VmdConfigurationResponse> getConfigurationListByVariantCode(@PathVariable String variantCode) {
        log.info("内部服务请求根据版本代码[{}]获取配置列表", variantCode);
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByVariantCode(variantCode);
        return ServiceConfigurationAssembler.INSTANCE.toConfigurationResponseList(dtoList);
    }

    /**
     * 根据基础车型代码获取配置列表（废弃，baseModelCode 语义与 variantCode 一致）
     *
     * @param baseModelCode 基础车型代码
     * @return 配置列表
     */
    @Deprecated
    @GetMapping("/configuration/listByBaseModelCode/{baseModelCode}")
    public List<VmdConfigurationResponse> getConfigurationListByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("内部服务请求根据基础车型代码[{}]获取配置列表", baseModelCode);
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByBaseModelCode(baseModelCode);
        return ServiceConfigurationAssembler.INSTANCE.toConfigurationResponseList(dtoList);
    }

    /**
     * 根据配置代码获取配置详细信息（基础字段 + 选项值 + 产品树补全层级，CR-047 §5.7）
     * 产品树 LEFT JOIN 补全 modelCode/carLineCode/platformCode/brandCode，上层投影缺失时返回 null/省略
     *
     * @param configurationCode 配置代码
     * @return 配置详细信息
     */
    @GetMapping("/configuration/{configurationCode}")
    public VmdConfigurationResponse getConfigurationByCode(@PathVariable String configurationCode) {
        log.info("内部服务请求根据配置代码[{}]获取配置详细信息", configurationCode);
        ConfigurationDto configurationDto = configurationAppService.getConfigurationByCode(configurationCode);
        if (configurationDto == null) {
            log.info("配置[{}]不存在", configurationCode);
            return null;
        }
        List<ConfigurationOptionCodeDto> optionCodeDtoList = configurationAppService.searchOptionCode(configurationCode, null);

        VmdConfigurationResponse response = ServiceConfigurationAssembler.INSTANCE.toConfigurationResponse(configurationDto);
        response.setOptionCodes(ServiceConfigurationAssembler.INSTANCE.toConfigurationOptionCodeResponseList(optionCodeDtoList));
        return response;
    }

    /**
     * 根据版本代码获取生产配置列表（废弃）
     *
     * @param variantCode 版本代码
     * @return 生产配置列表
     */
    @Deprecated
    @GetMapping("/buildConfig/list/{variantCode}")
    public List<VmdBuildConfigResponse> getBuildConfigListByVariantCode(@PathVariable String variantCode) {
        log.info("内部服务请求根据版本代码[{}]获取生产配置列表", variantCode);
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByVariantCode(variantCode);
        return ServiceConfigurationAssembler.INSTANCE.toExResponseList(dtoList);
    }

    /**
     * 根据基础车型代码获取生产配置列表（废弃）
     *
     * @param baseModelCode 基础车型代码
     * @return 生产配置列表
     */
    @Deprecated
    @GetMapping("/buildConfig/listByBaseModelCode/{baseModelCode}")
    public List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("内部服务请求根据基础车型代码[{}]获取生产配置列表", baseModelCode);
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByBaseModelCode(baseModelCode);
        return ServiceConfigurationAssembler.INSTANCE.toExResponseList(dtoList);
    }

    /**
     * 根据生产配置代码获取生产配置详细信息（包含选项值，废弃）
     * CR-047：brandCode 沿产品树派生，不再读取 Configuration 冗余列 / 单独车系回查
     *
     * @param buildConfigCode 生产配置代码
     * @return 生产配置详细信息
     */
    @Deprecated
    @GetMapping("/buildConfig/{buildConfigCode}")
    public VmdBuildConfigResponse getBuildConfigByCode(@PathVariable String buildConfigCode) {
        log.info("内部服务请求根据生产配置代码[{}]获取生产配置详细信息", buildConfigCode);
        ConfigurationDto configurationDto = configurationAppService.getConfigurationByCode(buildConfigCode);
        if (configurationDto == null) {
            log.info("生产配置[{}]不存在", buildConfigCode);
            return null;
        }
        List<ConfigurationOptionCodeDto> optionCodeDtoList = configurationAppService.searchOptionCode(buildConfigCode, null);

        VmdBuildConfigResponse response = ServiceConfigurationAssembler.INSTANCE.toExResponse(configurationDto);
        response.setOptionCodes(ServiceConfigurationAssembler.INSTANCE.toOptionCodeExResponseList(optionCodeDtoList));
        response.setBrandCode(configurationDto.getBrandCode());
        return response;
    }

}
