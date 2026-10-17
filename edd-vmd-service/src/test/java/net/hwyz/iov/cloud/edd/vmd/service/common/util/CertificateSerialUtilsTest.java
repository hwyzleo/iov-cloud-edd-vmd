package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CertificateSerialUtils 单元测试（VMD-DSN-CR-060 §8.2）。
 * <p>
 * 校验 canonical 化严格委派 framework {@code PkiClient.canonicalSerial}（去分隔符 + 去 0x + 去前导零 + 小写），
 * 并对非十六进制历史脏数据提供 trim 兼容读回退。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
class CertificateSerialUtilsTest {

    @Test
    @DisplayName("canonical 委派 framework 规则：去冒号/空格/Tab、去 0x、去前导零、小写")
    void canonical_delegatesToFrameworkRule() {
        assertEquals("a0b0c", CertificateSerialUtils.canonical("0A:0B:0C"));
        assertEquals("a0b0c", CertificateSerialUtils.canonical("0A 0B 0C"));
        assertEquals("a0b0c", CertificateSerialUtils.canonical("0A\t0B\t0C"));
        assertEquals("a0b0c", CertificateSerialUtils.canonical("  0x0A0B0C  "));
        assertEquals("1", CertificateSerialUtils.canonical("01"));
        assertEquals("a0b0c", CertificateSerialUtils.canonical("0x0A0B0C"));
    }

    @Test
    @DisplayName("非十六进制序列号无法 canonical 化时回退 trim 原值（兼容读）")
    void canonical_nonHexadecimal_fallsBackToTrim() {
        assertEquals("CR055-CERT-0001", CertificateSerialUtils.canonical("  CR055-CERT-0001 "));
        assertEquals("CR058-CERT-0001", CertificateSerialUtils.canonical("CR058-CERT-0001"));
    }

    @Test
    @DisplayName("null/空串/纯空白原样返回，不抛异常")
    void canonical_nullOrBlank_passthrough() {
        assertNull(CertificateSerialUtils.canonical(null));
        assertEquals("", CertificateSerialUtils.canonical(""));
        assertEquals("   ", CertificateSerialUtils.canonical("   "));
    }
}
