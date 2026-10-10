package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆导入补发动作注册表单元测试
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作注册表
 * <p>
 * 覆盖：按 ImportType 返回有序动作（生命周期 → 绑定 → 软件实装；PRODUCE 仅生产事件）、
 * 重复注册忽略、未登记查询返回 null。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@DisplayName("VehicleImportReplayActionRegistry 测试")
class VehicleImportReplayActionRegistryTest {

    @Test
    @DisplayName("按ImportType返回有序动作集合")
    void actionsForImportType() {
        VehicleImportReplayActionRegistry registry = new VehicleImportReplayActionRegistry(List.of(
                fakeAction("PRODUCE_EVENT"),
                fakeAction("TOL_LIFECYCLE_ENSURE"),
                fakeAction("EOL_LIFECYCLE_ENSURE"),
                fakeAction("BINDING_EVENT_REPLAY"),
                fakeAction("SOFTWARE_INVENTORY_EVENT_REPLAY")));

        List<String> produce = registry.actionsForImportType("PRODUCE").stream()
                .map(VehicleImportReplayAction::actionType).toList();
        assertEquals(List.of("PRODUCE_EVENT"), produce);

        List<String> tol = registry.actionsForImportType("TOL").stream()
                .map(VehicleImportReplayAction::actionType).toList();
        assertEquals(List.of("TOL_LIFECYCLE_ENSURE", "BINDING_EVENT_REPLAY", "SOFTWARE_INVENTORY_EVENT_REPLAY"), tol);

        List<String> eol = registry.actionsForImportType("EOL").stream()
                .map(VehicleImportReplayAction::actionType).toList();
        assertEquals(List.of("EOL_LIFECYCLE_ENSURE", "BINDING_EVENT_REPLAY", "SOFTWARE_INVENTORY_EVENT_REPLAY"), eol);
    }

    @Test
    @DisplayName("重复注册同类型动作应忽略")
    void duplicateRegistrationIgnored() {
        VehicleImportReplayAction first = fakeAction("PRODUCE_EVENT");
        VehicleImportReplayAction second = fakeAction("PRODUCE_EVENT");
        VehicleImportReplayActionRegistry registry = new VehicleImportReplayActionRegistry(List.of(first, second));
        assertSame(first, registry.getAction("PRODUCE_EVENT"));
    }

    @Test
    @DisplayName("未登记动作查询返回null")
    void unregisteredReturnsNull() {
        VehicleImportReplayActionRegistry registry = new VehicleImportReplayActionRegistry(List.of());
        assertNull(registry.getAction("UNKNOWN_ACTION"));
        assertFalse(registry.isRegistered("UNKNOWN_ACTION"));
    }

    private VehicleImportReplayAction fakeAction(String actionType) {
        return new VehicleImportReplayAction() {
            @Override
            public String actionType() {
                return actionType;
            }

            @Override
            public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
                return List.of();
            }

            @Override
            public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                           VehicleImportReplayActionTarget target) {
                return VehicleImportReplayActionResult.success();
            }
        };
    }
}
