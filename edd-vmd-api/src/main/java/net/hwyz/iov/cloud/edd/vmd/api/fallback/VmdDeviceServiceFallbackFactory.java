package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.DeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdDeviceService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdDeviceServiceFallbackFactory implements FallbackFactory<VmdDeviceService> {

    @Override
    public VmdDeviceService create(Throwable throwable) {
        return new VmdDeviceService() {
            @Override
            public DeviceExResponse getByCode(String code) {
                log.error("设备服务根据设备代码[{}]查询设备信息调用失败", code, throwable);
                return null;
            }

            @Override
            public List<DeviceExResponse> listAllFota() {
                log.error("设备服务获取所有升级设备信息调用失败", throwable);
                return null;
            }
        };
    }
}
