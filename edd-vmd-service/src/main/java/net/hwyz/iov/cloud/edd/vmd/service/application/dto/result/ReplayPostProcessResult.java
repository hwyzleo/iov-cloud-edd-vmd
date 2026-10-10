package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 零件导入后置处理重放结果
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
public class ReplayPostProcessResult {

    /**
     * 重放请求ID
     */
    private String replayId;

    /**
     * 候选实例总数
     */
    private int itemCount;

    /**
     * 规划动作总数
     */
    private int actionCount;

    /**
     * 已入Outbox事件动作数（仅表示已写入Outbox，Kafka 投递由 Relay 跟踪）
     */
    private int queuedCount;

    /**
     * 成功动作数
     */
    private int successCount;

    /**
     * 跳过动作数
     */
    private int skippedCount;

    /**
     * 失败动作数
     */
    private int failureCount;

    /**
     * 失败明细列表（partCode:sn:actionType 原因）
     */
    private List<String> failures;
}
