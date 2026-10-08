package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥在线申请命令（CR-055 §7.1）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisionCmd {

    /**
     * 调用幂等键（framework create 幂等键，同 requestId 重试幂等）
     */
    private String requestId;

    /**
     * 设备实例序列号（keyprov payload）
     */
    private String deviceSn;

    /**
     * 设备类别（如 TBOX）
     */
    private String deviceCategory;

    /**
     * 受治理业务域代码
     */
    private String businessDomain;

    /**
     * 受治理用途代码
     */
    private String purpose;

    /**
     * VAGW 认证会话对应的设备序列号（RD-055-8 双重校验；无设备会话时为空）
     * <p>非空且与 payload deviceSn 不一致 → 拒绝，framework 调用次数为 0。
     */
    private String sessionDeviceSn;
}
