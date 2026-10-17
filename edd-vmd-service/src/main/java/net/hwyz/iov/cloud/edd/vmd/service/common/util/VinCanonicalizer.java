package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import cn.hutool.crypto.digest.DigestUtil;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VinInvalidException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VIN 规范化与校验（VMD-DSN-CR-061 RD-061-2 / FW-SEC-DSN-CR-013 §6）。
 * <p>
 * 规范化口径与 framework {@code KeyAnchor.vin(...)} 一致：{@code trim → 大写(Locale.ROOT) →
 * 长度恰 17 → 字符集 [A-HJ-NPR-Z0-9]}（ISO 3779，排除 I/O/Q）。VMD 在调用 framework 前自行
 * fail-closed，避免把非法锚带进派生流程。
 * <p>
 * <strong>安全边界</strong>：本类不产生也不记录 VIN 原文；{@link #sha256Hex(String)} 仅用于
 * 审计/幂等摘要，绝不替代派生的锚原文。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
public final class VinCanonicalizer {

    /** 规范化后的 VIN 长度 */
    public static final int VIN_LENGTH = 17;

    /** ISO 3779：大写字母 + 数字，排除 I/O/Q */
    private static final Pattern VIN_PATTERN = Pattern.compile("[A-HJ-NPR-Z0-9]{" + VIN_LENGTH + "}");

    private VinCanonicalizer() {
    }

    /**
     * 规范化并校验 VIN；非法即 fail-closed。
     *
     * @param vin 原始 VIN（可为 null）
     * @return 规范化后的 17 位大写 VIN
     * @throws VinInvalidException 非空但不符合规范化规则（异常正文不含 VIN 原文）
     */
    public static String normalizeAndValidate(String vin) {
        if (vin == null) {
            throw new VinInvalidException("VIN 缺失");
        }
        String normalized = vin.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != VIN_LENGTH) {
            throw new VinInvalidException("VIN 规范化后长度非法（要求 " + VIN_LENGTH + " 位）");
        }
        if (!VIN_PATTERN.matcher(normalized).matches()) {
            throw new VinInvalidException("VIN 规范化后字符集非法（要求 [A-HJ-NPR-Z0-9]）");
        }
        return normalized;
    }

    /**
     * 是否合法 VIN（非抛异常判定）。
     */
    public static boolean isValid(String vin) {
        if (vin == null) {
            return false;
        }
        String normalized = vin.trim().toUpperCase(Locale.ROOT);
        return normalized.length() == VIN_LENGTH && VIN_PATTERN.matcher(normalized).matches();
    }

    /**
     * 规范化 VIN 的 SHA-256 摘要（十六进制小写），用于审计/幂等摘要，不参与派生。
     *
     * @param vin 原始 VIN；先规范化
     * @return sha256 十六进制摘要
     * @throws VinInvalidException VIN 非法
     */
    public static String sha256Hex(String vin) {
        String normalized = normalizeAndValidate(vin);
        return DigestUtil.sha256Hex(normalized.getBytes(StandardCharsets.UTF_8));
    }
}