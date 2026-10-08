package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥吊销管理请求（CR-055 §7.3）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptBusinessKeyRevokeRequest {

    /**
     * 吊销幂等键（framework revoke 幂等键，结果未知以原键对账）
     */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * framework/KMS 不透明标识
     */
    @NotBlank(message = "keyId 不能为空")
    private String keyId;

    /**
     * 吊销原因
     */
    @NotBlank(message = "吊销原因不能为空")
    private String reason;
}
