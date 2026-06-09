package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.ServiceConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigurationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CarLineAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleModelConfigAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
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
    private final CarLineAppService carLineAppService;

    /**
     * 根据特征族特征值组合得到匹配的生产配置代码
     *
     * @param featureCodeMap 特征族代码-特征值代码映射
     * @return 生产配置代码
     */
    @GetMapping("/buildConfigCode")
    public String getVehicleBuildConfigCode(@RequestParam Map<String, String> featureCodeMap) {
        log.info("内部服务请求根据特征族特征值[{}]得到匹配的生产配置代码", featureCodeMap);
        return vehicleModelConfigAppService.getVehicleBuildConfigCode(featureCodeMap);
    }

    /**
     * 根据版本代码获取生产配置列表
     *
     * @param variantCode 版本代码
     * @return 生产配置列表
     */
    @GetMapping("/buildConfig/list/{variantCode}")
    public List<VmdBuildConfigResponse> getBuildConfigListByVariantCode(@PathVariable String variantCode) {
        log.info("内部服务请求根据版本代码[{}]获取生产配置列表", variantCode);
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByVariantCode(variantCode);
        return ServiceConfigurationAssembler.INSTANCE.toExResponseList(dtoList);
    }

    /**
     * 根据基础车型代码获取生产配置列表
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
     * 根据生产配置代码获取生产配置详细信息（包含特征值）
     *
     * @param buildConfigCode 生产配置代码
     * @return 生产配置详细信息
     */
    @GetMapping("/buildConfig/{buildConfigCode}")
    public VmdBuildConfigResponse getBuildConfigByCode(@PathVariable String buildConfigCode) {
        log.info("内部服务请求根据生产配置代码[{}]获取生产配置详细信息", buildConfigCode);
        ConfigurationDto configurationDto = configurationAppService.getConfigurationByCode(buildConfigCode);
        List<ConfigurationFeatureCodeDto> featureCodeDtoList = configurationAppService.searchFeatureCode(buildConfigCode, null);

        VmdBuildConfigResponse response = ServiceConfigurationAssembler.INSTANCE.toExResponse(configurationDto);
        response.setFeatureCodes(ServiceConfigurationAssembler.INSTANCE.toFeatureCodeExResponseList(featureCodeDtoList));

        if (configurationDto.getCarLineCode() != null) {
            CarLine carLine = carLineAppService.getSeriesByCode(configurationDto.getCarLineCode());
            if (carLine != null && carLine.getBrandCode() != null) {
                response.setBrandCode(carLine.getBrandCode());
            }
        }

        return response;
    }

}
