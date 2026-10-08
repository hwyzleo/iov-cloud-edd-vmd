package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥在线申请请求（CR-055 §7.1 keyprov / Open API）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisionRequest {

    /**
     * 调用幂等键
     */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 设备实例序列号
     */
    @NotBlank(message = "deviceSn 不能为空")
    private String deviceSn;

    /**
     * 设备类别（如 TBOX）
     */
    private String deviceCategory;

    /**
     * 受治理业务域代码
     */
    @NotBlank(message = "businessDomain 不能为空")
    private String businessDomain;

    /**
     * 受治理用途代码
     */
    @NotBlank(message = "purpose 不能为空")
    private String purpose;
}
