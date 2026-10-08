package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessKeyState 值对象单元测试（CR-055）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
class BusinessKeyStateTest {

    @Test
    void valOf_roundTrip() {
        for (BusinessKeyState state : BusinessKeyState.values()) {
            assertEquals(state, BusinessKeyState.valOf(state.getValue()));
        }
        assertNull(BusinessKeyState.valOf("UNKNOWN"));
        assertNull(BusinessKeyState.valOf(null));
    }

    @Test
    void isActive_onlyActive() {
        assertTrue(BusinessKeyState.ACTIVE.isActive());
        assertFalse(BusinessKeyState.PENDING.isActive());
        assertFalse(BusinessKeyState.DEPRECATED.isActive());
        assertFalse(BusinessKeyState.REVOKED.isActive());
    }

    @Test
    void canDecrypt_activeOrDeprecated() {
        assertTrue(BusinessKeyState.ACTIVE.canDecrypt());
        assertTrue(BusinessKeyState.DEPRECATED.canDecrypt());
        assertFalse(BusinessKeyState.PENDING.canDecrypt());
        assertFalse(BusinessKeyState.REVOKING.canDecrypt());
        assertFalse(BusinessKeyState.REVOKED.canDecrypt());
        assertFalse(BusinessKeyState.EXPIRED.canDecrypt());
        assertFalse(BusinessKeyState.FAILED.canDecrypt());
        assertFalse(BusinessKeyState.RECONCILE_REQUIRED.canDecrypt());
    }
}
