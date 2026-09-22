package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 器件 HSM 能力值非法异常（CR-049 / RD-049-2）
 * <p>
 * MDM hsmCapability 为未知枚举（主数据/契约错误）时抛出，
 * 不得降级为 NONE 或静默跳过；进入既有预置失败语义（批次未处理 + 备注错误 + 失败计数）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
public class SecurityPresetInvalidCapabilityException extends VmdBaseException {

    public SecurityPresetInvalidCapabilityException(String rawValue, String nodeCode) {
        super(VmdErrorCode.SECURITY_PRESET_INVALID_CAPABILITY,
                "rawValue=" + rawValue + ", nodeCode=" + nodeCode);
    }
}
