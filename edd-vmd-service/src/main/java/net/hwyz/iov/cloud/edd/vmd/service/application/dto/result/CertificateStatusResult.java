package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 证书状态结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateStatusResult {

    /**
     * 业务请求ID
     */
    private String requestId;

    /**
     * 状态
     */
    private String status;

    /**
     * 证书序列号
     */
    private String certSn;

    /**
     * 证书指纹
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
     * 失败原因
     */
    private String failReason;

}
