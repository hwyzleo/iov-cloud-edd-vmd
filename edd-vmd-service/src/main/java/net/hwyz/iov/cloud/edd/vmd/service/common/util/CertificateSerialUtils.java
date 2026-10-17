package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import net.hwyz.iov.cloud.framework.security.crypto.client.PkiClient;

/**
 * 证书序列号 canonical 化工具（VMD-DSN-CR-060 §8.2）。
 * <p>
 * 入站、仓储写入、查询条件、Recipient、expectedSerial 与日志摘要前统一使用 framework
 * {@link PkiClient#canonicalSerial(String)} 作为唯一 canonical 规则，禁止在 VMD 侧自定义
 * “去冒号/转大写/去前导零”等并行演化规则。
 * <p>
 * 兼容读：历史 {@code tb_veh_certificate.cert_sn} 可能并非 canonical 十六进制（例如测试基线伪
 * serial），此时无法 canonical 化，回退为去除首尾空白后的原值，保证旧数据仍可查询；canonical
 * 一致性审计与历史回填由独立数据修复脚本/发布阻断项完成（CR-060 §4.1/§8.2）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
public final class CertificateSerialUtils {

    private CertificateSerialUtils() {
    }

    /**
     * canonical 化证书序列号。
     * <p>
     * 委派 framework {@link PkiClient#canonicalSerial(String)}；当序列号无法解析为十六进制
     * （历史/脏数据）时回退为 trim 后的原值，避免查询与登记链路因非 canonical 数据硬失败。
     *
     * @param serial 原始序列号，可为 null
     * @return canonical 序列号；入参为 null/空白时原样返回；无法 canonical 化时返回 trim 原值
     */
    public static String canonical(String serial) {
        if (serial == null || serial.isBlank()) {
            return serial;
        }
        try {
            return PkiClient.canonicalSerial(serial);
        } catch (IllegalArgumentException e) {
            return serial.trim();
        }
    }
}
