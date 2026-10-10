package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.List;

/**
 * 零件导入后置处理重放请求
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
@EqualsAndHashCode(callSuper = true)
public class ReplayPartImportPostProcessRequest extends BaseRequest {

    /**
     * 重放原因
     */
    private String reason;

    /**
     * 请求ID（可选，作为 replayId；未提供时由服务端生成）
     */
    private String requestId;

    /**
     * 动作范围（可选，显式选择动作子集；空表示全部适用动作）
     */
    private List<String> scope;

    /**
     * 是否仅重试失败或未完成动作
     */
    private Boolean retryFailedOnly;
}
