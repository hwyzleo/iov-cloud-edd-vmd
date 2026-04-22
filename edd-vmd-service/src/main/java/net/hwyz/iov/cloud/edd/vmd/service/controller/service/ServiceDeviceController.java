package net.hwyz.iov.cloud.edd.vmd.service.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.service.application.DeviceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/device/v1")
public class ServiceDeviceController {

    private final DeviceAppService deviceAppService;

    /**
     * 根据设备代码查询设备信息
     *
     * @param code 设备代码
     * @return 设备信息
     */
    @GetMapping("/{code}")
    public DeviceExService getByCode(@PathVariable String code) {
        log.info("根据设备代码[{}]查询设备信息", code);
        return DeviceExServiceMapper.INSTANCE.fromDo(deviceAppService.getDeviceByCode(code));
    }

    /**
     * 获取所有升级设备信息
     *
     * @return 设备信息列表
     */
    @GetMapping("/listAllFota")
    public List<DeviceExService> listAllFota() {
        log.info("获取所有升级设备信息");
        return DeviceExServiceMapper.INSTANCE.fromDoList(deviceAppService.listAllFota());
    }

}
