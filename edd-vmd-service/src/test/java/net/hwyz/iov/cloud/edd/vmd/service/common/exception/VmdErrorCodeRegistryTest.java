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
        // CR-056 新增零件导入后置处理重放域错误码
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806078"));
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806079"));
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806080"));
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
    void partReplayErrorCodes_present() {
        assertEquals("806078", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_NOT_ALLOWED.getCode());
        assertEquals("806079", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_IN_PROGRESS.getCode());
        assertEquals("806080", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_ACTION_NOT_FOUND.getCode());
    }

    @Test
    void businessKeyProvisioningErrorCodes_present() {
        // CR-058 新增业务密钥多用途开通批次/设备导入ACK域错误码
        assertEquals("806083", VmdErrorCode.BUSINESS_KEY_PROVISIONING_NOT_FOUND.getCode());
        assertEquals("806084", VmdErrorCode.BUSINESS_KEY_PROVISIONING_PURPOSE_INCOMPLETE.getCode());
        assertEquals("806085", VmdErrorCode.BUSINESS_KEY_PROVISIONING_ACK_CONFLICT.getCode());
        assertEquals("806086", VmdErrorCode.BUSINESS_KEY_PROVISIONING_STATE_NOT_ALLOWED.getCode());
        assertEquals("806087", VmdErrorCode.BUSINESS_KEY_PROVISIONING_ITEM_MISMATCH.getCode());
        assertEquals("806088", VmdErrorCode.BUSINESS_KEY_PROVISIONING_DEVICE_NOT_ELIGIBLE.getCode());
        assertEquals("806089", VmdErrorCode.BUSINESS_KEY_PROVISIONING_EXPIRED.getCode());
        assertEquals(VmdErrorCode.BUSINESS_KEY_PROVISIONING_NOT_FOUND,
                ErrorCodeRegistry.findByCode("806083").orElse(null));
    }

    @Test
    void registry_lookup() {
        assertEquals(VmdErrorCode.BUSINESS_KEY_NOT_EXIST,
                ErrorCodeRegistry.findByCode("806068").orElse(null));
        assertTrue(ErrorCodeRegistry.findByCode("999999").isEmpty());
    }

    @Test
    void keyprovServiceErrorCodes_present() {
        // CR-059 官方 Keyprov Service 契约域错误码（806090～806100）
        assertEquals("806090", VmdErrorCode.KEYPROV_CALLER_UNAUTHORIZED.getCode());
        assertEquals("806091", VmdErrorCode.KEYPROV_DEVICE_CONTEXT_MISMATCH.getCode());
        assertEquals("806092", VmdErrorCode.KEYPROV_BINDING_INVALID.getCode());
        assertEquals("806093", VmdErrorCode.KEYPROV_CERTIFICATE_INVALID.getCode());
        assertEquals("806094", VmdErrorCode.KEYPROV_REQUEST_INVALID.getCode());
        assertEquals("806095", VmdErrorCode.KEYPROV_IDEMPOTENCY_CONFLICT.getCode());
        assertEquals("806096", VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE.getCode());
        assertEquals("806097", VmdErrorCode.KEYPROV_WRAP_FAILED.getCode());
        assertEquals("806098", VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN.getCode());
        assertEquals("806099", VmdErrorCode.KEYPROV_BATCH_NOT_FOUND.getCode());
        assertEquals("806100", VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE.getCode());
        assertEquals(VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE,
                ErrorCodeRegistry.findByCode("806100").orElse(null));
    }

    @Test
    void keyprovCertificateResolutionErrorCodes_present() {
        // CR-060 framework 证书材料解析与 Provider 就绪门禁域错误码（806101～806105）
        assertEquals("806101", VmdErrorCode.KEYPROV_CERTIFICATE_MATERIAL_UNAVAILABLE.getCode());
        assertEquals("806102", VmdErrorCode.KEYPROV_CERTIFICATE_EVIDENCE_MISMATCH.getCode());
        assertEquals("806103", VmdErrorCode.KEYPROV_CERTIFICATE_REFERENCE_AMBIGUOUS.getCode());
        assertEquals("806104", VmdErrorCode.KEYPROV_CERTIFICATE_POLICY_REJECTED.getCode());
        assertEquals("806105", VmdErrorCode.KEYPROV_PROVIDER_NOT_READY.getCode());
        assertEquals(VmdErrorCode.KEYPROV_CERTIFICATE_POLICY_REJECTED,
                ErrorCodeRegistry.findByCode("806104").orElse(null));
    }

    @Test
    void keyprovDerivedRootErrorCodes_present() {
        // CR-061 V2C 根派生域错误码（806106～806108）
        assertEquals("806106", VmdErrorCode.KEYPROV_VIN_BINDING_INVALID.getCode());
        assertEquals("806107", VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY.getCode());
        assertEquals("806108", VmdErrorCode.KEYPROV_DERIVATION_UNSUPPORTED.getCode());
        assertEquals(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY,
                ErrorCodeRegistry.findByCode("806107").orElse(null));
    }

    @Test
    void tboxEcdhCertificateErrorCodes_present() {
        // VMD-DSN-CR-063 逻辑 TBOX ECDH-ES v1 证书 Profile 域错误码（806112～806121）
        assertEquals("806112", VmdErrorCode.CERTIFICATE_CSR_PUBLIC_KEY_NOT_ALLOWED.getCode());
        assertEquals("806113", VmdErrorCode.CERTIFICATE_CSR_KEY_USAGE_INVALID.getCode());
        assertEquals("806114", VmdErrorCode.CERTIFICATE_CSR_CLIENT_AUTH_MISSING.getCode());
        assertEquals("806115", VmdErrorCode.CERTIFICATE_CSR_EKU_CONFLICT.getCode());
        assertEquals("806116", VmdErrorCode.CERTIFICATE_CSR_UNSUPPORTED_CRITICAL_EXTENSION.getCode());
        assertEquals("806117", VmdErrorCode.CERTIFICATE_PUBLIC_KEY_BINDING_MISMATCH.getCode());
        assertEquals("806118", VmdErrorCode.CERTIFICATE_UID_BINDING_MISMATCH.getCode());
        assertEquals("806119", VmdErrorCode.CERTIFICATE_ISSUED_PROFILE_MISMATCH.getCode());
        assertEquals("806120", VmdErrorCode.CERTIFICATE_MATERIAL_UNAVAILABLE.getCode());
        assertEquals("806121", VmdErrorCode.CERTIFICATE_ENROLLMENT_OUTCOME_UNKNOWN.getCode());
        assertEquals(VmdErrorCode.CERTIFICATE_ISSUED_PROFILE_MISMATCH,
                ErrorCodeRegistry.findByCode("806119").orElse(null));
    }
}
