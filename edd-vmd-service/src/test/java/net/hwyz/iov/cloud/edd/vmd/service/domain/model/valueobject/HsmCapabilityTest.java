package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HsmCapability 枚举边界测试（CR-049）
 * <p>
 * 值域 NONE/SHE/HSM_LIGHT/HSM_FULL；未知枚举抛契约错误，不得静默转换为 NONE（RD-049-2）。
 * </p>
 *
 * @author hwyz_leo
 */
@DisplayName("HsmCapability 枚举测试")
class HsmCapabilityTest {

    @Test
    @DisplayName("值域应为 NONE/SHE/HSM_LIGHT/HSM_FULL")
    void shouldHaveFourValues() {
        assertEquals(4, HsmCapability.values().length);
        assertNotNull(HsmCapability.NONE);
        assertNotNull(HsmCapability.SHE);
        assertNotNull(HsmCapability.HSM_LIGHT);
        assertNotNull(HsmCapability.HSM_FULL);
    }

    @Test
    @DisplayName("严格解析应支持合法值与大小写兼容")
    void fromRawValue_shouldParseValidValues() {
        assertEquals(HsmCapability.NONE, HsmCapability.fromRawValue("NONE"));
        assertEquals(HsmCapability.SHE, HsmCapability.fromRawValue("SHE"));
        assertEquals(HsmCapability.HSM_LIGHT, HsmCapability.fromRawValue("HSM_LIGHT"));
        assertEquals(HsmCapability.HSM_FULL, HsmCapability.fromRawValue("HSM_FULL"));
        assertEquals(HsmCapability.HSM_FULL, HsmCapability.fromRawValue("hsm_full"));
        assertEquals(HsmCapability.NONE, HsmCapability.fromRawValue(" NONE "));
    }

    @Test
    @DisplayName("null/空白应返回 null（由调用方走兼容兜底）")
    void fromRawValue_shouldReturnNullForBlank() {
        assertNull(HsmCapability.fromRawValue(null));
        assertNull(HsmCapability.fromRawValue(""));
        assertNull(HsmCapability.fromRawValue("   "));
    }

    @Test
    @DisplayName("未知枚举应抛异常，不得转换为 NONE（RD-049-2）")
    void fromRawValue_shouldThrowOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> HsmCapability.fromRawValue("HSM_SUPER"));
        assertThrows(IllegalArgumentException.class, () -> HsmCapability.fromRawValue("LOW"));
    }
}
