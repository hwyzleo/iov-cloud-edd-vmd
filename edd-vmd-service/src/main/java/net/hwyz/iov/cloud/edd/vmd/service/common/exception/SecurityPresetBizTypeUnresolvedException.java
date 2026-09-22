package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 安全常量预置 BizType 不可解析异常（CR-049 / RD-049-5）
 * <p>
 * 判定为需要预置但 deviceCategory 与旧节点码均无法解析 BizType 时抛出，
 * 复用导入失败语义：批次未处理 + 备注错误 + 失败计数 + 允许重导，不允许静默成功。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
public class SecurityPresetBizTypeUnresolvedException extends VmdBaseException {

    public SecurityPresetBizTypeUnresolvedException(String deviceCategory, String nodeCode) {
        super(VmdErrorCode.SECURITY_PRESET_BIZ_TYPE_UNRESOLVED,
                "deviceCategory=" + deviceCategory + ", nodeCode=" + nodeCode);
    }
}
