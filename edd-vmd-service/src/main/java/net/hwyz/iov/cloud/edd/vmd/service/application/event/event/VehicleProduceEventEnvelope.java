package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车辆生产事件 Kafka 信封
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 * <p>
 * 用于构造发送到 Kafka 的车辆生产事件消息
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleProduceEventEnvelope {

    // ========== 信封必填字段 ==========

    /**
     * 事件唯一ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 聚合类型（VEHICLE）
     */
    private String aggregateType;

    /**
     * 聚合ID（VIN）
     */
    private String aggregateId;

    /**
     * 聚合版本号
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
     * 事件payload（当前车辆完整快照）
     */
    private VehicleProducePayload payload;

    // ========== 补发扩展字段 ==========

    /**
     * 是否为补发事件
     */
    private Boolean replay;

    /**
     * 原批次号
     */
    private String batchNum;

    /**
     * 补发请求ID
     */
    private String replayId;

    /**
     * 补发操作人
     */
    private String replayOperator;

    /**
     * 补发时间
     */
    private LocalDateTime replayedAt;

    /**
     * 车辆生产事件 payload
     * <p>
     * 包含当前车辆完整快照，不是历史原始报文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleProducePayload {

        /**
         * 车架号
         */
        private String vin;

        /**
         * 生产时间
         */
        private LocalDateTime produceTime;

        /**
         * 工厂代码
         */
        private String plantCode;

        /**
         * 品牌代码
         */
        private String brandCode;

        /**
         * 平台代码
         */
        private String platformCode;

        /**
         * 车系代码
         */
        private String carLineCode;

        /**
         * 车型代码
         */
        private String modelCode;

        /**
         * 版本代码
         */
        private String variantCode;

        /**
         * 配置代码
         */
        private String configurationCode;

        /**
         * 订单号
         */
        private String orderNum;
    }
}
