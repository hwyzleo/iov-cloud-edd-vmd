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
        assertFalse(BusinessKeyState.PENDING_RECOVERY.canDecrypt());
        assertFalse(BusinessKeyState.SUPERSEDED.canDecrypt());
    }

    @Test
    void cr066_superseded_isTerminalUnusable_andNotActive() {
        // CR-066 §6.4：SUPERSEDED 为终态、不可用，不参与 ACTIVE/PENDING/轮换候选/目录解析
        assertTrue(BusinessKeyState.SUPERSEDED.isTerminalUnusable());
        assertFalse(BusinessKeyState.SUPERSEDED.isActive());
        assertFalse(BusinessKeyState.SUPERSEDED.reusableForClaim());
        assertFalse(BusinessKeyState.SUPERSEDED.isActive());
    }

    @Test
    void cr066_pendingRecovery_isRecoverableIntermediate() {
        // CR-066 §6.4：PENDING_RECOVERY 为 FAILED→PENDING 恢复中间态，可推进、可 claim 复用
        assertFalse(BusinessKeyState.PENDING_RECOVERY.isTerminalUnusable());
        assertTrue(BusinessKeyState.PENDING_RECOVERY.reusableForClaim());
        assertFalse(BusinessKeyState.PENDING_RECOVERY.isActive());
    }

    @Test
    void cr066_reusableForClaim_matrix() {
        // CR-066 §8/§6.3：可复用 = ACTIVE/PENDING/FAILED/RECONCILE_REQUIRED
        assertTrue(BusinessKeyState.ACTIVE.reusableForClaim());
        assertTrue(BusinessKeyState.PENDING.reusableForClaim());
        assertTrue(BusinessKeyState.FAILED.reusableForClaim());
        assertTrue(BusinessKeyState.RECONCILE_REQUIRED.reusableForClaim());
        // 其余不可复用
        assertFalse(BusinessKeyState.DEPRECATED.reusableForClaim());
        assertFalse(BusinessKeyState.REVOKING.reusableForClaim());
        assertFalse(BusinessKeyState.REVOKED.reusableForClaim());
        assertFalse(BusinessKeyState.EXPIRED.reusableForClaim());
    }
}
