package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleExService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleOrderExService;
import net.hwyz.iov.cloud.edd.vmd.api.service.ExVehicleService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ExVehicleServiceFallbackFactory implements FallbackFactory<ExVehicleService> {

    @Override
    public ExVehicleService create(Throwable throwable) {
        return new ExVehicleService() {
            @Override
            public void bindOrder(String vin, VehicleOrderExService vehicleOrder) {
                log.error("车辆服务车辆[{}]绑定订单[{}]调用失败", vin, vehicleOrder.getOrderNum(), throwable);
            }

            @Override
            public VehicleExService getByVin(String vin) {
                log.error("车辆服务根据车架号[{}]查询车辆信息调用失败", vin, throwable);
                return null;
            }
        };
    }
}
