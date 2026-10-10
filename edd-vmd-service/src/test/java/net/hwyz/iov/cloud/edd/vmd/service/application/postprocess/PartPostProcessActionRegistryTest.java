package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零件导入后置处理动作注册表单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@DisplayName("PartPostProcessActionRegistry 测试")
class PartPostProcessActionRegistryTest {

    @Test
    @DisplayName("构造时自注册全部 Handler，可按动作类型发现")
    void registerAndGet() {
        List<PartPostProcessActionHandler> handlers = List.of(
                new PartInboundEventActionHandler(null),
                new BindingFactEventActionHandler(null),
                new TspSyncActionHandler(null),
                new OtaSyncActionHandler(null),
                new IdkSyncActionHandler(null),
                new SecurityPresetActionHandler(null, null, null, null, null));
        PartPostProcessActionRegistry registry = new PartPostProcessActionRegistry(handlers);

        assertTrue(registry.isRegistered("PART_INBOUND_EVENT"));
        assertTrue(registry.isRegistered("BINDING_FACT_EVENT"));
        assertTrue(registry.isRegistered("TSP_SYNC"));
        assertTrue(registry.isRegistered("OTA_SYNC"));
        assertTrue(registry.isRegistered("IDK_SYNC"));
        assertTrue(registry.isRegistered("SECURITY_PRESET"));
        assertNotNull(registry.getHandler("PART_INBOUND_EVENT"));
        assertNull(registry.getHandler("UNKNOWN_ACTION"));
    }

    @Test
    @DisplayName("同动作类型重复注册忽略并告警")
    void duplicateRegistrationIgnored() {
        List<PartPostProcessActionHandler> handlers = List.of(
                new PartInboundEventActionHandler(null),
                new PartInboundEventActionHandler(null));
        PartPostProcessActionRegistry registry = new PartPostProcessActionRegistry(handlers);
        // 不抛异常，后注册的被忽略
        assertNotNull(registry.getHandler("PART_INBOUND_EVENT"));
    }
}
