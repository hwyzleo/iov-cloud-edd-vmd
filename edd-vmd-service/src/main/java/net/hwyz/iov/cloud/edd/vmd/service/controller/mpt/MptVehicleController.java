package net.hwyz.iov.cloud.edd.vmd.service.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleLifecycleVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleLifecycleMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehLifecycleDo;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
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
        List<VehicleVo> vehicleBasicInfoPoList = vehicleAppService.search(vehicle.getVin(), vehicle.getBuildConfigCode(),
                getBeginTime(vehicle), getEndTime(vehicle), null, null);
        return ApiResponse.ok(getPageResult(vehicleBasicInfoPoList));
    }

    /**
     * 分页查询可分配车辆信息
     *
     * @param vehicle 车辆信息
     * @return 车辆信息列表
     */
    @RequiresPermissions("completeVehicle:vehicle:info:list")
    @GetMapping(value = "/listAssignable")
    public ApiResponse<PageResult<VehicleVo>> listAssignable(VehicleVo vehicle) {
        log.info("管理后台用户[{}]分页查询可分配车辆信息", SecurityUtils.getUsername());
        startPage();
        List<VehicleVo> vehicleBasicInfoPoList = vehicleAppService.search(vehicle.getVin(), vehicle.getBuildConfigCode(),
                getBeginTime(vehicle), getEndTime(vehicle), true, false);
        return ApiResponse.ok(getPageResult(vehicleBasicInfoPoList));
    }

    /**
     * 分页查询车辆生命周期
     *
     * @param vin 车辆VIN号
     * @return 车辆生命周期列表
     */
    @RequiresPermissions("completeVehicle:vehicle:info:query")
    @GetMapping(value = "/{vin}/lifecycle")
    public ApiResponse<List<VehicleLifecycleVo>> listLifecycle(@PathVariable String vin) {
        log.info("管理后台用户[{}]分页查询车辆[{}]生命周期", SecurityUtils.getUsername(), vin);
        List<VmdVehLifecycleDo> vehLifecyclePoList = vehicleLifecycleAppService.listLifecycle(vin);
        return ApiResponse.ok(VehicleLifecycleMapper.INSTANCE.fromDoList(vehLifecyclePoList));
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
     * 根据车辆ID获取车辆信息
     *
     * @param vehicleId 车辆ID
     * @return 车辆信息
     */
    @RequiresPermissions("completeVehicle:vehicle:info:query")
    @GetMapping(value = "/{vehicleId}")
    public ApiResponse<VehicleVo> getInfo(@PathVariable Long vehicleId) {
        log.info("管理后台用户[{}]根据车辆ID[{}]获取车辆信息", SecurityUtils.getUsername(), vehicleId);
        VmdVehBasicInfoDo vehBasicInfoPo = vehicleAppService.getVehicleById(vehicleId);
        return ApiResponse.ok(VehicleMapper.INSTANCE.fromDo(vehBasicInfoPo));
    }

    /**
     * 新增车辆信息
     *
     * @param vehicle 车辆信息
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:info:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody VehicleVo vehicle) {
        log.info("管理后台用户[{}]新增车辆信息[{}]", SecurityUtils.getUsername(), vehicle.getVin());
        if (!vehicleAppService.checkVinUnique(vehicle.getId(), vehicle.getVin())) {
            return ApiResponse.fail("新增车辆'" + vehicle.getVin() + "'失败，车辆车架号已存在");
        }
        VmdVehBasicInfoDo vehBasicInfoPo = VehicleMapper.INSTANCE.toDo(vehicle);
        vehBasicInfoPo.setCreateBy(SecurityUtils.getUserId().toString());
        return vehicleAppService.createVehicle(vehBasicInfoPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 新增车辆生命周期
     *
     * @param vin              车架号
     * @param vehicleLifecycle 车辆生命周期
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:info:edit")
    @PostMapping("/{vin}/lifecycle")
    public ApiResponse<Void> addLifecycle(@PathVariable String vin, @Validated @RequestBody VehicleLifecycleVo vehicleLifecycle) {
        log.info("管理后台用户[{}]新增车辆[{}]生命周期[{}]", SecurityUtils.getUsername(), vin, vehicleLifecycle.getNode());
        VmdVehLifecycleDo vehLifecyclePo = VehicleLifecycleMapper.INSTANCE.toDo(vehicleLifecycle);
        vehLifecyclePo.setCreateBy(SecurityUtils.getUserId().toString());
        return vehicleLifecycleAppService.createVehicleLifecycle(vehLifecyclePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存车辆信息
     *
     * @param vehicle 车辆信息
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:info:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody VehicleVo vehicle) {
        log.info("管理后台用户[{}]修改保存车辆信息[{}]", SecurityUtils.getUsername(), vehicle.getVin());
        if (!vehicleAppService.checkVinUnique(vehicle.getId(), vehicle.getVin())) {
            return ApiResponse.fail("修改保存车辆'" + vehicle.getVin() + "'失败，车辆车架号已存在");
        }
        VmdVehBasicInfoDo vehBasicInfoPo = VehicleMapper.INSTANCE.toDo(vehicle);
        vehBasicInfoPo.setModifyBy(SecurityUtils.getUserId().toString());
        return vehicleAppService.modifyVehicle(vehBasicInfoPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存车辆生命周期
     *
     * @param vin              车架号
     * @param vehicleLifecycle 车辆生命周期
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:info:edit")
    @PutMapping("/{vin}/lifecycle")
    public ApiResponse<Void> editLifecycle(@PathVariable String vin, @Validated @RequestBody VehicleLifecycleVo vehicleLifecycle) {
        log.info("管理后台用户[{}]修改保存车辆[{}]生命周期[{}]", SecurityUtils.getUsername(), vin, vehicleLifecycle.getNode());
        VmdVehLifecycleDo vehLifecyclePo = VehicleLifecycleMapper.INSTANCE.toDo(vehicleLifecycle);
        vehLifecyclePo.setModifyBy(SecurityUtils.getUserId().toString());
        return vehicleLifecycleAppService.modifyVehicleLifecycle(vehLifecyclePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
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

    /**
     * 删除车辆生命周期
     *
     * @param vin          车架号
     * @param lifecycleIds 车辆生命周期ID数组
     * @return 结果
     */
    @Log(title = "车辆管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:info:edit")
    @DeleteMapping("/{vin}/lifecycle/{lifecycleIds}")
    public ApiResponse<Void> removeLifecycle(@PathVariable String vin, @PathVariable Long[] lifecycleIds) {
        log.info("管理后台用户[{}]删除车辆[{}]生命周期节点[{}]", SecurityUtils.getUsername(), vin, lifecycleIds);
        return vehicleLifecycleAppService.deleteVehicleLifecycleByIds(lifecycleIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }
}
