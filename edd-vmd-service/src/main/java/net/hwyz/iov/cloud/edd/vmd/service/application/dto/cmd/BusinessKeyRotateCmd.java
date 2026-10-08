package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥轮换命令（CR-055 F20 §6.4，管理接口）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyRotateCmd {

    /**
     * 轮换幂等键（新密钥 create 幂等键）
     */
    private String requestId;

    /**
     * 设备实例序列号
     */
    private String deviceSn;

    /**
     * 设备类别
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
     * 操作人ID（管理审计）
     */
    private String operatorId;
}
