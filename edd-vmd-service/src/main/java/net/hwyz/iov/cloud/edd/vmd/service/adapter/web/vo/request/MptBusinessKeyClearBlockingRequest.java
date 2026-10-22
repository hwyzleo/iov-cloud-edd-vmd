package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MPT 一键清理阻塞——清理请求（VMD-DSN-CR-065 §6.3）。
 * <p>
 * body 字段：maintenanceRequestId / expectedState / expectedModifyTime / reasonCode / reason / ticketNo；
 * requestId 走路径参数。expectedState + expectedModifyTime 防止运维基于过期页面执行（STALE_VIEW）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptBusinessKeyClearBlockingRequest {

    /** 维护操作幂等/审计关联号 */
    @NotBlank(message = "maintenanceRequestId 不能为空")
    private String maintenanceRequestId;

    /** 运维页面所见批次状态（PREPARING / RECONCILE_REQUIRED） */
    @NotBlank(message = "expectedState 不能为空")
    private String expectedState;

    /** 运维页面所见批次修改时间（防过期页面 STALE_VIEW，不可为空） */
    @NotNull(message = "expectedModifyTime 不能为空")
    private LocalDateTime expectedModifyTime;

    /** 受治理终止原因码（TerminationReasonCode） */
    @NotBlank(message = "reasonCode 不能为空")
    private String reasonCode;

    /** 自由文本上下文（服务端做长度限制/换行清理/敏感信息过滤） */
    private String reason;

    /** 工单号 */
    private String ticketNo;
}
