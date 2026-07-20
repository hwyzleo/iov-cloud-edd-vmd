package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书申请结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateApplyResult {

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
     * PKI申请编号
     */
    private String pkiRequestId;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 证书DER Base64编码
     */
    private String certificateDerBase64;

    /**
     * 证书链DER Base64编码
     */
    private String[] chainDerBase64;

    /**
     * 证书颁发者
     */
    private String issuer;

    /**
     * 证书指纹
     */
    private String fingerprint;

    /**
     * 有效期开始时间
     */
    private String notBefore;

    /**
     * 有效期结束时间
     */
    private String notAfter;

}
