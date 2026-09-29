package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 证书安装结果补录请求（MPT，CR-053）
 * <p>
 * 复用共享确认内核：校验 requestId + certSn + deviceSn；原因与工单必填。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CertificateConfirmInstalledRequest extends BaseRequest {

    /**
     * 证书序列号（对象校验）
     */
    @NotBlank(message = "certSn不能为空")
    private String certSn;

    /**
     * 设备SN（对象校验）
     */
    @NotBlank(message = "deviceSn不能为空")
    private String deviceSn;

    /**
     * 安装结果：SUCCESS/FAILED
     */
    @NotBlank(message = "result不能为空")
    private String result;

    /**
     * 失败原因
     */
    private String failReason;

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
