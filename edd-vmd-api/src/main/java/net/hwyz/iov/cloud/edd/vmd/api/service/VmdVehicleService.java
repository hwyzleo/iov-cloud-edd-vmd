package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.VehicleOrderExRequest;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车辆相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehicle/v1", fallbackFactory = VmdVehicleServiceFallbackFactory.class)
public interface VmdVehicleService {

    /**
     * 车辆绑定订单
     *
     * @param vin          车架号
     * @param vehicleOrder 车辆订单
     */
    @PostMapping("/{vin}/action/bindOrder")
    void bindOrder(@PathVariable String vin, @RequestBody @Validated VehicleOrderExRequest vehicleOrder);

    /**
     * 根据车架号查询车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @GetMapping("/{vin}")
    VehicleExResponse getByVin(@PathVariable String vin);

}
