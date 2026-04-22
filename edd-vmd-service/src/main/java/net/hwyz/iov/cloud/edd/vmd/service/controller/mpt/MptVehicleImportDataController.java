package net.hwyz.iov.cloud.edd.vmd.service.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehImportDataDo;
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
 * 车辆导入数据相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicleImportData/v1")
public class MptVehicleImportDataController extends BaseController {

    private final VehicleImportDataAppService vehicleImportDataAppService;

    /**
     * 分页查询车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 车辆导入数据列表
     */
    @RequiresPermissions("completeVehicle:vehicle:importData:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehicleImportDataVo>> list(VehicleImportDataVo vehicleImportData) {
        log.info("管理后台用户[{}]分页查询车辆导入数据", SecurityUtils.getUsername());
        startPage();
        List<VehicleImportDataVo> vehicleImportDataVoList = vehicleImportDataAppService.search(vehicleImportData.getBatchNum(),
                vehicleImportData.getType(), vehicleImportData.getVersion(), getBeginTime(vehicleImportData), getEndTime(vehicleImportData));
        return ApiResponse.ok(getPageResult(vehicleImportDataVoList));
    }

    /**
     * 导出车辆导入数据
     *
     * @param response          响应
     * @param vehicleImportData 车辆导入数据
     */
    @Log(title = "车辆导入数据管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:importData:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehicleImportDataVo vehicleImportData) {
        log.info("管理后台用户[{}]导出车辆导入数据", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆导入数据ID获取车辆导入数据
     *
     * @param vehicleImportDataId 车辆导入数据ID
     * @return 车辆导入数据
     */
    @RequiresPermissions("completeVehicle:vehicle:importData:query")
    @GetMapping(value = "/{vehicleImportDataId}")
    public ApiResponse<VehicleImportDataVo> getInfo(@PathVariable Long vehicleImportDataId) {
        log.info("管理后台用户[{}]根据车辆导入数据ID[{}]获取车辆导入数据", SecurityUtils.getUsername(), vehicleImportDataId);
        VmdVehImportDataDo vehImportDataPo = vehicleImportDataAppService.getVehicleImportDataById(vehicleImportDataId);
        return ApiResponse.ok(VehicleImportDataMapper.INSTANCE.fromDo(vehImportDataPo));
    }

    /**
     * 新增车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 结果
     */
    @Log(title = "车辆导入数据管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:importData:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody VehicleImportDataVo vehicleImportData) {
        log.info("管理后台用户[{}]新增车辆导入数据[{}]", SecurityUtils.getUsername(), vehicleImportData.getBatchNum());
        if (!vehicleImportDataAppService.checkBatchNumUnique(vehicleImportData.getId(), vehicleImportData.getBatchNum())) {
            return ApiResponse.fail("新增车辆导入数据'" + vehicleImportData.getBatchNum() + "'失败，批次号已存在");
        }
        VmdVehImportDataDo vehImportDataPo = VehicleImportDataMapper.INSTANCE.toDo(vehicleImportData);
        vehImportDataPo.setCreateBy(SecurityUtils.getUserId().toString());
        ApiResponse<Void> result = vehicleImportDataAppService.createVehicleImportData(vehImportDataPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
        try {
            vehicleImportDataAppService.parseVehicleImportData(vehImportDataPo.getBatchNum());
        } catch (Exception e) {
            return ApiResponse.fail("车辆导入数据'" + vehicleImportData.getBatchNum() + "'解析异常");
        }
        return result;
    }

    /**
     * 修改保存车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 结果
     */
    @Log(title = "车辆导入数据管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:importData:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody VehicleImportDataVo vehicleImportData) {
        log.info("管理后台用户[{}]修改保存车辆导入数据[{}]", SecurityUtils.getUsername(), vehicleImportData.getBatchNum());
        if (!vehicleImportDataAppService.checkBatchNumUnique(vehicleImportData.getId(), vehicleImportData.getBatchNum())) {
            return ApiResponse.fail("修改保存车辆导入数据'" + vehicleImportData.getBatchNum() + "'失败，批次号已存在");
        }
        VmdVehImportDataDo vehImportDataPo = VehicleImportDataMapper.INSTANCE.toDo(vehicleImportData);
        vehImportDataPo.setModifyBy(SecurityUtils.getUserId().toString());
        ApiResponse<Void> result = vehicleImportDataAppService.modifyVehicleImportData(vehImportDataPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
        try {
            vehicleImportDataAppService.parseVehicleImportData(vehImportDataPo.getBatchNum());
        } catch (Exception e) {
            return ApiResponse.fail("车辆导入数据'" + vehicleImportData.getBatchNum() + "'解析异常");
        }
        return result;
    }

    /**
     * 删除车辆导入数据
     *
     * @param vehicleImportDataIds 车辆导入数据ID数组
     * @return 结果
     */
    @Log(title = "车辆导入数据管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:importData:remove")
    @DeleteMapping("/{vehicleImportDataIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] vehicleImportDataIds) {
        log.info("管理后台用户[{}]删除车辆导入数据[{}]", SecurityUtils.getUsername(), vehicleImportDataIds);
        return vehicleImportDataAppService.deleteVehicleImportDataByIds(vehicleImportDataIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }
}
