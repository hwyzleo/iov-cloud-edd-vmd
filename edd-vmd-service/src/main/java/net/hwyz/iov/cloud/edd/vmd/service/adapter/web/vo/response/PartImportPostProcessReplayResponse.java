package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 零件导入后置处理重放主任务响应
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
public class PartImportPostProcessReplayResponse {

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

    /**
     * 逐零件、逐动作状态明细
     */
    private List<PartImportPostProcessReplayItemResponse> items;
}
