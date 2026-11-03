package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VmdErrorCode 注册表校验（CR-040 延续）
 * <p>
 * 校验：806xxx 格式、唯一性、范围（806000 基准码 + 业务码 806001～806999）。
 * <p>
 * VMD-DSN-CR-074：证书/运行期业务密钥/Keyprov 错误码（806040～806052、806060～806065、
 * 806066～806077、806083～806089、806090～806131 中除 806106 外）已随迁出退役，退役编号
 * 登记于 {@link VmdErrorCode} 类注释，禁止重新分配；本测试只校验保留业务错误码。
 *
 * @author hwyz_leo
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
        // 保留业务错误码（CR-056 零件导入后置处理重放域）
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806078"));
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806079"));
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806080"));
        // CR-072 device_category 最小快照域
        assertTrue(ErrorCodeRegistry.getAllCodes().contains("806132"));
    }

    @Test
    void partReplayErrorCodes_present() {
        assertEquals("806078", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_NOT_ALLOWED.getCode());
        assertEquals("806079", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_IN_PROGRESS.getCode());
        assertEquals("806080", VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_ACTION_NOT_FOUND.getCode());
    }

    @Test
    void deviceCategoryErrorCodes_present() {
        assertEquals("806132", VmdErrorCode.DEVICE_CATEGORY_NODE_CONFLICT.getCode());
        assertEquals("806133", VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED.getCode());
        assertEquals("806134", VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode());
        assertEquals("806135", VmdErrorCode.DEVICE_CATEGORY_CORRECTION_NOT_SUPPORTED.getCode());
        assertEquals("806136", VmdErrorCode.DEVICE_CATEGORY_DISABLED.getCode());
    }

    @Test
    void softwareAndSecurityPresetErrorCodes_present() {
        // 软件实装域（保留）
        assertEquals("806053", VmdErrorCode.SOFTWARE_MANIFEST_INVALID.getCode());
        assertEquals("806057", VmdErrorCode.SOFTWARE_IDEMPOTENCY_CONFLICT.getCode());
        // 产线安全常量预置域（保留）
        assertEquals("806058", VmdErrorCode.SECURITY_PRESET_INVALID_CAPABILITY.getCode());
        assertEquals("806059", VmdErrorCode.SECURITY_PRESET_BIZ_TYPE_UNRESOLVED.getCode());
    }

    @Test
    void vinInvalidErrorCode_retainedForVinCanonicalizer() {
        // CR-074 迁出后：806106 KEYPROV_VIN_BINDING_INVALID 由 VMD 保留业务 VIN 规范化继续复用
        assertEquals("806106", VmdErrorCode.KEYPROV_VIN_BINDING_INVALID.getCode());
        assertEquals(VmdErrorCode.KEYPROV_VIN_BINDING_INVALID,
                ErrorCodeRegistry.findByCode("806106").orElse(null));
    }

    @Test
    void retiredCodes_notRegistered() {
        // CR-074 退役编号不得再活动注册（抽查：证书 806040 / 业务密钥 806066 / Keyprov 806090）
        assertTrue(ErrorCodeRegistry.findByCode("806040").isEmpty());
        assertTrue(ErrorCodeRegistry.findByCode("806066").isEmpty());
        assertTrue(ErrorCodeRegistry.findByCode("806090").isEmpty());
        assertTrue(ErrorCodeRegistry.findByCode("806131").isEmpty());
        // 未分配编号也不存在
        assertTrue(ErrorCodeRegistry.findByCode("999999").isEmpty());
    }

    @Test
    void registry_lookup() {
        assertEquals(VmdErrorCode.SOFTWARE_MANIFEST_INVALID,
                ErrorCodeRegistry.findByCode("806053").orElse(null));
    }
}
