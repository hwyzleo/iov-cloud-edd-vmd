package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ApplySoftwareManifestItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSoftwareBindingResolver;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareManifestItemInvalidException;
import org.springframework.stereotype.Component;

/**
 * OTA 观测事件 → 领域命令映射器
 * <p>
 * VMD-DSN-CR-046: 将事件 item 映射为 ApplySoftwareManifestItemCmd，
 * 固定 source=VEHICLE_REPORT / isConfirmed=true（由消费者装配），
 * 不做 SINGLE_IMAGE/MULTI_TARGET 双分支。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
@Component
public class InventoryObservedMapper {

    /**
     * legacy 单镜像默认 Target
     */
    public static final String LEGACY_SINGLE_IMAGE_TARGET = "ECU_IMAGE";

    /**
     * 将事件 item 映射为软件实装清单条目命令
     *
     * @param event      观测事件（事件级字段）
     * @param item       软件实装单元
     * @param resolution 已解析的唯一 active 绑定
     * @return 软件实装清单条目命令
     * @throws SoftwareManifestItemInvalidException item 必填缺失或字段非法
     */
    public ApplySoftwareManifestItemCmd toManifestItem(
            VehicleSoftwareInventoryObservedEvent event,
            VehicleSoftwareInventoryObservedEvent.Item item,
            VehicleSoftwareBindingResolver.BindingResolution resolution) {

        if (item == null || item.getEcuId() == null || item.getEcuId().isBlank()
                || item.getSoftwareTargetCode() == null || item.getSoftwareTargetCode().isBlank()
                || item.getSwVersion() == null || item.getSwVersion().isBlank()) {
            throw new SoftwareManifestItemInvalidException(
                    "item 必填缺失（ecuId/softwareTargetCode/swVersion）");
        }

        // 同 Target 多 Slot 时 active 必须由上游明确；SINGLE_IMAGE 无槽位默认 active
        Boolean isActiveSlot = item.getActive();
        if (item.getSlot() != null && !item.getSlot().isBlank() && isActiveSlot == null) {
            throw new SoftwareManifestItemInvalidException(
                    "item[" + item.getEcuId() + "]多Slot场景必须显式声明active");
        }
        if (isActiveSlot == null) {
            isActiveSlot = Boolean.TRUE;
        }

        // 变更类型：事件契约未定义，默认 INITIAL
        String changeType = "INITIAL";

        return ApplySoftwareManifestItemCmd.builder()
                .bindingId(resolution.bindingId())
                .partId(resolution.partId())
                .vehicleNodeCode(item.getEcuId())
                .softwareTargetCode(item.getSoftwareTargetCode())
                .softwarePartNo(item.getSoftwarePartNumber())
                .softwareVersion(item.getSwVersion())
                .slot(item.getSlot())
                .isActiveSlot(isActiveSlot)
                .digest(item.getDigest())
                .changeType(changeType)
                .observationKey(event.getObservationKey())
                .canonicalizationVersion(event.getCanonicalizationVersion())
                .canonicalDigest(event.getCanonicalDigest())
                .build();
    }
}
