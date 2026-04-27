package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.VehicleOrderExRequest;
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
     */
    @PostMapping("/{vin}/action/bindOrder")
    public void bindOrder(@PathVariable String vin, @RequestBody @Validated VehicleOrderExRequest vehicleOrder) {
        log.info("内部服务请求车辆[{}]绑定订单[{}]", vin, vehicleOrder.getOrderNum());
        vehicleAppService.bindOrder(vin, vehicleOrder.getOrderNum());
    }

    /**
     * 根据车架号查询车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @GetMapping("/{vin}")
    public VehicleExResponse getByVin(@PathVariable String vin) {
        log.info("内部服务请求根据车架号[{}]查询车辆信息", vin);
        return VmdVehicleExServiceAssembler.INSTANCE.fromDomain(vehicleAppService.getVehicleBasicInfoByVin(vin));
    }

}
