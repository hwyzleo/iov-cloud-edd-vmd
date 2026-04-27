package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleModelConfigAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
