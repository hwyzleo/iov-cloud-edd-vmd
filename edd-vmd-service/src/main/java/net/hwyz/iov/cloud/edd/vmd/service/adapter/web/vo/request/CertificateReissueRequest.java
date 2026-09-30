package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 证书重新签发/续期请求（MPT，CR-053 扩展）
 * <p>
 * 用于原证书有效期过短、密钥轮换或注入失败后需要一张全新证书的场景：作废旧证书后以新有效期重签。
 * CSR 全文不落库，须由设备侧重新提供（同公钥续期 / 新公钥换钥）；原因与工单必填（授权操作）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CertificateReissueRequest extends BaseRequest {

    /**
     * CSR DER Base64（设备侧重新提供；同公钥即续期，新公钥即换钥）
     */
    @NotBlank(message = "csrDerBase64不能为空")
    private String csrDerBase64;

    /**
     * 人工原因（必填）
     */
    @NotBlank(message = "reason不能为空")
    private String reason;

    /**
     * 工单号（必填）
     */
    @NotBlank(message = "ticketNo不能为空")
    private String ticketNo;

}
