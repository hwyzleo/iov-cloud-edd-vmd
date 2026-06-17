package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConstantStateTest {

    @Test
    void valOf_validValue_returnsEnum() {
        assertEquals(SecurityConstantState.PENDING, SecurityConstantState.valOf("PENDING"));
        assertEquals(SecurityConstantState.PRESET, SecurityConstantState.valOf("PRESET"));
        assertEquals(SecurityConstantState.FAILED, SecurityConstantState.valOf("FAILED"));
    }

    @Test
    void valOf_invalidValue_returnsNull() {
        assertNull(SecurityConstantState.valOf("INVALID"));
        assertNull(SecurityConstantState.valOf(""));
        assertNull(SecurityConstantState.valOf(null));
    }

    @Test
    void getValue_returnsCorrectValue() {
        assertEquals("PENDING", SecurityConstantState.PENDING.getValue());
        assertEquals("PRESET", SecurityConstantState.PRESET.getValue());
        assertEquals("FAILED", SecurityConstantState.FAILED.getValue());
    }

    @Test
    void getLabel_returnsCorrectLabel() {
        assertEquals("待预置", SecurityConstantState.PENDING.getLabel());
        assertEquals("已预置", SecurityConstantState.PRESET.getLabel());
        assertEquals("预置失败", SecurityConstantState.FAILED.getLabel());
    }
}
