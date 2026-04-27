package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.DeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.DeviceExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DeviceAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/device/v1")
public class ServiceDeviceController extends BaseController {

    private final DeviceAppService deviceAppService;

    /**
     * 根据设备代码查询设备信息
     *
     * @param code 设备代码
     * @return 设备信息
     */
    @GetMapping("/{code}")
    public DeviceExResponse getByCode(@PathVariable String code) {
        log.info("内部服务请求根据设备代码[{}]查询设备信息", code);
        return DeviceExServiceAssembler.INSTANCE.fromDomain(deviceAppService.getDeviceByCode(code));
    }

    /**
     * 获取所有升级设备信息
     *
     * @return 设备信息列表
     */
    @GetMapping("/listAllFota")
    public List<DeviceExResponse> listAllFota() {
        log.info("内部服务请求获取所有升级设备信息");
        return DeviceExServiceAssembler.INSTANCE.fromDomainList(deviceAppService.listAllFota());
    }

}
