package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥轮换结果（CR-055 F20 §6.4）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyRotationResult {

    /**
     * 轮换幂等键
     */
    private String requestId;

    /**
     * 新密钥 keyId
     */
    private String newKeyId;

    /**
     * 新密钥业务版本
     */
    private Long newBusinessKeyVersion;

    /**
     * 旧密钥 keyId
     */
    private String oldKeyId;

    /**
     * 旧密钥业务版本
     */
    private Long oldBusinessKeyVersion;

    /**
     * 新密钥状态（ACTIVE）
     */
    private String state;
}
