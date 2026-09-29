package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书申请详情结果（MPT，CR-053）
 * <p>
 * 详情仅返回 CSR SHA-256 指纹，不返回 CSR 全文；附带脱敏操作审计时间线。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDetailResult {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 业务请求ID
     */
    private String requestId;

    /**
     * MES原请求号
     */
    private String originalRequestId;

    /**
     * PKI申请编号
     */
    private String pkiRequestId;

    /**
     * 车辆VIN
     */
    private String vin;

    /**
     * 设备类别
     */
    private String deviceCategory;

    /**
     * 设备SN
     */
    private String deviceSn;

    /**
     * 证书Profile
     */
    private String certificateProfile;

    /**
     * CSR SHA-256 指纹（不含 CSR 全文）
     */
    private String csrFingerprint;

    /**
     * X.509 Subject
     */
    private String subject;

    /**
     * X.509 Issuer
     */
    private String issuer;

    /**
     * 证书序列号
     */
    private String certSn;

    /**
     * 证书状态
     */
    private String status;

    /**
     * 来源系统（MES / MPT_COMPENSATION）
     */
    private String sourceSystem;

    /**
     * 证书SHA-256指纹
     */
    private String certificateFingerprint;

    /**
     * 有效期开始时间
     */
    private LocalDateTime notBefore;

    /**
     * 有效期结束时间
     */
    private LocalDateTime notAfter;

    /**
     * 签发时间
     */
    private LocalDateTime issuedAt;

    /**
     * 安装确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 人工补偿原因
     */
    private String compensationReason;

    /**
     * 关联工单号
     */
    private String ticketNo;

    /**
     * 最后人工操作人
     */
    private String lastOperator;

    /**
     * 最后人工操作时间
     */
    private LocalDateTime lastOperationAt;

    /**
     * 最近失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;

    /**
     * 脱敏操作审计时间线
     */
    private List<CertificateOperationResult> operations;

}
