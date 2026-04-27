package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehicleConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleConfigAppService;
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
 * 车辆配置相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicleConfig/v1")
public class MptVehicleConfigController extends BaseController {

    private final VehicleConfigAppService vehicleConfigAppService;

    /**
     * 分页查询车辆配置
     *
     * @param vehicleConfig 车辆配置
     * @return 车辆配置列表
     */
    @RequiresPermissions("iov:configCenter:vehicleConfig:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehicleConfigVo>> list(VehicleConfigVo vehicleConfig) {
        log.info("管理后台用户[{}]分页查询车辆配置", SecurityUtils.getUsername());
        startPage();
        VehicleConfigQuery query = VehicleConfigQuery.builder()
                .vin(vehicleConfig.getVin())
                .version(vehicleConfig.getVersion())
                .beginTime(getBeginTime(vehicleConfig))
                .endTime(getEndTime(vehicleConfig))
                .build();
        List<VehicleConfigDto> dtoList = vehicleConfigAppService.search(query);
        return ApiResponse.ok(getPageResult(MptVehicleConfigAssembler.INSTANCE.fromConfigDtoList(dtoList)));
    }

    /**
     * 分页查询车辆配置项
     *
     * @param vin               车架号
     * @param vehicleConfigItem 车辆配置项
     * @return 车辆配置项列表
     */
    @RequiresPermissions("iov:configCenter:vehicleConfig:list")
    @GetMapping(value = "/{vin}/configItem/list")
    public ApiResponse<PageResult<VehicleConfigItemVo>> listConfigItem(@PathVariable String vin, VehicleConfigItemVo vehicleConfigItem) {
        log.info("管理后台用户[{}]分页查询车辆[{}]配置项", SecurityUtils.getUsername(), vin);
        startPage();
        List<VehicleConfigItemDto> dtoList = vehicleConfigAppService.searchItem(vin, vehicleConfigItem.getVersion());
        return ApiResponse.ok(getPageResult(MptVehicleConfigAssembler.INSTANCE.fromItemDtoList(dtoList)));
    }

    /**
     * 导出车辆配置
     *
     * @param response      响应
     * @param vehicleConfig 车辆配置
     */
    @Log(title = "车辆配置管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("iov:configCenter:vehicleConfig:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehicleConfigVo vehicleConfig) {
        log.info("管理后台用户[{}]导出车辆配置", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆配置ID获取车辆配置
     *
     * @param vehicleConfigId 车辆配置ID
     * @return 车辆配置
     */
    @RequiresPermissions("iov:configCenter:vehicleConfig:query")
    @GetMapping(value = "/{vehicleConfigId}")
    public ApiResponse<VehicleConfigVo> getInfo(@PathVariable Long vehicleConfigId) {
        log.info("管理后台用户[{}]根据车辆配置ID[{}]获取车辆配置", SecurityUtils.getUsername(), vehicleConfigId);
        return ApiResponse.ok(MptVehicleConfigAssembler.INSTANCE.fromConfigDto(vehicleConfigAppService.getVehicleConfigById(vehicleConfigId)));
    }

    /**
     * 根据车辆配置项ID获取车辆配置项
     *
     * @param vin                 车架号
     * @param vehicleConfigItemId 车辆配置项ID
     * @return 车辆配置项
     */
    @RequiresPermissions("iov:configCenter:vehicleConfig:query")
    @GetMapping(value = "/{vin}/configItem/{vehicleConfigItemId}")
    public ApiResponse<VehicleConfigItemVo> getConfigItemInfo(@PathVariable String vin, @PathVariable Long vehicleConfigItemId) {
        log.info("管理后台用户[{}]根据车辆[{}]配置项ID[{}]获取车辆配置项", SecurityUtils.getUsername(), vin, vehicleConfigItemId);
        return ApiResponse.ok(MptVehicleConfigAssembler.INSTANCE.fromItemDto(vehicleConfigAppService.getVehicleConfigItemById(vehicleConfigItemId)));
    }
}
