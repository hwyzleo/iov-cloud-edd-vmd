package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 零件导入后置处理重放动作明细响应
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
public class PartImportPostProcessReplayItemResponse {

    private Long id;
    private String replayId;
    private String partCode;
    private String sn;
    private String actionType;
    private String idempotencyKey;
    private String status;
    private Integer attemptCount;
    private String eventId;
    private Long outboxId;
    private String errorCode;
    private String errorMessage;
    private String skipReason;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
