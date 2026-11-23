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
 * 车辆首次激活候选事实消费台账表 持久化对象（VMD-DSN-CR-077 / V72）
 * <p>
 * 唯一键 {@code uk_veh_act_consume_event(event_id)} 与
 * {@code uk_veh_act_consume_business(vin, binding_operation_id)} 承担并发双重去重兜底。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_activation_fact_consume")
public class VehicleActivationFactConsumePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件 ID（幂等键 1）
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 事件契约版本
     */
    @TableField("schema_version")
    private Integer schemaVersion;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 绑定操作 ID（幂等键 2）
     */
    @TableField("binding_operation_id")
    private String bindingOperationId;

    /**
     * 来源事实引用
     */
    @TableField("source_fact_ref")
    private String sourceFactRef;

    /**
     * 载荷摘要（与 eventId 无关）
     */
    @TableField("payload_digest")
    private String payloadDigest;

    /**
     * 摘要来源：EVENT / LOCAL
     */
    @TableField("digest_source")
    private String digestSource;

    /**
     * 事实发生绝对时刻（UTC）
     */
    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    /**
     * 接收绝对时刻（UTC）
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 状态：RECORDED / DUPLICATE / QUARANTINED / FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 是否作用于投影（0/1）
     */
    @TableField("applied")
    private Integer applied;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 错误 / 隔离原因摘要（脱敏、截断）
     */
    @TableField("last_error")
    private String lastError;

    /**
     * 来源 Topic（重放定位）
     */
    @TableField("topic")
    private String topic;

    /**
     * 来源分区
     */
    @TableField("partition_no")
    private Integer partitionNo;

    /**
     * 来源位点
     */
    @TableField("record_offset")
    private Long recordOffset;

}
