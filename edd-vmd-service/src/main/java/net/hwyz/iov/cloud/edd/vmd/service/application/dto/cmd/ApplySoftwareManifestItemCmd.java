package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 软件实装清单条目命令
 * <p>
 * VMD-DSN-CR-046: 由 InventoryObservedMapper 将 OTA 观测事件 item 映射而来，
 * 固定 source=VEHICLE_REPORT / isConfirmed=true，进入 SoftwareInventoryAppService.applyManifest 消解。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplySoftwareManifestItemCmd {

    /**
     * 绑定ID（= vehicle_part.id，由 BindingResolver 解析）
     */
    private Long bindingId;

    /**
     * 零件ID（= part_info.id，由 BindingResolver 解析）
     */
    private Long partId;

    /**
     * 车载节点代码（← item.ecuId）
     */
    private String vehicleNodeCode;

    /**
     * 软件目标代码（← item.softwareTargetCode；legacy 单镜像 → ECU_IMAGE）
     */
    private String softwareTargetCode;

    /**
     * 软件零件号（← item.softwarePartNumber）
     */
    private String softwarePartNo;

    /**
     * 软件版本（← item.swVersion）
     */
    private String softwareVersion;

    /**
     * 槽位（← item.slot，可空）
     */
    private String slot;

    /**
     * 是否当前启动槽（← item.active；SINGLE_IMAGE 无槽位默认 true）
     */
    private Boolean isActiveSlot;

    /**
     * 制品摘要（← item.digest）
     */
    private String digest;

    /**
     * 变更类型（事件契约未定义，默认 INITIAL）
     */
    private String changeType;

    /**
     * 观测身份（← event.observationKey）
     */
    private String observationKey;

    /**
     * canonicalization 版本（← event.canonicalizationVersion）
     */
    private Integer canonicalizationVersion;

    /**
     * canonical 摘要（← event.canonicalDigest）
     */
    private String canonicalDigest;
}
