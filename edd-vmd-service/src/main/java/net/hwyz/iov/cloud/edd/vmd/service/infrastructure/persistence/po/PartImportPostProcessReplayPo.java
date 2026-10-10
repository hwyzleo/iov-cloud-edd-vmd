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
 * 零件导入后置处理重放主任务表 持久化对象
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_import_postprocess_replay")
public class PartImportPostProcessReplayPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 重放请求ID（幂等键）
     */
    @TableField("replay_id")
    private String replayId;

    /**
     * 关联的零件导入数据ID
     */
    @TableField("part_import_data_id")
    private Long partImportDataId;

    /**
     * 原导入批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private String operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 重放原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 动作范围（逗号分隔动作类型子集，空表示全部适用动作）
     */
    @TableField("scope")
    private String scope;

    /**
     * 状态：PENDING/RUNNING/PARTIAL_SUCCESS/SUCCESS/FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 候选实例总数
     */
    @TableField("total_item_count")
    private Integer totalItemCount;

    /**
     * 规划动作总数
     */
    @TableField("total_action_count")
    private Integer totalActionCount;

    /**
     * 已入Outbox事件动作数
     */
    @TableField("queued_count")
    private Integer queuedCount;

    /**
     * 成功动作数
     */
    @TableField("success_count")
    private Integer successCount;

    /**
     * 跳过动作数
     */
    @TableField("skipped_count")
    private Integer skippedCount;

    /**
     * 失败动作数
     */
    @TableField("failure_count")
    private Integer failureCount;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
