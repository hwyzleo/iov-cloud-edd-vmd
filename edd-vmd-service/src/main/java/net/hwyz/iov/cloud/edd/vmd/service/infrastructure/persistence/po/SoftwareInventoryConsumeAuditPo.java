package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * OTA 车辆软件观测消费审计表 持久化对象
 * <p>
 * VMD-DSN-CR-046: 事件级幂等（eventId + observationKey）+ item 隔离 + DLQ 监控
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_software_inventory_consume_audit")
public class SoftwareInventoryConsumeAuditPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * OTA 观测事件 ID（幂等键，UK）
     */
    @TableField("event_id")
    private String eventId;

    /**
     * OTA FULL 观测身份（幂等键，UK）
     */
    @TableField("observation_key")
    private String observationKey;

    /**
     * VIN 哈希（SHA-256，脱敏审计）
     */
    @TableField("vin_hash")
    private String vinHash;

    /**
     * 状态：PROCESSING/SUCCESS/PARTIAL/QUARANTINED/FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 事件 item 总数
     */
    @TableField("item_total")
    private Integer itemTotal;

    /**
     * 成功写入数
     */
    @TableField("item_applied")
    private Integer itemApplied;

    /**
     * 幂等/版本gate忽略数
     */
    @TableField("item_ignored")
    private Integer itemIgnored;

    /**
     * 隔离数（无绑定/多绑定/非法item）
     */
    @TableField("item_quarantined")
    private Integer itemQuarantined;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最近错误码
     */
    @TableField("last_error_code")
    private String lastErrorCode;

    /**
     * 隔离明细 JSON（ecuId/target/slot/脱敏VIN/原因）
     */
    @TableField("quarantine_detail")
    private String quarantineDetail;

    /**
     * 接收时间
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
