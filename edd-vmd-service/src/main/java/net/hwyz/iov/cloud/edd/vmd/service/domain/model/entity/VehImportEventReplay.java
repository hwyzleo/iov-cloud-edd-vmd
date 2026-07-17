package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 车辆导入成功事件补发审计领域实体
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehImportEventReplay extends BaseDo<Long> {

    private Long id;
    private String replayId;
    private Long vehImportDataId;
    private String batchNum;
    private String eventType;
    private String operatorId;
    private String operatorName;
    private String reason;
    private String status;
    private Integer totalCount;
    private Integer queuedCount;
    private Integer failureCount;
    private String failureDetail;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
}
