package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车辆导入事件补发响应
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * VMD-DSN-CR-057: 扩展分动作结果
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayEventResponse {

    /**
     * 补发请求ID
     */
    private String replayId;

    /**
     * 导入类型（PRODUCE/TOL/EOL）
     */
    private String importType;

    /**
     * 总动作数（规划的动作目标总数）
     */
    private int totalCount;

    /**
     * 已入队数（写入Outbox）
     */
    private int queuedCount;

    /**
     * 成功动作数
     */
    private int successCount;

    /**
     * 跳过动作数
     */
    private int skipCount;

    /**
     * 失败动作数
     */
    private int failureCount;

    /**
     * 分动作结果
     */
    private List<ReplayActionResponse> actionResults;

    /**
     * 失败详情列表
     */
    private List<String> failures;
}
