package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆导入补发单动作预检结果响应
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayActionPreviewResponse {

    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 动作名称
     */
    private String actionName;

    /**
     * 可执行目标数
     */
    private int eligibleCount;

    /**
     * 跳过车辆数（无候选目标）
     */
    private int skipCount;

    /**
     * 跳过原因（可空）
     */
    private String reason;
}
