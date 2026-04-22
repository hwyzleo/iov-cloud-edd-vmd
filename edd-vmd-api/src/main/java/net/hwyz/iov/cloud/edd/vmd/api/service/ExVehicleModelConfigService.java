package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.ExVehicleModelConfigServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车辆车系车型配置相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleModelConfigService", value = ServiceNameConstants.TSP_VMD, path = "/service/vehicleModelConfig", fallbackFactory = ExVehicleModelConfigServiceFallbackFactory.class)
public interface ExVehicleModelConfigService {

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

}
