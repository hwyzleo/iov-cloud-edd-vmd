package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 证书申请列表响应（MPT，CR-053）
 * <p>
 * 列表不返回 CSR 全文，仅返回证书元数据与状态。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateListResponse {

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
     * 修改时间
     */
    private LocalDateTime modifyTime;

}
