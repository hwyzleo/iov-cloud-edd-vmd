package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;

import java.time.LocalDateTime;

/**
 * 滞留开通批次运维终止命令（VMD-DSN-CR-065 §3.1）。
 * <p>
 * 业务幂等由目标 requestId、当前终态与条件更新保证；maintenanceRequestId 仅用于操作审计关联。
 * expectedState + expectedModifyTime 防止运维人员基于过期页面执行（视图过期 STALE_VIEW）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminateStaleProvisioningCommand {

    /** 维护操作幂等/审计关联号（由调用方生成） */
    private String maintenanceRequestId;

    /** TBOX 稳定 requestId（目标批次） */
    private String requestId;

    /** 运维页面所见批次状态；事务内须与最新一致，否则 STALE_VIEW */
    private BusinessKeyProvisioningState expectedState;

    /** 运维页面所见批次修改时间；事务内须与最新一致，否则 STALE_VIEW */
    private LocalDateTime expectedModifyTime;

    /** 受治理终止原因码 */
    private TerminationReasonCode reasonCode;

    /** 自由文本上下文（仅补充、非状态机判断输入；将做长度限制/换行清理/敏感信息过滤） */
    private String reason;

    /** 工单号（审计关联） */
    private String ticketNo;
}
