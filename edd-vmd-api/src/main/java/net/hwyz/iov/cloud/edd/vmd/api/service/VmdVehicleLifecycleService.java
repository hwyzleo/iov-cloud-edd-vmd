package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleLifecycleServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车辆生命周期相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleLifecycleService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehicleLifecycle/v1", fallbackFactory = VmdVehicleLifecycleServiceFallbackFactory.class)
public interface VmdVehicleLifecycleService {

    /**
     * 记录第一次申请节点
     *
     * @param vin      车架号
     * @param nodeCode 节点编码
     */
    @PostMapping("/{vin}/recordFirstApplyNode")
    void recordFirstApplyNode(@PathVariable String vin, @RequestParam String nodeCode);

}
