package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VmdErrorCode 注册表校验（CR-040 延续 + CR-055 新增业务密钥域码）
 * <p>
 * 校验：806xxx 格式、唯一性、范围（806000 基准码 + 业务码 806001～806999）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
class VmdErrorCodeRegistryTest {

    @Test
    void allCodes_formatAndUnique() {
        Set<String> codes = new HashSet<>();
        for (VmdErrorCode errorCode : VmdErrorCode.values()) {
            assertTrue(ErrorCodeRegistry.isValidCode(errorCode.getCode()),
                    "错误码格式非法: " + errorCode.name() + "=" + errorCode.getCode());
            assertTrue(codes.add(errorCode.getCode()), "错误码重复: " + errorCode.getCode());
        }
    }

    @Test
    void registry_builtConsistently() {
        assertEquals(VmdErrorCode.values().length, ErrorCodeRegistry.getRegisteredCount());
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806065"));
        // CR-055 新增业务密钥域错误码
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806066"));
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806077"));
    }

    @Test
    void businessKeyErrorCodes_present() {
        assertEquals("806066", VmdErrorCode.BUSINESS_KEY_DEVICE_SESSION_MISMATCH.getCode());
        assertEquals("806067", VmdErrorCode.BUSINESS_KEY_DOMAIN_NOT_AUTHORIZED.getCode());
        assertEquals("806068", VmdErrorCode.BUSINESS_KEY_NOT_EXIST.getCode());
        assertEquals("806069", VmdErrorCode.BUSINESS_KEY_MULTIPLE_ACTIVE.getCode());
        assertEquals("806070", VmdErrorCode.BUSINESS_KEY_STATE_NOT_ALLOWED.getCode());
        assertEquals("806071", VmdErrorCode.BUSINESS_KEY_IDEMPOTENCY_CONFLICT.getCode());
        assertEquals("806072", VmdErrorCode.BUSINESS_KEY_ROTATION_CONFLICT.getCode());
        assertEquals("806073", VmdErrorCode.BUSINESS_KEY_DEVICE_CERT_NOT_FOUND.getCode());
        assertEquals("806074", VmdErrorCode.BUSINESS_KEY_KMS_UNAVAILABLE.getCode());
        assertEquals("806075", VmdErrorCode.BUSINESS_KEY_WRAP_FAILED.getCode());
        assertEquals("806076", VmdErrorCode.BUSINESS_KEY_REVOCATION_FAILED.getCode());
        assertEquals("806077", VmdErrorCode.BUSINESS_KEY_OUTCOME_UNKNOWN.getCode());
    }

    @Test
    void registry_lookup() {
        assertEquals(VmdErrorCode.BUSINESS_KEY_NOT_EXIST,
                ErrorCodeRegistry.findByCode("806068").orElse(null));
        assertTrue(ErrorCodeRegistry.findByCode("999999").isEmpty());
    }
}
