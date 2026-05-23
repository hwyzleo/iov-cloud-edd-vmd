package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆生命周期对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleLifecycle/v1")
public class ServiceVehicleLifecycleController extends BaseController {

    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 记录第一次申请节点
     *
     * @param vin      车架号
     * @param nodeCode 节点编码
     */
    @PostMapping("/{vin}/recordFirstApplyNode")
    public void recordFirstApplyNode(@PathVariable String vin, @RequestParam String nodeCode) {
        log.info("内部服务请求记录车辆[{}]第一次申请节点[{}]", vin, nodeCode);
        vehicleLifecycleAppService.recordFirstApplyNode(vin, nodeCode);
    }

}
