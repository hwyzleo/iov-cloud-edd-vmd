package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DeviceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DevicePo;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备信息相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/device/v1")
public class MptDeviceController extends BaseController {

    private final DeviceAppService deviceAppService;

    /**
     * 分页查询设备信息
     *
     * @param device 设备信息
     * @return 设备信息列表
     */
    @RequiresPermissions("completeVehicle:vehicle:device:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<DeviceVo>> list(DeviceVo device) {
        log.info("管理后台用户[{}]分页查询设备信息", SecurityUtils.getUsername());
        startPage();
        List<DeviceVo> deviceVoList = deviceAppService.search(device.getCode(), device.getName(), device.getFuncDomain(),
                getBeginTime(device), getEndTime(device));
        return ApiResponse.ok(getPageResult(deviceVoList));
    }

    /**
     * 获取所有设备项
     *
     * @return 设备类型列表
     */
    @RequiresPermissions("completeVehicle:vehicle:device:list")
    @GetMapping(value = "/listAllDeviceItem")
    public ApiResponse listAllDeviceItem() {
        log.info("管理后台用户[{}]获取所有设备项", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (DeviceItem deviceItem : DeviceItem.values()) {
            list.add(Map.of("code", deviceItem.name(), "label", deviceItem.label));
        }
        return ApiResponse.ok(list);
    }

    /**
     * 获取所有设备
     *
     * @return 设备列表
     */
    @RequiresPermissions("completeVehicle:vehicle:device:list")
    @GetMapping(value = "/listAllDevice")
    public ApiResponse listAllDevice() {
        log.info("管理后台用户[{}]获取所有设备", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (DevicePo device : deviceAppService.listAll()) {
            list.add(Map.of("code", device.getCode(), "label", device.getName()));
        }
        return ApiResponse.ok(list);
    }

    /**
     * 导出设备信息
     *
     * @param response 响应
     * @param device   设备信息
     */
    @Log(title = "设备信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:device:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceVo device) {
        log.info("管理后台用户[{}]导出设备信息", SecurityUtils.getUsername());
    }

    /**
     * 根据设备信息ID获取设备信息
     *
     * @param deviceId 设备信息ID
     * @return 设备信息信息
     */
    @RequiresPermissions("completeVehicle:vehicle:device:query")
    @GetMapping(value = "/{deviceId}")
    public ApiResponse getInfo(@PathVariable Long deviceId) {
        log.info("管理后台用户[{}]根据设备信息ID[{}]获取设备信息", SecurityUtils.getUsername(), deviceId);
        DevicePo devicePo = deviceAppService.getDeviceById(deviceId);
        return ApiResponse.ok(DeviceAssembler.INSTANCE.fromPo(devicePo));
    }

    /**
     * 新增设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    @Log(title = "设备信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:device:add")
    @PostMapping
    public ApiResponse add(@Validated @RequestBody DeviceVo device) {
        log.info("管理后台用户[{}]新增设备信息[{}]", SecurityUtils.getUsername(), device.getCode());
        if (!deviceAppService.checkCodeUnique(device.getId(), device.getCode())) {
            return ApiResponse.fail("新增设备信息'" + device.getCode() + "'失败，设备信息代码已存在");
        }
        DevicePo devicePo = DeviceAssembler.INSTANCE.toPo(device);
        devicePo.setCreateBy(SecurityUtils.getUserId().toString());
        return deviceAppService.createDevice(devicePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    @Log(title = "设备信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:device:edit")
    @PutMapping
    public ApiResponse edit(@Validated @RequestBody DeviceVo device) {
        log.info("管理后台用户[{}]修改保存设备信息[{}]", SecurityUtils.getUsername(), device.getCode());
        if (!deviceAppService.checkCodeUnique(device.getId(), device.getCode())) {
            return ApiResponse.fail("修改保存设备信息'" + device.getCode() + "'失败，设备信息代码已存在");
        }
        DevicePo devicePo = DeviceAssembler.INSTANCE.toPo(device);
        devicePo.setModifyBy(SecurityUtils.getUserId().toString());
        return deviceAppService.modifyDevice(devicePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 删除设备信息
     *
     * @param deviceIds 设备信息ID数组
     * @return 结果
     */
    @Log(title = "设备信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:device:remove")
    @DeleteMapping("/{deviceIds}")
    public ApiResponse remove(@PathVariable Long[] deviceIds) {
        log.info("管理后台用户[{}]删除设备信息[{}]", SecurityUtils.getUsername(), deviceIds);
        return deviceAppService.deleteDeviceByIds(deviceIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
