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
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆-零件绑定关系相关管理接口实现类
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
     * 分页查询绑定关系
     *
     * @param vehiclePart 绑定关系
     * @return 绑定关系列表
     */
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehiclePartResponse>> list(VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]分页查询绑定关系", SecurityContextHolder.getUserName());
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
     * 导出绑定关系
     *
     * @param response 响应
     * @param vehiclePart 绑定关系
     */
    @Log(title = "绑定关系管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]导出绑定关系", SecurityContextHolder.getUserName());
    }

    /**
     * 根据绑定关系ID获取绑定关系
     *
     * @param vehiclePartId 绑定关系ID
     * @return 绑定关系
     */
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:query")
    @GetMapping(value = "/{vehiclePartId}")
    public ApiResponse<VehiclePartResponse> getInfo(@PathVariable Long vehiclePartId) {
        log.info("管理后台用户[{}]根据绑定关系ID[{}]获取绑定关系", SecurityContextHolder.getUserName(), vehiclePartId);
        return ApiResponse.ok(MptVehiclePartAssembler.INSTANCE.fromDto(vehiclePartAppService.getVehiclePartById(vehiclePartId)));
    }

    /**
     * 新增绑定关系
     *
     * @param vehiclePart 绑定关系
     * @return 结果
     */
    @Log(title = "绑定关系管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]新增车辆[{}]绑定关系[{}]", SecurityContextHolder.getUserName(), vehiclePart.getVin(), vehiclePart.getPn());
        vehiclePartAppService.createVehiclePart(MptVehiclePartAssembler.INSTANCE.toCmd(vehiclePart), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存绑定关系
     *
     * @param vehiclePart 绑定关系
     * @return 结果
     */
    @Log(title = "绑定关系管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody VehiclePartRequest vehiclePart) {
        log.info("管理后台用户[{}]修改保存车辆[{}]绑定关系[{}]", SecurityContextHolder.getUserName(), vehiclePart.getVin(), vehiclePart.getPn());
        vehiclePartAppService.modifyVehiclePart(MptVehiclePartAssembler.INSTANCE.toCmd(vehiclePart), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除绑定关系
     *
     * @param vehiclePartIds 绑定关系ID数组
     * @return 结果
     */
    @Log(title = "绑定关系管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:vehiclePart:remove")
    @DeleteMapping("/{vehiclePartIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] vehiclePartIds) {
        log.info("管理后台用户[{}]删除绑定关系[{}]", SecurityContextHolder.getUserName(), vehiclePartIds);
        return vehiclePartAppService.deleteVehiclePartByIds(vehiclePartIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
