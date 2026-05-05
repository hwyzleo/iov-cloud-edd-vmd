package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleModelConfigServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 车辆车系车型配置相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleModelConfigService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehicleModelConfig/v1", fallbackFactory = VmdVehicleModelConfigServiceFallbackFactory.class)
public interface VmdVehicleModelConfigService {

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
    String getVehicleBuildConfigCode(@RequestParam String baseModelCode, @RequestParam String exteriorCode,
                                     @RequestParam String interiorCode, @RequestParam String wheelCode,
                                     @RequestParam String tireCode, @RequestParam String spareTireCode,
                                     @RequestParam String adasCode, @RequestParam String seatCode);

    /**
     * 根据基础车型代码获取生产配置列表
     *
     * @param baseModelCode 基础车型代码
     * @return 生产配置列表
     */
    @GetMapping("/buildConfig/list/{baseModelCode}")
    List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(@PathVariable String baseModelCode);

    /**
     * 根据生产配置代码获取生产配置详细信息（包含特征值）
     *
     * @param buildConfigCode 生产配置代码
     * @return 生产配置详细信息
     */
    @GetMapping("/buildConfig/{buildConfigCode}")
    VmdBuildConfigResponse getBuildConfigByCode(@PathVariable String buildConfigCode);

}
