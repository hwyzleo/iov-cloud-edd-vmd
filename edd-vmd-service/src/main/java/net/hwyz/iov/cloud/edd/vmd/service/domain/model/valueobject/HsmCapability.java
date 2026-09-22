package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

/**
 * 器件 HSM 能力枚举（CR-049）
 * <p>
 * 与 MDM 主数据 mdm_eead_vehicle_node.hsm_capability 值域对齐：
 * NONE / SHE / HSM_LIGHT / HSM_FULL。
 * 未知枚举按契约错误处理（抛异常），不得静默转换为 NONE（RD-049-2）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
public enum HsmCapability {

    /**
     * 无安全芯片能力
     */
    NONE,

    /**
     * SHE 安全硬件扩展（本期不触发器件级安全常量预置）
     */
    SHE,

    /**
     * HSM 轻量能力（触发器件级安全常量预置）
     */
    HSM_LIGHT,

    /**
     * HSM 完整能力（触发器件级安全常量预置）
     */
    HSM_FULL;

    /**
     * 严格解析原始值
     *
     * @param raw 原始字符串（MDM 透传值）
     * @return 对应枚举；null/空白返回 null（由调用方按兼容兜底处理）
     * @throws IllegalArgumentException 未知枚举值（契约错误，不得降级为 NONE）
     */
    public static HsmCapability fromRawValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        for (HsmCapability capability : values()) {
            if (capability.name().equalsIgnoreCase(trimmed)) {
                return capability;
            }
        }
        throw new IllegalArgumentException("未知的 HSM 能力枚举值: " + raw);
    }
}
