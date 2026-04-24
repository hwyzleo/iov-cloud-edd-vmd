package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleVoAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicle/v1")
public class MptVehicleController extends BaseController {

    private final VehicleAppService vehicleAppService;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 分页查询车辆信息
     *
     * @param vehicle 车辆信息
     * @return 车辆信息列表
     */
    @RequiresPermissions("completeVehicle:vehicle:info:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehicleVo>> list(VehicleVo vehicle) {
        log.info("管理后台用户[{}]分页查询车辆信息", SecurityUtils.getUsername());
        startPage();
        List<VehicleVo> vehicleVoList = vehicleAppService.search(vehicle.getVin(), vehicle.getBuildConfigCode(),
                getBeginTime(vehicle), getEndTime(vehicle), null, null);
        return ApiResponse.ok(getPageResult(vehicleVoList));
    }

    /**
     * 根据车架号获取车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @RequiresPermissions("completeVehicle:vehicle:info:query")
    @GetMapping(value = "/vin/{vin}")
    public ApiResponse<VehicleVo> getInfoByVin(@PathVariable String vin) {
        log.info("管理后台用户[{}]根据车架号[{}]获取车辆信息", SecurityUtils.getUsername(), vin);
        VehicleDto vehicleDto = vehicleAppService.getVehicleByVin(vin);
        return ApiResponse.ok(VehicleVoAssembler.INSTANCE.fromDto(vehicleDto));
    }

    /**
     * 导出车辆信息
     *
     * @param response 响应
     * @param vehicle  车辆信息
     */
    @Log(title = "车辆管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:info:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehicleVo vehicle) {
        log.info("管理后台用户[{}]导出车辆信息", SecurityUtils.getUsername());
    }

    /**
     * 删除车辆信息
     *
     * @param vehicleIds 车辆ID数组
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:info:remove")
    @DeleteMapping("/{vehicleIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] vehicleIds) {
        log.info("管理后台用户[{}]删除车辆信息[{}]", SecurityUtils.getUsername(), vehicleIds);
        return vehicleAppService.deleteVehicleByIds(vehicleIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }
}
