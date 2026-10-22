package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运维操作者上下文（VMD-DSN-CR-065 §7.1 审计）。
 * <p>
 * 由 MPT 认证会话解析，随终止命令进入审计记录；不得拼入批次 fail_reason
 * （fail_reason 仅保留紧凑非敏感终止码 + 审计关联号）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorContext {

    /** 操作者ID */
    private String operatorId;

    /** 操作者名称 */
    private String operatorName;

    /** 来源IP */
    private String sourceIp;

    /** 来源UA */
    private String userAgent;
}
