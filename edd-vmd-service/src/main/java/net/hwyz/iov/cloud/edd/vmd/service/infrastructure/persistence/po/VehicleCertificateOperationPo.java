package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 证书人工补偿操作审计表 持久化对象
 * <p>
 * CR-053：记录 MPT 高风险人工写操作业务时间线，不参与证书状态权威判定。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_certificate_operation")
public class VehicleCertificateOperationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作记录ID（幂等键）
     */
    @TableField("operation_id")
    private String operationId;

    /**
     * 关联证书申请 request_id
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 操作类型：COMPENSATE / RECONCILE / CONFIRM_INSTALLED
     */
    @TableField("action")
    private String action;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private String operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 人工原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 关联工单号
     */
    @TableField("ticket_no")
    private String ticketNo;

    /**
     * MES原请求号
     */
    @TableField("original_request_id")
    private String originalRequestId;

    /**
     * 操作前证书状态
     */
    @TableField("before_status")
    private String beforeStatus;

    /**
     * 操作后证书状态
     */
    @TableField("after_status")
    private String afterStatus;

    /**
     * 规范化请求摘要 + CSR SHA-256 指纹（不含 CSR 全文/证书本体/凭据）
     */
    @TableField("request_digest")
    private String requestDigest;

    /**
     * 操作结果：SUCCESS / IDEMPOTENT_HIT / CONFLICT / FAILED / PENDING
     */
    @TableField("result")
    private String result;

    /**
     * 失败错误码（806xxx）
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 来源IP
     */
    @TableField("source_ip")
    private String sourceIp;

    /**
     * 终端 User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 操作发生时间
     */
    @TableField("occurred_at")
    private LocalDateTime occurredAt;

}
