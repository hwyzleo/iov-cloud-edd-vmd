package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 证书人工操作审计结果（MPT，CR-053）
 * <p>
 * 仅返回脱敏字段，不包含 CSR 全文、证书本体或凭据。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateOperationResult {

    /**
     * 操作类型：COMPENSATE / RECONCILE / CONFIRM_INSTALLED
     */
    private String action;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 人工原因
     */
    private String reason;

    /**
     * 关联工单号
     */
    private String ticketNo;

    /**
     * 操作前证书状态
     */
    private String beforeStatus;

    /**
     * 操作后证书状态
     */
    private String afterStatus;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 操作发生时间
     */
    private LocalDateTime occurredAt;

}
