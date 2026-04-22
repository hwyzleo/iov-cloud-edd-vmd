package net.hwyz.iov.cloud.edd.vmd.service.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.ManufacturerMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.ManufacturerAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehManufacturerDo;
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
 * 车辆工厂相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/manufacturer/v1")
public class MptManufacturerController extends BaseController {

    private final ManufacturerAppService manufacturerAppService;

    /**
     * 分页查询车辆工厂信息
     *
     * @param manufacturer 车辆工厂信息
     * @return 车辆工厂信息列表
     */
    @RequiresPermissions("completeVehicle:product:manufacturer:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ManufacturerVo>> list(ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]分页查询工厂信息", SecurityUtils.getUsername());
        startPage();
        List<ManufacturerVo> manufacturerVoList = manufacturerAppService.search(manufacturer.getCode(), manufacturer.getName(),
                getBeginTime(manufacturer), getEndTime(manufacturer));
        return ApiResponse.ok(getPageResult(manufacturerVoList));
    }

    /**
     * 导出车辆工厂信息
     *
     * @param response     响应
     * @param manufacturer 车辆平台信息
     */
    @Log(title = "工厂管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:manufacturer:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]导出车辆工厂信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆工厂ID获取车辆工厂信息
     *
     * @param manufacturerId 车辆工厂ID
     * @return 车辆工厂信息
     */
    @RequiresPermissions("completeVehicle:product:manufacturer:query")
    @GetMapping(value = "/{manufacturerId}")
    public ApiResponse<ManufacturerVo> getInfo(@PathVariable Long manufacturerId) {
        log.info("管理后台用户[{}]根据车辆工厂ID[{}]获取车辆工厂信息", SecurityUtils.getUsername(), manufacturerId);
        VmdVehManufacturerDo manufacturerPo = manufacturerAppService.getManufacturerById(manufacturerId);
        return ApiResponse.ok(ManufacturerMapper.INSTANCE.fromDo(manufacturerPo));
    }

    /**
     * 新增车辆工厂信息
     *
     * @param manufacturer 车辆工厂信息
     * @return 结果
     */
    @Log(title = "工厂管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:manufacturer:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]新增车辆工厂信息[{}]", SecurityUtils.getUsername(), manufacturer.getCode());
        if (!manufacturerAppService.checkCodeUnique(manufacturer.getId(), manufacturer.getCode())) {
            return ApiResponse.fail("新增车辆工厂'" + manufacturer.getCode() + "'失败，车辆工厂代码已存在");
        }
        VmdVehManufacturerDo manufacturerPo = ManufacturerMapper.INSTANCE.toDo(manufacturer);
        manufacturerPo.setCreateBy(SecurityUtils.getUserId().toString());
        return manufacturerAppService.createManufacturer(manufacturerPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
    }

    /**
     * 修改保存车辆工厂信息
     *
     * @param manufacturer 车辆工厂信息
     * @return 结果
     */
    @Log(title = "工厂管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:manufacturer:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]修改保存车辆工厂信息[{}]", SecurityUtils.getUsername(), manufacturer.getCode());
        if (!manufacturerAppService.checkCodeUnique(manufacturer.getId(), manufacturer.getCode())) {
            return ApiResponse.fail("修改保存车辆工厂'" + manufacturer.getCode() + "'失败，车辆工厂代码已存在");
        }
        VmdVehManufacturerDo manufacturerPo = ManufacturerMapper.INSTANCE.toDo(manufacturer);
        manufacturerPo.setModifyBy(SecurityUtils.getUserId().toString());
        return manufacturerAppService.modifyManufacturer(manufacturerPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
    }

    /**
     * 删除车辆工厂信息
     *
     * @param manufacturerIds 车辆工厂ID数组
     * @return 结果
     */
    @Log(title = "工厂管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:manufacturer:remove")
    @DeleteMapping("/{manufacturerIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] manufacturerIds) {
        log.info("管理后台用户[{}]删除车辆工厂信息[{}]", SecurityUtils.getUsername(), manufacturerIds);
        for (Long manufacturerId : manufacturerIds) {
            if (manufacturerAppService.checkManufacturerVehicleExist(manufacturerId)) {
                return ApiResponse.fail("删除车辆工厂'" + manufacturerId + "'失败，该车辆工厂下存在车辆");
            }
        }
        return manufacturerAppService.deletePlatformByIds(manufacturerIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
