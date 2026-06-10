package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleNodeExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车载节点对外服务接口实现类
 *
 * <p>CR-020：由 ServiceDeviceController 迁移而来。
 * 车载节点（VehicleNode，原Device设备）对外服务接口。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleNode/v1")
public class ServiceVehicleNodeController extends BaseController {

    private final VehicleNodeAppService vehicleNodeAppService;

    /**
     * 根据车载节点代码查询车载节点信息
     *
     * @param code 车载节点代码
     * @return 车载节点信息
     */
    @GetMapping("/{code}")
    public VehicleNodeExResponse getByCode(@PathVariable String code) {
        log.info("内部服务请求根据车载节点代码[{}]查询车载节点信息", code);
        return VehicleNodeExServiceAssembler.INSTANCE.fromDomain(vehicleNodeAppService.getVehicleNodeByCode(code));
    }

    /**
     * 获取所有FOTA升级车载节点信息
     *
     * @return 车载节点信息列表
     */
    @GetMapping("/listAllFota")
    public List<VehicleNodeExResponse> listAllFota() {
        log.info("内部服务请求获取所有FOTA升级车载节点信息");
        return VehicleNodeExServiceAssembler.INSTANCE.fromDomainList(vehicleNodeAppService.listAllFota());
    }

}
