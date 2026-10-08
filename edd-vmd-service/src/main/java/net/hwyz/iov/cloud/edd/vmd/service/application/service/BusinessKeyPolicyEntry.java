package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.time.Duration;
import java.util.Set;

/**
 * 业务密钥策略条目（版本化，不可变）
 * <p>
 * CR-055 §3.3：管理调用方、设备类别、业务域、用途、允许操作、算法、有效期和重叠窗口。
 * 同一 (businessDomain, purpose) 唯一对应一条策略；未登记的域/用途一律视为未授权（fail-closed）。
 *
 * @param businessDomain       受治理业务域代码
 * @param purpose              受治理用途代码
 * @param deviceCategories     允许的设备类别（如 TBOX）
 * @param allowedActions       允许的操作集合
 * @param algorithm            密钥算法（透传 KMS，如 AES）
 * @param keySpec              密钥规格（如 AES-256-GCM）
 * @param validity             密钥有效期（驱动 valid_to）
 * @param decryptWindow        DEPRECATED 仅解密重叠窗口（驱动 decrypt_until = deprecateTime + window）
 * @param policyVersion        策略版本（创建时快照）
 * @author hwyz_leo
 * @since 2026-10-08
 */
public record BusinessKeyPolicyEntry(
        String businessDomain,
        String purpose,
        Set<String> deviceCategories,
        Set<BusinessKeyAction> allowedActions,
        String algorithm,
        String keySpec,
        Duration validity,
        Duration decryptWindow,
        String policyVersion
) {

    public boolean allowsAction(BusinessKeyAction action) {
        return allowedActions != null && allowedActions.contains(action);
    }

    public boolean allowsDeviceCategory(String deviceCategory) {
        if (deviceCategories == null || deviceCategories.isEmpty()) {
            return true;
        }
        if (deviceCategory == null) {
            // 运行时目录解析无设备类别维度：创建时（PROVISION）已校验类别，此处放行
            return true;
        }
        return deviceCategories.contains(deviceCategory);
    }
}
