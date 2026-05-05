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
     * 根据车型配置类型得到匹配的生产配置代码
     *
     * @param baseModelCode 基础车型代码
     * @param exteriorCode  外饰代码
     * @param interiorCode  内饰代码
     * @param wheelCode     轮毂代码
     * @param tireCode      轮胎代码
     * @param spareTireCode 备胎代码
     * @param adasCode      智驾代码
     * @param seatCode      座椅代码
     * @return 生产配置代码
     */
    @GetMapping("/buildConfigCode")
    public String getVehicleBuildConfigCode(@RequestParam String baseModelCode, @RequestParam String exteriorCode,
                                            @RequestParam String interiorCode, @RequestParam String wheelCode,
                                            @RequestParam String tireCode, @RequestParam String spareTireCode,
                                            @RequestParam String adasCode, @RequestParam String seatCode) {
        log.info("内部服务请求根据车型配置类型得到匹配的生产配置代码");
        return vehicleModelConfigAppService.getVehicleBuildConfigCode(baseModelCode, exteriorCode, interiorCode,
                wheelCode, tireCode, spareTireCode, adasCode, seatCode);
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
