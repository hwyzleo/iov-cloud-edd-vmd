package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 车辆导入事件补发逐项动作审计领域实体
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 * <p>
 * 唯一键 (replay_id, action_type, aggregate_type, aggregate_id, aggregate_version)，
 * 保证单次请求逐项幂等。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehImportEventReplayItem extends BaseDo<Long> {

    private Long id;
    private String replayId;
    private String actionType;
    private String aggregateType;
    private String aggregateId;
    private Long aggregateVersion;
    private String sourceRecordId;
    private String eventId;
    /** 状态：PENDING/RUNNING/QUEUED/SUCCESS/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL */
    private String status;
    private String skipReason;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
}
