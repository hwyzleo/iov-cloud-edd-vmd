package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.SoftwareManifestRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareInventoryExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareManifestApplyResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆软件实装清单对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/softwareInventory/v1")
public class ServiceSoftwareInventoryController extends BaseController {

    private final SoftwareInventoryAppService softwareInventoryAppService;
    private final VehiclePartAppService vehiclePartAppService;

    /**
     * 查询车辆当前软件实装清单
     *
     * @param vin 车架号
     * @return 软件实装清单列表
     */
    @GetMapping("/vin/{vin}/current")
    public List<SoftwareInventoryExResponse> getCurrentSoftwareInventory(@PathVariable String vin) {
        log.info("内部服务请求查询车辆[{}]当前软件清单", vin);

        // 查询车辆所有 active 绑定
        List<VehiclePart> activeBindings = vehiclePartAppService.getActiveBindingsByVin(vin);

        List<SoftwareInventoryExResponse> result = new ArrayList<>();
        for (VehiclePart binding : activeBindings) {
            // 查询每个绑定的当前软件清单
            List<PartSoftwareInstallation> inventory = softwareInventoryAppService.getCurrentInventory(binding.getPartId());
            for (PartSoftwareInstallation installation : inventory) {
                result.add(SoftwareInventoryExResponse.builder()
                        .bindingId(installation.getBindingId())
                        .partId(installation.getPartId())
                        .vin(vin)
                        .vehicleNodeCode(binding.getVehicleNodeCode())
                        .softwareTargetCode(installation.getSoftwareTargetCode())
                        .softwarePartNo(installation.getSoftwarePartNo())
                        .softwareVersion(installation.getSoftwareVersion())
                        .slot(installation.getSlot())
                        .isConfirmed(installation.getIsConfirmed())
                        .inventoryVersion(installation.getInventoryVersion())
                        .source(installation.getSource())
                        .changeType(installation.getChangeType())
                        .effectiveFrom(installation.getEffectiveFrom())
                        .build());
            }
        }

        return result;
    }

    /**
     * 查询车辆软件版本变化历史
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码（可选）
     * @param softwareTargetCode 软件目标代码（可选）
     * @return 软件版本变化时间线
     */
    @GetMapping("/vin/{vin}/history")
    public List<SoftwareInventoryExResponse> getSoftwareHistory(
            @PathVariable String vin,
            @RequestParam(required = false) String vehicleNodeCode,
            @RequestParam(required = false) String softwareTargetCode) {
        log.info("内部服务请求查询车辆[{}]软件历史, vehicleNodeCode={}, softwareTargetCode={}", vin, vehicleNodeCode, softwareTargetCode);

        // 查询车辆所有 active 绑定
        List<VehiclePart> activeBindings = vehiclePartAppService.getActiveBindingsByVin(vin);

        List<SoftwareInventoryExResponse> result = new ArrayList<>();
        for (VehiclePart binding : activeBindings) {
            // 如果指定了车载节点代码，过滤不匹配的绑定
            if (vehicleNodeCode != null && !vehicleNodeCode.equals(binding.getVehicleNodeCode())) {
                continue;
            }

            // 查询该零件的所有软件安装记录（包括历史）
            List<PartSoftwareInstallation> history = softwareInventoryAppService.getCurrentInventory(binding.getPartId());
            for (PartSoftwareInstallation installation : history) {
                // 如果指定了软件目标代码，过滤不匹配的记录
                if (softwareTargetCode != null && !softwareTargetCode.equals(installation.getSoftwareTargetCode())) {
                    continue;
                }

                result.add(SoftwareInventoryExResponse.builder()
                        .bindingId(installation.getBindingId())
                        .partId(installation.getPartId())
                        .vin(vin)
                        .vehicleNodeCode(binding.getVehicleNodeCode())
                        .softwareTargetCode(installation.getSoftwareTargetCode())
                        .softwarePartNo(installation.getSoftwarePartNo())
                        .softwareVersion(installation.getSoftwareVersion())
                        .slot(installation.getSlot())
                        .isConfirmed(installation.getIsConfirmed())
                        .inventoryVersion(installation.getInventoryVersion())
                        .source(installation.getSource())
                        .changeType(installation.getChangeType())
                        .effectiveFrom(installation.getEffectiveFrom())
                        .build());
            }
        }

        return result;
    }

    /**
     * 全量/增量分页拉取当前软件清单
     *
     * @param cursor 分页游标（可选，首次拉取不传）
     * @param updatedAfter 增量拉取起始时间（可选）
     * @param size 每页大小（默认 100）
     * @return 软件实装清单列表
     */
    @GetMapping("/bootstrap")
    public List<SoftwareInventoryExResponse> listCurrentSoftwareInventory(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String updatedAfter,
            @RequestParam(defaultValue = "100") Integer size) {
        log.info("内部服务请求全量分页拉取软件清单, cursor={}, updatedAfter={}, size={}", cursor, updatedAfter, size);

        // TODO: 实现全量/增量分页拉取逻辑
        // 这里需要根据实际业务需求实现分页查询
        return new ArrayList<>();
    }

    /**
     * 外部来源同步回写软件实装清单
     *
     * @param request 软件实装清单写入请求
     * @return 写入结果
     */
    @PostMapping("/apply")
    public SoftwareManifestApplyResponse applySoftwareManifest(@RequestBody SoftwareManifestRequest request) {
        log.info("内部服务请求写入软件实装清单, vin={}, source={}, itemCount={}", request.getVin(), request.getSource(), request.getItems().size());

        int applied = 0;
        int ignoredByVersionGate = 0;
        Long currentInventoryVersion = 0L;

        for (SoftwareManifestRequest.SoftwareManifestItem item : request.getItems()) {
            // 查询车辆对应的零件绑定
            // 这里需要根据 vehicleNodeCode 和 vin 查询对应的 partId 和 bindingId
            // 简化实现：假设可以通过 vehiclePartAppService 查询
            VehiclePart binding = vehiclePartAppService.getActiveBindingByVinAndNodeCode(request.getVin(), item.getVehicleNodeCode());
            if (binding == null) {
                log.warn("未找到车辆[{}]节点[{}]的active绑定", request.getVin(), item.getVehicleNodeCode());
                continue;
            }

            // 调用消解算法
            SoftwareInventoryAppService.ApplyManifestResult result = softwareInventoryAppService.applyManifest(
                    binding.getPartId(),
                    binding.getId(),
                    request.getVin(),
                    item.getSoftwareTargetCode(),
                    item.getSoftwarePartNo(),
                    item.getSoftwareVersion(),
                    item.getDigest(),
                    item.getSlot(),
                    item.getChangeType(),
                    request.getSource(),
                    request.getRequestId(),
                    request.getOccurredAt(),
                    Instant.now(),
                    item.getIsConfirmed() != null ? item.getIsConfirmed() : false);

            if (result.applied()) {
                applied++;
            }
            if (result.ignoredByVersionGate()) {
                ignoredByVersionGate++;
            }
            if (result.currentInventoryVersion() > currentInventoryVersion) {
                currentInventoryVersion = result.currentInventoryVersion();
            }
        }

        return SoftwareManifestApplyResponse.builder()
                .applied(applied)
                .ignoredByVersionGate(ignoredByVersionGate)
                .currentInventoryVersion(currentInventoryVersion)
                .build();
    }
}
