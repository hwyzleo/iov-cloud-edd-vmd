package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 零件实例入站事件 Kafka 信封
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 用于构造发送到 Kafka 的零件实例入站事件消息（topic=vmd.part-inbound.changed，Key=partCode:sn）。
 * 重放场景从当前 part_info 构造当前快照，不复用历史 payload；
 * 消费者按实例业务键 (partCode, sn) 与版本幂等收敛。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartInboundEventEnvelope {

    // ========== 信封必填字段 ==========

    /**
     * 事件唯一ID（新生成，不复用历史）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 聚合类型（PART_INSTANCE）
     */
    private String aggregateType;

    /**
     * 聚合ID（partCode:sn）
     */
    private String aggregateId;

    /**
     * 聚合版本号（实例版本）
     */
    private Long version;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 生产者标识
     */
    private String producer;

    /**
     * 事件payload（当前零件实例完整快照）
     */
    private PartInboundPayload payload;

    // ========== 重放扩展字段 ==========

    /**
     * 是否为重放事件
     */
    private Boolean replay;

    /**
     * 原导入批次号
     */
    private String batchNum;

    /**
     * 重放请求ID
     */
    private String replayId;

    /**
     * 重放操作人
     */
    private String replayOperator;

    /**
     * 重放时间
     */
    private LocalDateTime replayedAt;

    /**
     * 零件实例入站事件 payload
     * <p>
     * 包含当前零件实例完整快照，不是历史原始报文。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartInboundPayload {

        /**
         * 零件编码
         */
        private String partCode;

        /**
         * 零件序列号
         */
        private String sn;

        /**
         * 车载节点代码（可空）
         */
        private String vehicleNodeCode;

        /**
         * 零件类型快照
         */
        private String partType;

        /**
         * 供应商编码（透传）
         */
        private String supplierCode;

        /**
         * 硬件零件号
         */
        private String hardwarePn;

        /**
         * 硬件版本号
         */
        private String hardwareVer;

        /**
         * 配置字
         */
        private String configWord;

        /**
         * 类型特有标准化属性（extra JSON）
         */
        private String extra;

        /**
         * 实例状态
         */
        private Integer instanceState;

        /**
         * 原导入批次号
         */
        private String batchNum;

        // ========== 绑定快照（当前 active 绑定时填充） ==========

        /**
         * 车架号（active 绑定时）
         */
        private String vin;

        /**
         * 绑定ID（active 绑定时 = vehicle_part.id）
         */
        private Long bindingId;

        /**
         * 设备分类（active 绑定时，取自 mdm_vehicle_node）
         */
        private String deviceCategory;

        /**
         * 安装位置（active 绑定时）
         */
        private String position;

        /**
         * 绑定时间（active 绑定时）
         */
        private LocalDateTime bindTime;
    }
}
