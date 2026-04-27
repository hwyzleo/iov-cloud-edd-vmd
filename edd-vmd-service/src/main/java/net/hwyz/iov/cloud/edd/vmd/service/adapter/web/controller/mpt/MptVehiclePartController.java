package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehiclePartRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehiclePartResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆零件相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehiclePart/v1")
public class MptVehiclePartController extends BaseController {

    private final VehiclePartAppService vehiclePartAppService;

    /**
     * 分页查询车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 车辆零件列表
     */
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehiclePartResponse>> list(VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]分页查询车辆零件", SecurityUtils.getUsername());
        startPage();
        VehiclePartQuery query = VehiclePartQuery.builder()
                .vin(vehiclePart.getVin())
                .pn(vehiclePart.getPn())
                .beginTime(getBeginTime(vehiclePart))
                .endTime(getEndTime(vehiclePart))
                .build();
        List<VehiclePartDto> dtoList = vehiclePartAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptVehiclePartAssembler.INSTANCE::fromDto)));
    }

    /**
     * 导出车辆零件
     *
     * @param response    响应
     * @param vehiclePart 车辆零件
     */
    @Log(title = "车辆零件管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]导出车辆零件", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆零件ID获取车辆零件
     *
     * @param vehiclePartId 车辆零件ID
     * @return 车辆零件
     */
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:query")
    @GetMapping(value = "/{vehiclePartId}")
    public ApiResponse<VehiclePartResponse> getInfo(@PathVariable Long vehiclePartId) {
        log.info("管理后台用户[{}]根据车辆零件ID[{}]获取车辆零件", SecurityUtils.getUsername(), vehiclePartId);
        return ApiResponse.ok(MptVehiclePartAssembler.INSTANCE.fromDto(vehiclePartAppService.getVehiclePartById(vehiclePartId)));
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 结果
     */
    @Log(title = "车辆零件管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]新增车辆[{}]零件[{}:{}]", SecurityUtils.getUsername(), vehiclePart.getVin(), vehiclePart.getPn(), vehiclePart.getSn());
        if (!vehiclePartAppService.checkPnAndSnUnique(vehiclePart.getId(), vehiclePart.getPn(), vehiclePart.getSn())) {
            return ApiResponse.fail("新增车辆零件'" + vehiclePart.getPn() + "'失败，车辆零件已存在");
        }
        vehiclePartAppService.createVehiclePart(MptVehiclePartAssembler.INSTANCE.toCmd(vehiclePart), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 结果
     */
    @Log(title = "车辆零件管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]修改保存车辆[{}]零件[{}:{}]", SecurityUtils.getUsername(), vehiclePart.getVin(), vehiclePart.getPn(), vehiclePart.getSn());
        if (!vehiclePartAppService.checkPnAndSnUnique(vehiclePart.getId(), vehiclePart.getPn(), vehiclePart.getSn())) {
            return ApiResponse.fail("修改保存车辆零件'" + vehiclePart.getPn() + "'失败，车辆零件已存在");
        }
        vehiclePartAppService.modifyVehiclePart(MptVehiclePartAssembler.INSTANCE.toCmd(vehiclePart), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车辆零件
     *
     * @param vehiclePartIds 车辆零件ID数组
     * @return 结果
     */
    @Log(title = "车辆零件管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:remove")
    @DeleteMapping("/{vehiclePartIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] vehiclePartIds) {
        log.info("管理后台用户[{}]删除车辆零件[{}]", SecurityUtils.getUsername(), vehiclePartIds);
        return vehiclePartAppService.deleteVehiclePartByIds(vehiclePartIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
