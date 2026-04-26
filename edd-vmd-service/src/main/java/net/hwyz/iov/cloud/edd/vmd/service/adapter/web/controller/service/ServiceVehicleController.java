package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleExService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleOrderExService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VmdVehicleExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicle/v1")
public class ServiceVehicleController extends BaseController {

    private final VehicleAppService vehicleAppService;

    /**
     * 车辆绑定订单
     *
     * @param vin          车架号
     * @param vehicleOrder 车辆订单
     * @return 结果
     */
    @PostMapping("/{vin}/action/bindOrder")
    public ApiResponse<Void> bindOrder(@PathVariable String vin, @RequestBody @Validated VehicleOrderExService vehicleOrder) {
        log.info("内部服务请求车辆[{}]绑定订单[{}]", vin, vehicleOrder.getOrderNum());
        vehicleAppService.bindOrder(vin, vehicleOrder.getOrderNum());
        return ApiResponse.ok();
    }

    /**
     * 根据车架号查询车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @GetMapping("/{vin}")
    public ApiResponse<VehicleExService> getByVin(@PathVariable String vin) {
        log.info("内部服务请求根据车架号[{}]查询车辆信息", vin);
        return ApiResponse.ok(VmdVehicleExServiceAssembler.INSTANCE.fromDomain(vehicleAppService.getVehicleBasicInfoByVin(vin)));
    }

}
