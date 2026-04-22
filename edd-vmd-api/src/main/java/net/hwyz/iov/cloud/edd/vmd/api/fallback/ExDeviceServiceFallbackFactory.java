package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.api.service.ExDeviceService;
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
public class ExDeviceServiceFallbackFactory implements FallbackFactory<ExDeviceService> {

    @Override
    public ExDeviceService create(Throwable throwable) {
        return new ExDeviceService() {
            @Override
            public DeviceExService getByCode(String code) {
                log.error("设备服务根据设备代码[{}]查询设备信息调用失败", code, throwable);
                return null;
            }

            @Override
            public List<DeviceExService> listAllFota() {
                log.error("设备服务获取所有升级设备信息调用失败", throwable);
                return null;
            }
        };
    }
}
