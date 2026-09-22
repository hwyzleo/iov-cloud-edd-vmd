package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HsmUidFieldResolver 单元测试（CR-049 §4.2）
 * <p>
 * 本期 HSM UID 字段名统一默认 HSM（RD-049-3），未来 MDM 增列可接入配置字段。
 * </p>
 *
 * @author hwyz_leo
 */
@DisplayName("HsmUidFieldResolver 测试")
class HsmUidFieldResolverTest {

    private final HsmUidFieldResolver resolver = new HsmUidFieldResolver();

    @Test
    @DisplayName("默认应返回 HSM")
    void default_shouldReturnHsm() {
        assertEquals("HSM", resolver.resolve(null));
        assertEquals("HSM", resolver.resolve(""));
        assertEquals("HSM", resolver.resolve("   "));
        assertEquals("HSM", HsmUidFieldResolver.DEFAULT_HSM_UID_FIELD);
    }

    @Test
    @DisplayName("MDM 配置字段非空时优先使用配置值（未来扩展位）")
    void configuredField_shouldBeUsedWhenPresent() {
        assertEquals("chipUid", resolver.resolve("chipUid"));
        assertEquals("hsm_uid", resolver.resolve("hsm_uid"));
    }
}
