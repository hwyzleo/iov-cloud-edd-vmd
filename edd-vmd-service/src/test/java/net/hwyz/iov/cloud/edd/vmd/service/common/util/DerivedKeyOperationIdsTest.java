package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DerivedKeyOperationIds 单元测试（VMD-DSN-CR-061 §4.6/§8）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
class DerivedKeyOperationIdsTest {

    @Test
    @DisplayName("operationId 同 (requestId, purpose, version) 恒定，不引入随机/时间因子")
    void operationId_deterministic() {
        String a = DerivedKeyOperationIds.operationId("REQ-1", "UPLINK_AEAD", 2L);
        String b = DerivedKeyOperationIds.operationId("REQ-1", "UPLINK_AEAD", 2L);
        assertEquals(a, b);
        assertNotEquals(a, DerivedKeyOperationIds.operationId("REQ-1", "DOWNLINK_AEAD", 2L));
        assertNotEquals(a, DerivedKeyOperationIds.operationId("REQ-1", "UPLINK_AEAD", 3L));
        assertNotEquals(a, DerivedKeyOperationIds.operationId("REQ-2", "UPLINK_AEAD", 2L));
    }

    @Test
    @DisplayName("idempotencyKey 同上下文恒定，且含 deviceSn/derivationVersion 维度")
    void idempotencyKey_deterministic() {
        String a = DerivedKeyOperationIds.idempotencyKey("REQ-1", "SN1", "UPLINK_AEAD", 2L, "v1");
        String b = DerivedKeyOperationIds.idempotencyKey("REQ-1", "SN1", "UPLINK_AEAD", 2L, "v1");
        assertEquals(a, b);
        assertNotEquals(a, DerivedKeyOperationIds.idempotencyKey("REQ-1", "SN2", "UPLINK_AEAD", 2L, "v1"));
        assertNotEquals(a, DerivedKeyOperationIds.idempotencyKey("REQ-1", "SN1", "UPLINK_AEAD", 2L, "v2"));
    }

    @Test
    @DisplayName("null 安全：版本缺省按 0，文本缺省按空串")
    void nullSafe() {
        assertEquals(DerivedKeyOperationIds.operationId("REQ-1", "UPLINK_AEAD", null),
                DerivedKeyOperationIds.operationId("REQ-1", "UPLINK_AEAD", 0L));
        assertNotNull(DerivedKeyOperationIds.operationId(null, null, null));
        assertNotNull(DerivedKeyOperationIds.idempotencyKey(null, null, null, null, null));
    }
}
