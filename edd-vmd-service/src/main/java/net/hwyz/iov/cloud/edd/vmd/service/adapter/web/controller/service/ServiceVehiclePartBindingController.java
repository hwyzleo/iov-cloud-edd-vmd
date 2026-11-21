package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartBindingExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleByDeviceDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.exception.InputParamInvalidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆-零件绑定关系对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehiclePartBinding/v1")
public class ServiceVehiclePartBindingController extends BaseController {

    private final VehiclePartAppService vehiclePartAppService;
    private final VehicleNodeAppService vehicleNodeAppService;

    /**
     * 根据车架号查询当前全部 active 设备绑定
     *
     * @param vin 车架号
     * @return active 绑定列表
     */
    @GetMapping("/vin/{vin}/active")
    public List<VehiclePartBindingExResponse> listActiveBindingsByVin(@PathVariable String vin) {
        log.info("内部服务请求根据车架号[{}]查询active绑定", vin);
        // 查询 active 绑定（bindState = 1）
        VehiclePartQuery query = VehiclePartQuery.builder()
                .vin(vin)
                .bindState(1)
                .build();
        List<VehiclePartDto> dtoList = vehiclePartAppService.search(query);

        // 转换为 Response
        List<VehiclePartBindingExResponse> responseList = dtoList.stream()
                .map(dto -> {
                    VehiclePartBindingExResponse response = VehiclePartBindingExResponse.builder()
                            .bindingId(dto.getId())
                            .vin(dto.getVin())
                            .partCode(dto.getPartCode())
                            .sn(dto.getSn())
                            .vehicleNodeCode(dto.getVehicleNodeCode())
                            .bindTime(dto.getBindTime())
                            .build();
                    // 获取设备分类
                    if (dto.getVehicleNodeCode() != null) {
                        VehicleNode vehicleNode = vehicleNodeAppService.getVehicleNodeByCode(dto.getVehicleNodeCode());
                        if (vehicleNode != null) {
                            response.setDeviceCategory(vehicleNode.getDeviceCategory());
                        }
                    }
                    return response;
                })
                .collect(Collectors.toList());

        return responseList;
    }

    /**
     * 全量/增量分页拉取 active 绑定
     *
     * @param cursor 分页游标（可选，首次拉取不传）
     * @param size   每页大小（默认 100）
     * @return active 绑定列表
     */
    @GetMapping("/bootstrap")
    public List<VehiclePartBindingExResponse> listBindingsForBootstrap(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "100") Integer size) {
        log.info("内部服务请求全量分页拉取active绑定, cursor={}, size={}", cursor, size);

        // 查询 active 绑定（bindState = 1），支持分页
        VehiclePartQuery query = VehiclePartQuery.builder()
                .bindState(1)
                .beginId(cursor)
                .pageSize(size)
                .build();

        List<VehiclePartDto> dtoList = vehiclePartAppService.search(query);

        // 转换为 Response
        List<VehiclePartBindingExResponse> responseList = dtoList.stream()
                .map(dto -> {
                    VehiclePartBindingExResponse response = VehiclePartBindingExResponse.builder()
                            .bindingId(dto.getId())
                            .vin(dto.getVin())
                            .partCode(dto.getPartCode())
                            .sn(dto.getSn())
                            .vehicleNodeCode(dto.getVehicleNodeCode())
                            .bindTime(dto.getBindTime())
                            .build();
                    // 获取设备分类
                    if (dto.getVehicleNodeCode() != null) {
                        VehicleNode vehicleNode = vehicleNodeAppService.getVehicleNodeByCode(dto.getVehicleNodeCode());
                        if (vehicleNode != null) {
                            response.setDeviceCategory(vehicleNode.getDeviceCategory());
                        }
                    }
                    return response;
                })
                .collect(Collectors.toList());

        return responseList;
    }

    /**
     * 根据设备标识查询当前 active 绑定的车辆（VMD-DSN-CR-076，只读）
     * <p>
     * 供受信内部服务（CIAM 设备令牌签发）取得可信 VIN。三态结论通过 200 + bindingState 表达；
     * 无绑定与数据冲突不使用 404 / 空对象；冲突不返回 VIN。
     * 参数缺失/非法返回框架既有参数错误（100001），不混同为「无绑定」。
     * 只读：不触发绑定变更事件、不写时间线与首次激活。
     * </p>
     *
     * @param deviceId       设备序列号（绑定事实 tb_part_info.sn）
     * @param deviceCategory 设备类别（如 IVI）
     * @return 设备→当前绑定车辆只读结果
     */
    @GetMapping("/vehicleByDevice")
    public VehiclePartBindingByDeviceExResponse getVehicleByDevice(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String deviceCategory) {
        String normalizedDeviceId = trimToNull(deviceId);
        String normalizedCategory = trimToNull(deviceCategory);
        if (normalizedDeviceId == null) {
            throw new InputParamInvalidException("deviceId不能为空");
        }
        if (normalizedDeviceId.length() > 255) {
            throw new InputParamInvalidException("deviceId长度不能超过255");
        }
        if (normalizedCategory == null) {
            throw new InputParamInvalidException("deviceCategory不能为空");
        }
        if (normalizedCategory.length() > 20) {
            throw new InputParamInvalidException("deviceCategory长度不能超过20");
        }

        long begin = System.currentTimeMillis();
        VehicleByDeviceDto dto = vehiclePartAppService.queryVehicleByDevice(normalizedDeviceId, normalizedCategory);
        log.info("内部服务请求按设备标识[{}]类别[{}]查询当前绑定车辆, 结论={}, 耗时={}ms",
                maskDeviceId(normalizedDeviceId), normalizedCategory, dto.getBindingState(),
                System.currentTimeMillis() - begin);

        return VehiclePartBindingByDeviceExResponse.builder()
                .bindingState(dto.getBindingState())
                .vin(dto.getVin())
                .factRef(dto.getFactRef())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    /**
     * 去除首尾空白，空串归为 null
     *
     * @param value 原值
     * @return 规范化后的值
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 设备标识脱敏（保留前 4 与后 4 位，不输出完整设备标识）
     *
     * @param deviceId 设备标识
     * @return 脱敏后的设备标识
     */
    private static String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() <= 8) {
            return "****";
        }
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
    }

}