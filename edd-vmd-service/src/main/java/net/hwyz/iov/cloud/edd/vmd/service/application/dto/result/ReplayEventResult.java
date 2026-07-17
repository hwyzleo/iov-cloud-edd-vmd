package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 事件补发处理结果
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
public class ReplayEventResult {

    /**
     * 补发请求ID
     */
    private String replayId;

    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 已入队数（写入Outbox）
     */
    private int queuedCount;

    /**
     * 失败记录数
     */
    private int failureCount;

    /**
     * 失败详情列表
     */
    private List<String> failures;
}
