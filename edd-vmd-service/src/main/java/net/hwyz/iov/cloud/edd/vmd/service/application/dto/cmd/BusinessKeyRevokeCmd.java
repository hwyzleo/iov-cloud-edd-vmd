package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥吊销命令（CR-055 F20 §6.5，管理接口）
 * <p>
 * 按 keyId 吊销；吊销原因必填（审计）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyRevokeCmd {

    /**
     * 吊销幂等键（framework revoke 幂等键，结果未知以原键对账）
     */
    private String requestId;

    /**
     * framework/KMS 不透明标识
     */
    private String keyId;

    /**
     * 吊销原因
     */
    private String reason;

    /**
     * 操作人ID（管理审计）
     */
    private String operatorId;
}
