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
 * 车辆导入事件补发主任务审计表 持久化对象
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * VMD-DSN-CR-057: 扩展为按 ImportType 路由的动作补偿主任务
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_import_event_replay")
public class VehImportEventReplayPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 补发请求ID（幂等键）
     */
    @TableField("replay_id")
    private String replayId;

    /**
     * 关联的车辆导入数据ID
     */
    @TableField("veh_import_data_id")
    private Long vehImportDataId;

    /**
     * 原批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 导入类型（PRODUCE/TOL/EOL，动作路由依据；原 event_type 更名）
     */
    @TableField("import_type")
    private String importType;

    /**
     * 请求动作范围：逗号分隔的动作类型子集，空表示全部适用动作
     */
    @TableField("requested_actions")
    private String requestedActions;

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
     * 补发原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 状态：PENDING/RUNNING/QUEUED/SUCCESS/SUCCEEDED_WITH_SKIPS/PARTIAL_FAILED/FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 总记录数
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 已入队数（写入Outbox）
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
    @TableField("skip_count")
    private Integer skipCount;

    /**
     * 失败数
     */
    @TableField("failure_count")
    private Integer failureCount;

    /**
     * 失败详情（JSON，受限长度）
     */
    @TableField("failure_detail")
    private String failureDetail;

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
