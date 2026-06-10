package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleNodeServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 车载节点相关服务接口
 *
 * <p>CR-020：由 VmdDeviceService 迁移而来。
 * 车载节点（VehicleNode，原Device设备）对外 Feign 契约。</p>
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "vmdVehicleNodeService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehicleNode/v1", fallbackFactory = VmdVehicleNodeServiceFallbackFactory.class)
public interface VmdVehicleNodeService {

    /**
     * 根据车载节点代码查询车载节点信息
     *
     * @param code 车载节点代码
     * @return 车载节点信息
     */
    @GetMapping("/{code}")
    VehicleNodeExResponse getByCode(@PathVariable String code);

    /**
     * 获取所有FOTA升级车载节点信息
     *
     * @return 车载节点信息列表
     */
    @GetMapping("/listAllFota")
    List<VehicleNodeExResponse> listAllFota();

}
