package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VinInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VinCanonicalizer 单元测试（VMD-DSN-CR-061 RD-061-2）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
class VinCanonicalizerTest {

    @Test
    @DisplayName("trim + 大写(Locale.ROOT) 后长度恰 17 且字符集合法 → 规范化成功")
    void normalizeAndValidate_valid() {
        assertEquals("HWYZTEST000000001", VinCanonicalizer.normalizeAndValidate("  hwyztest000000001  "));
        assertEquals("HWYZTEST000000001", VinCanonicalizer.normalizeAndValidate("HWYZTEST000000001"));
    }

    @Test
    @DisplayName("null / 长度非法 / 字符集含 I O Q / 小写后仍非法 → fail-closed")
    void normalizeAndValidate_invalid() {
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate(null));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate(""));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST00000000"));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST0000000012"));
        // I / O / Q 为 ISO 3779 排除字符
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST00000000I"));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST00000000O"));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST00000000Q"));
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.normalizeAndValidate("HWYZTEST0000000-1"));
    }

    @Test
    @DisplayName("异常正文不得包含 VIN 原文（安全边界）")
    void exceptionMessageDoesNotLeakVin() {
        String vin = "HWYZTEST00000000I";
        VinInvalidException ex = assertThrows(VinInvalidException.class,
                () -> VinCanonicalizer.normalizeAndValidate(vin));
        assertFalse(String.valueOf(ex.getMessage()).contains(vin), "异常正文不得包含 VIN 原文");
    }

    @Test
    @DisplayName("isValid 非抛异常判定与规范化同口径")
    void isValid() {
        assertTrue(VinCanonicalizer.isValid("hwyztest000000001"));
        assertTrue(VinCanonicalizer.isValid("  HWYZTEST000000001 "));
        assertFalse(VinCanonicalizer.isValid(null));
        assertFalse(VinCanonicalizer.isValid("HWYZTEST00000000"));
        assertFalse(VinCanonicalizer.isValid("HWYZTEST00000000I"));
    }

    @Test
    @DisplayName("sha256Hex 先规范化，大小写/空白差异得到相同摘要")
    void sha256Hex_normalizesBeforeHash() {
        String a = VinCanonicalizer.sha256Hex("HWYZTEST000000001");
        String b = VinCanonicalizer.sha256Hex("  hwyztest000000001 ");
        assertEquals(a, b);
        assertEquals(64, a.length());
        assertThrows(VinInvalidException.class, () -> VinCanonicalizer.sha256Hex("bad"));
    }
}
