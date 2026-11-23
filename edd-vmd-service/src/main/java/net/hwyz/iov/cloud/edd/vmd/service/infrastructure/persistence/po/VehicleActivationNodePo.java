package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 车辆首次激活节点投影表 持久化对象（VMD-DSN-CR-077 / V72）
 * <p>
 * 唯一键 {@code uk_veh_activation_node_vin}；{@code first_activated_at} 取已消费事实
 * {@code occurred_at} 最小值（单调收敛）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_activation_node")
public class VehicleActivationNodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号（Kafka Key；唯一键）
     */
    @TableField("vin")
    private String vin;

    /**
     * 首次激活绝对时刻（UTC，事实 occurred_at 最小值）
     */
    @TableField("first_activated_at")
    private LocalDateTime firstActivatedAt;

    /**
     * 决定首次激活的绑定操作 ID
     */
    @TableField("binding_operation_id")
    private String bindingOperationId;

    /**
     * 来源事实引用（不含 PII）
     */
    @TableField("source_fact_ref")
    private String sourceFactRef;

    /**
     * 载荷摘要 sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与 eventId 无关
     */
    @TableField("payload_digest")
    private String payloadDigest;

    /**
     * 决定该投影的事件 ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 事实发生绝对时刻（UTC）
     */
    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    /**
     * VMD 首次接收该事实的绝对时刻（UTC）
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 事件契约版本
     */
    @TableField("schema_version")
    private Integer schemaVersion;

    /**
     * 摘要来源：EVENT / LOCAL（仅观测）
     */
    @TableField("digest_source")
    private String digestSource;

    /**
     * VIN 是否已在 VMD 建档：0=未建档
     */
    @TableField("vin_resolved")
    private Integer vinResolved;

}
