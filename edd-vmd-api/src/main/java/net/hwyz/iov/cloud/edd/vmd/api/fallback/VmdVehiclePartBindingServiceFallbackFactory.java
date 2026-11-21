package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
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

            @Override
            public VehiclePartBindingByDeviceExResponse getVehicleByDevice(String deviceId, String deviceCategory) {
                // 脱敏：不输出完整设备标识（CR-076 §7）
                log.error("车辆零件绑定服务按设备标识[{}]类别[{}]查询当前绑定车辆调用失败",
                        maskDeviceId(deviceId), deviceCategory, throwable);
                throw new RuntimeException("车辆零件绑定服务按设备标识查询当前绑定车辆调用失败", throwable);
            }
        };
    }

    /**
     * 设备标识脱敏（保留前 4 与后 4 位）
     *
     * @param deviceId 设备标识
     * @return 脱敏后的设备标识
     */
    private static String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() <= 8) {
            return "****";
        }
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
    }
}