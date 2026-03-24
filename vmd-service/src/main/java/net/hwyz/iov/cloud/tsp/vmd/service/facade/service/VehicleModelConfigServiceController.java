package net.hwyz.iov.cloud.tsp.vmd.service.facade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.tsp.vmd.service.application.service.VehicleModelConfigAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆车系车型配置相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/vehicleModelConfig")
public class VehicleModelConfigServiceController {

    private final VehicleModelConfigAppService vehicleModelConfigAppService;

    /**
     * 根据车型配置类型得到匹配的车型配置代码
     *
     * @param baseModelCode 基础车型代码
     * @param exteriorCode  外饰代码
     * @param interiorCode  内饰代码
     * @param wheelCode     轮毂代码
     * @param tireCode      轮胎代码
     * @param spareTireCode 备胎代码
     * @param adasCode      智驾代码
     * @param seatCode      座椅代码
     * @return 车型配置代码
     */
    @GetMapping("/buildConfigCode")
    public String getVehicleBuildConfigCode(@RequestParam String baseModelCode, @RequestParam String exteriorCode,
                                            @RequestParam String interiorCode, @RequestParam String wheelCode,
                                            @RequestParam String tireCode, @RequestParam String spareTireCode,
                                            @RequestParam String adasCode, @RequestParam String seatCode) {
        logger.info("根据车型配置类型[{}:{}:{}:{}:{}:{}:{}:{}]得到匹配的车型配置代码", baseModelCode, exteriorCode, interiorCode,
                wheelCode, tireCode, spareTireCode, adasCode, seatCode);
        return vehicleModelConfigAppService.getBuildConfigCodeByType(baseModelCode, exteriorCode, interiorCode, wheelCode,
                tireCode, spareTireCode, adasCode, seatCode);
    }
}
