package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.ServiceBuildConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BuildConfigAppService;
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
    private final BuildConfigAppService buildConfigAppService;

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
     * 根据基础车型代码获取生产配置列表
     *
     * @param baseModelCode 基础车型代码
     * @return 生产配置列表
     */
    @GetMapping("/buildConfig/list/{baseModelCode}")
    public List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(@PathVariable String baseModelCode) {
        log.info("内部服务请求根据基础车型代码[{}]获取生产配置列表", baseModelCode);
        List<BuildConfigDto> dtoList = buildConfigAppService.getBuildConfigListByBaseModelCode(baseModelCode);
        return ServiceBuildConfigAssembler.INSTANCE.toExResponseList(dtoList);
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
        BuildConfigDto buildConfigDto = buildConfigAppService.getBuildConfigByCode(buildConfigCode);
        List<BuildConfigFeatureCodeDto> featureCodeDtoList = buildConfigAppService.searchFeatureCode(buildConfigCode, null);

        VmdBuildConfigResponse response = ServiceBuildConfigAssembler.INSTANCE.toExResponse(buildConfigDto);
        response.setFeatureCodes(ServiceBuildConfigAssembler.INSTANCE.toFeatureCodeExResponseList(featureCodeDtoList));

        return response;
    }

}
