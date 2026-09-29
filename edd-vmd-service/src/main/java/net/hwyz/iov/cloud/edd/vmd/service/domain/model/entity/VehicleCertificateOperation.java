package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 证书人工补偿操作审计领域实体
 * <p>
 * CR-053：记录 MPT 高风险人工写操作（COMPENSATE / RECONCILE / CONFIRM_INSTALLED）的业务时间线。
 * 仅保存请求摘要、CSR 指纹、状态变化与操作上下文，不保存 CSR 全文、证书本体、设备私钥或 PKI 凭据。
 * 本表不参与证书状态权威判定。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCertificateOperation extends BaseDo<Long> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 操作记录ID（幂等键）
     */
    private String operationId;

    /**
     * 关联证书申请 request_id
     */
    private String requestId;

    /**
     * 操作类型：COMPENSATE / RECONCILE / CONFIRM_INSTALLED
     */
    private String action;

    /**
     * 操作人ID
     */
    private String operatorId;

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
     * MES原请求号
     */
    private String originalRequestId;

    /**
     * 操作前证书状态
     */
    private String beforeStatus;

    /**
     * 操作后证书状态
     */
    private String afterStatus;

    /**
     * 规范化请求摘要 + CSR SHA-256 指纹（不含 CSR 全文/证书本体/凭据）
     */
    private String requestDigest;

    /**
     * 操作结果：SUCCESS / IDEMPOTENT_HIT / CONFLICT / FAILED / PENDING
     */
    private String result;

    /**
     * 失败错误码（806xxx）
     */
    private String errorCode;

    /**
     * 来源IP
     */
    private String sourceIp;

    /**
     * 终端 User-Agent
     */
    private String userAgent;

    /**
     * 操作发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
