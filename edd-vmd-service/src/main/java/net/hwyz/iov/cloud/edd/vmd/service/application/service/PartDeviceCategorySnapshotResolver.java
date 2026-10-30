package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import org.springframework.stereotype.Component;

/**
 * 车载节点设备类别快照解析职责（CR-072 §4.1）
 * <p>
 * 输入合并策略批准后的最终 vehicleNodeCode，经本地 VehicleNode 投影解析 device_category；
 * application 不直接读 MdmVehicleNodePo/Mapper，统一经 VehicleNodeAppService（本地投影，row_valid=1）。
 * 解析采用一次完整本地投影读取（节点码/类别/版本来自同一读视图），失败按可重试/待治理分类。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartDeviceCategorySnapshotResolver {

    /** 对齐 tb_mdm_vehicle_node.device_category VARCHAR(20)；超过长度拒绝、不截断 */
    static final int MAX_CATEGORY_LENGTH = 20;

    private final VehicleNodeAppService vehicleNodeAppService;

    /**
     * 按最终节点码解析设备类别
     *
     * @param finalVehicleNodeCode 合并策略批准后的最终节点码（非空）
     * @return 解析结果（类别 + 投影来源标识/版本，可得时）
     * @throws DeviceCategoryResolveException 节点缺失（投影未就绪，可重试）/ 类别空 / 类别超长（待治理）
     */
    public ResolvedCategory resolve(String finalVehicleNodeCode) {
        VehicleNode node = vehicleNodeAppService.getVehicleNodeByCode(finalVehicleNodeCode);
        if (node == null) {
            // 证据不足：可能是投影同步尚未到达（可重试），不直接认定永久非法；记录来源未就绪/待治理
            throw new DeviceCategoryResolveException(finalVehicleNodeCode, true, "NODE_PROJECTION_NOT_READY_OR_INVALID");
        }
        String category = node.getDeviceCategory();
        if (category == null || category.isBlank()) {
            // 已确认数据治理问题：节点存在但类别为空
            throw new DeviceCategoryResolveException(finalVehicleNodeCode, false, "CATEGORY_BLANK");
        }
        String trimmed = category.trim();
        if (trimmed.length() > MAX_CATEGORY_LENGTH) {
            throw new DeviceCategoryResolveException(finalVehicleNodeCode, false, "CATEGORY_TOO_LONG");
        }
        return new ResolvedCategory(trimmed, node.getExternalRefId(), node.getExternalVersion());
    }

    /**
     * 解析结果（类别 + 投影来源标识/版本；externalRefId/externalVersion 不可得时为 null，不编造）
     */
    public record ResolvedCategory(String deviceCategory, String externalRefId, Long externalVersion) {
    }

}
