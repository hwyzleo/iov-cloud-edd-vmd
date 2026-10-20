package net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile;

import java.util.Locale;
import java.util.Optional;

/**
 * VMD 逻辑证书 Profile（VMD-DSN-CR-063 §4.1）。
 * <p>
 * VMD API 与领域模型只暴露逻辑 Profile 枚举，不接受物理 Provisioner / kid / 模板路径 /
 * Token / CA URL / 签发凭据。外部字符串经严格枚举解析，未知值 fail-closed 拒绝。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
public enum VmdCertificateProfile {

    /** 存量 sign-only 设备身份证书 Profile（mTLS 兼容，keyprovEligible=false，CR-044/054） */
    TBOX_TSP_CLIENT,

    /** 逻辑 TBOX ECDH-ES P-256 v1 Profile（CR-063；FW-SEC CR-017 validator，CR-062 Keyprov 前置门禁） */
    TBOX_DEVICE_ECDH_ES_P256_V1;

    /**
     * 严格解析外部字符串（去除两端空白后精确匹配枚举名；不 trim 内部、不大小写折叠）。
     *
     * @param value 外部 Profile 字符串
     * @return 解析结果；未知值返回 {@link Optional#empty()}
     */
    public static Optional<VmdCertificateProfile> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 严格解析并把未知/空值格式化为枚举名（供日志审计，不含敏感信息）
     */
    public static String describe(String value) {
        return parse(value).map(Enum::name).orElse("UNKNOWN:" + (value == null ? "" : value.toUpperCase(Locale.ROOT)));
    }
}
