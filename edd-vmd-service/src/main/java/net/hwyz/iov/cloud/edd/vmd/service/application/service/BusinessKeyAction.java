package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.util.Locale;

/**
 * 业务密钥操作动作（VMD 授权维度）
 * <p>
 * CR-055 §3.3：BusinessKeyPolicyRegistry 管理各域/用途允许的操作集合。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public enum BusinessKeyAction {

    /**
     * 在线申请/下发（创建材料并设备证书 Wrap）
     */
    PROVISION,

    /**
     * 正向目录解析（resolveActive）
     */
    RESOLVE_ACTIVE,

    /**
     * 反向目录解析/解密（resolveByKeyId + DECRYPT）
     */
    DECRYPT,

    /**
     * 轮换（创建新材料并原子切换 ACTIVE）
     */
    ROTATE,

    /**
     * 吊销
     */
    REVOKE;

    public static BusinessKeyAction valOf(String val) {
        if (val == null) {
            return null;
        }
        return valueOf(val.toUpperCase(Locale.ROOT));
    }
}
