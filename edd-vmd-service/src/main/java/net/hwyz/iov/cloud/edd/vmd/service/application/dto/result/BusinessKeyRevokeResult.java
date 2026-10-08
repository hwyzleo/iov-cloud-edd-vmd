package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥吊销结果（CR-055 F20 §6.5）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyRevokeResult {

    /**
     * 吊销幂等键
     */
    private String requestId;

    /**
     * keyId
     */
    private String keyId;

    /**
     * 业务版本
     */
    private Long businessKeyVersion;

    /**
     * 状态（REVOKED / RECONCILE_REQUIRED）
     */
    private String state;
}
