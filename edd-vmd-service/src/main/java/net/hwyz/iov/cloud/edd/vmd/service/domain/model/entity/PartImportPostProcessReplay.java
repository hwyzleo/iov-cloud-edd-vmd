package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 零件导入后置处理重放主任务领域实体
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartImportPostProcessReplay extends BaseDo<Long> {

    private Long id;
    private String replayId;
    private Long partImportDataId;
    private String batchNum;
    private String operatorId;
    private String operatorName;
    private String reason;
    private String scope;
    private String status;
    private Integer totalItemCount;
    private Integer totalActionCount;
    private Integer queuedCount;
    private Integer successCount;
    private Integer skippedCount;
    private Integer failureCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
}
