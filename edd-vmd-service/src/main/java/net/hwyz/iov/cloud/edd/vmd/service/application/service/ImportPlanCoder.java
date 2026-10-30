package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 导入操作计划规范编码器（CR-072 §预览安全）
 * <p>
 * planDigest 必须采用确定性内容：固定候选排序、字段序列化、NULL/空串表达与计划 schema 版本；
 * resolvedAt/plannedAt 等每次变化的运行时间不参与摘要（实际解析时间写审计）。
 * 序列化为 canonical JSON：递归排序键、无空白、显式 null（JSON null 表达"要求当前为 NULL"）。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
public final class ImportPlanCoder {

    /** 计划 schema 版本（category plan 生效标记；老操作无此版本 = 不获得类别补缺能力） */
    public static final String SCHEMA_VERSION = "cr072-category-v1";

    private ImportPlanCoder() {
    }

    /**
     * 生成确定性 canonical JSON（排序键、无空白、显式 null）
     *
     * @param map 计划对象
     * @return canonical JSON 字符串
     */
    public static String toCanonicalJson(Map<String, Object> map) {
        return canonical(map);
    }

    /**
     * 节点内容指纹（无 externalVersion 时）：sha256(nodeCode + ":" + deviceCategory)，可比较、不假造版本
     */
    public static String nodeFingerprint(String nodeCode, String deviceCategory) {
        if (nodeCode == null) {
            return null;
        }
        return hex(sha256((nodeCode + ":" + (deviceCategory != null ? deviceCategory : "")).getBytes(StandardCharsets.UTF_8)));
    }

    private static String canonical(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return quote(s);
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> rawMap) {
            // 排序键保证确定性；显式保留 null 值
            TreeMap<String, Object> sorted = new TreeMap<>();
            rawMap.forEach((k, v) -> sorted.put(String.valueOf(k), v));
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(quote(e.getKey())).append(":").append(canonical(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(canonical(item));
            }
            return sb.append("]").toString();
        }
        // 其他类型（含 enum/日期）按字符串序列化，保证确定性
        return quote(String.valueOf(value));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"").toString();
    }

    private static byte[] sha256(byte[] raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 供测试使用：列表排序稳定化 */
    static List<String> sortedCopy(List<String> values) {
        List<String> copy = new ArrayList<>(values);
        copy.sort(String::compareTo);
        return copy;
    }

}
