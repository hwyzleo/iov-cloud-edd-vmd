package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

/**
 * 器件级安全常量预置判定结果（CR-049）
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
public enum SecurityPresetDecision {

    /**
     * 需要执行安全常量预置（HSM_LIGHT / HSM_FULL，或兼容兜底命中）
     */
    PRESET_REQUIRED,

    /**
     * 不需要执行安全常量预置（NONE / SHE，或兼容兜底未命中）
     */
    PRESET_NOT_REQUIRED,

    /**
     * 能力值非法（未知枚举，主数据/契约错误），进入失败分支，不得静默跳过
     */
    INVALID_CAPABILITY
}
