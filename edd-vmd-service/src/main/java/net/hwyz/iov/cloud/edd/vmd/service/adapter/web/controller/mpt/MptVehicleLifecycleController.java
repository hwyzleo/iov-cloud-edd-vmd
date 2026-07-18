package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehicleLifecycleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleLifecycleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆生命周期管理接口实现类（MPT 端）
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicleLifecycle/v1")
public class MptVehicleLifecycleController extends BaseController {

    private final VehicleAppService vehicleAppService;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 查询车辆生命周期时间线
     *
     * @param vin 车架号
     * @return 车辆生命周期节点时间线列表（按 reachTime 升序，空值末尾）
     */
    @RequiresPermissions("vmd:vehicle:lifecycle:query")
    @GetMapping("/{vin}/timeline")
    public ApiResponse<List<VehicleLifecycleNodeResponse>> getTimeline(@PathVariable String vin) {
        log.info("管理后台用户[{}]查询车辆[{}]生命周期时间线", SecurityContextHolder.getUserName(), vin);
        vehicleAppService.checkVinExists(vin);
        List<VehicleLifecycleNode> nodes = vehicleLifecycleAppService.getVehicleTimelineByVin(vin);
        return ApiResponse.ok(MptVehicleLifecycleAssembler.INSTANCE.toTimelineResponseList(nodes));
    }

}
