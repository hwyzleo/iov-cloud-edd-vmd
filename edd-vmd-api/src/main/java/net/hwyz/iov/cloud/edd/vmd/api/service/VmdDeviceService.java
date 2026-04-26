package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdDeviceServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 设备相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exDeviceService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/device/v1", fallbackFactory = VmdDeviceServiceFallbackFactory.class)
public interface VmdDeviceService {

    /**
     * 根据设备代码查询设备信息
     *
     * @param code 设备代码
     * @return 设备信息
     */
    @GetMapping("/{code}")
    DeviceExService getByCode(@PathVariable String code);

    /**
     * 获取所有升级设备信息
     *
     * @return 设备信息列表
     */
    @GetMapping("/listAllFota")
    List<DeviceExService> listAllFota();

}
