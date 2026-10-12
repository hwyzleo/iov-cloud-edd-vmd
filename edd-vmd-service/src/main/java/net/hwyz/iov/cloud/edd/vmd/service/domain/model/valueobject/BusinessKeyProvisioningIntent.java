package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import java.util.Locale;

/**
 * 开通批次目标版本内部模式（CR-058 D35）
 * <p>
 * 由 VMD 依据 canonical {@code KeyProvisioningRequest.current_versions} + 当前 ACTIVE 目录 +
 * 服务端轮换策略推导，不出现在车云 wire（canonical proto 无 intent 字段）：
 * <ul>
 *   <li>INITIAL：无 ACTIVE，创建新 PENDING 版本</li>
 *   <li>REIMPORT：设备声明的当前版本 == 目录 ACTIVE 版本（丢失/换证/恢复重导入），复用 ACTIVE 行重新 Wrap，不新建版本</li>
 *   <li>ROTATE：设备声明版本缺失/落后于 ACTIVE（升级/轮换），创建新 PENDING 版本，完成时旧 ACTIVE 转 DEPRECATED</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public enum BusinessKeyProvisioningIntent {

    INITIAL,
    REIMPORT,
    ROTATE;

    public static BusinessKeyProvisioningIntent valOf(String val) {
        if (val == null) {
            return null;
        }
        return valueOf(val.trim().toUpperCase(Locale.ROOT));
    }
}
