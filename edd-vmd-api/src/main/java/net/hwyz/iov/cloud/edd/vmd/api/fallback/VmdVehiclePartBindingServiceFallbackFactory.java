package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehiclePartBindingService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车辆-零件绑定关系服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehiclePartBindingServiceFallbackFactory implements FallbackFactory<VmdVehiclePartBindingService> {

    @Override
    public VmdVehiclePartBindingService create(Throwable throwable) {
        return new VmdVehiclePartBindingService() {
            @Override
            public List<VehiclePartBindingExResponse> listActiveBindingsByVin(String vin) {
                log.error("车辆零件绑定服务根据车架号[{}]查询active绑定调用失败", vin, throwable);
                throw new RuntimeException("车辆零件绑定服务根据车架号[" + vin + "]查询active绑定调用失败", throwable);
            }

            @Override
            public List<VehiclePartBindingExResponse> listBindingsForBootstrap(Long cursor, Integer size) {
                log.error("车辆零件绑定服务全量分页拉取active绑定调用失败, cursor={}, size={}", cursor, size, throwable);
                throw new RuntimeException("车辆零件绑定服务全量分页拉取active绑定调用失败", throwable);
            }
        };
    }
}