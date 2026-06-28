package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartBindingExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
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

}