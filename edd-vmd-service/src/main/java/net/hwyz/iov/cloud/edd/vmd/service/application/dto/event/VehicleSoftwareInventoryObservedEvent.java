package net.hwyz.iov.cloud.edd.vmd.service.application.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * OTA 车辆软件观测事件 DTO
 * <p>
 * VMD-DSN-CR-046: IOV-OTA 经 Transactional Outbox 发布的云服务事件契约
 * （topic: ota.vehicle-software-inventory.observed，Key=VIN，不使用 Proto、不属于 PAR-PROTO）
 * <p>
 * VMD 信任事件为「OTA 已成功接受的 FULL 观测」，但仍执行契约校验、绑定校验与自身领域消解。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleSoftwareInventoryObservedEvent {

    /**
     * 事件ID（幂等键，必填）
     */
    private String eventId;

    /**
     * OTA FULL 观测身份（幂等键，必填）
     */
    private String observationKey;

    /**
     * 车架号（必填，Kafka Key=VIN）
     */
    private String vin;

    /**
     * 清单版本（必填，映射 sourceVersion）
     */
    private String inventoryRevision;

    /**
     * 清单模型（SINGLE_IMAGE/MULTI_TARGET，仅审计/查询上下文）
     */
    private String inventoryModel;

    /**
     * 源端采集时间（必填，映射 occurredAt / 版本时序 gate）
     */
    private Instant collectedAt;

    /**
     * IOV-OTA 成功受理时间（仅链路审计，不参与时序判定）
     */
    private Instant acceptedAt;

    /**
     * canonicalization 版本（必填，跨服务审计）
     */
    private Integer canonicalizationVersion;

    /**
     * canonical 摘要（必填，跨服务审计 / 幂等冲突判定）
     */
    private String canonicalDigest;

    /**
     * IOV-OTA 已规范化的完整 SoftwareUnit 列表
     */
    private List<Item> items;

    /**
     * 软件实装单元
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /**
         * ECU 标识（映射 vehicleNodeCode，绑定解析键）
         */
        private String ecuId;

        /**
         * 硬件零件号（仅审计/校验上下文）
         */
        private String hardwarePartNumber;

        /**
         * 硬件版本（仅审计/校验上下文）
         */
        private String hwVersion;

        /**
         * 软件目标代码
         */
        private String softwareTargetCode;

        /**
         * 软件零件号
         */
        private String softwarePartNumber;

        /**
         * 软件版本
         */
        private String swVersion;

        /**
         * A/B 槽位（可空；同 Target 多 Slot 时 active 必须由上游明确）
         */
        private String slot;

        /**
         * 是否当前启动槽（同 Target 多 Slot 时必填）
         */
        private Boolean active;

        /**
         * 制品摘要
         */
        private String digest;
    }
}
