package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 零件导入后置处理动作预览
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 供前端二次确认弹窗展示「本次将重放哪些动作」。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostProcessActionPreview {

    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 动作名称
     */
    private String label;

    /**
     * 是否适用（当前记录 / 投影状态下的静态判定）
     */
    private Boolean applicable;

    /**
     * 不适用原因（applicable=false 时）
     */
    private String reason;
}
