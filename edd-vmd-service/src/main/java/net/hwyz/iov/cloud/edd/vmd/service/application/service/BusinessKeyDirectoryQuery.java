package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * 业务密钥目录查询上下文（VMD 内部）
 * <p>
 * CR-055 §7.2：由 framework {@code DeviceKeyContext} 经 {@link BusinessKeyBizTypeMapper}
 * 归一化后的 VMD 目录查询键 (device_sn, business_domain, purpose)。
 *
 * @param deviceSn       设备实例序列号
 * @param businessDomain 受治理业务域代码
 * @param purpose        受治理用途代码
 * @author hwyz_leo
 * @since 2026-10-08
 */
public record BusinessKeyDirectoryQuery(String deviceSn, String businessDomain, String purpose) {
}
