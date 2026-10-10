package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆导入补发单动作执行结果响应
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayActionResponse {

    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 规划目标数
     */
    private int totalCount;

    /**
     * 已入Outbox数
     */
    private int queuedCount;

    /**
     * 成功数
     */
    private int successCount;

    /**
     * 跳过数
     */
    private int skipCount;

    /**
     * 失败数
     */
    private int failureCount;
}
