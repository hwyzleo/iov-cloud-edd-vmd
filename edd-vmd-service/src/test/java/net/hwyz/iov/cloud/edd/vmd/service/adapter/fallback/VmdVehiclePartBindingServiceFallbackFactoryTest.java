package net.hwyz.iov.cloud.edd.vmd.service.adapter.fallback;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehiclePartBindingServiceFallbackFactory;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehiclePartBindingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 车辆-零件绑定服务降级工厂单元测试（VMD-DSN-CR-076）
 * <p>
 * 覆盖新增 getVehicleByDevice 的 Fallback 分支，并回归既有方法降级行为。
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
class VmdVehiclePartBindingServiceFallbackFactoryTest {

    @Test
    @DisplayName("getVehicleByDevice 降级：远端失败时抛出运行时异常，不伪造绑定结论")
    void shouldThrowWhenVehicleByDeviceDegraded() {
        VmdVehiclePartBindingService service =
                new VmdVehiclePartBindingServiceFallbackFactory().create(new RuntimeException("remote down"));

        assertThrows(RuntimeException.class,
                () -> service.getVehicleByDevice("IVI0001ABCD0001", "IVI"));
    }

    @Test
    @DisplayName("getVehicleByDevice 降级：设备标识过短也安全脱敏（不抛脱敏异常）")
    void shouldMaskShortDeviceIdWithoutError() {
        VmdVehiclePartBindingService service =
                new VmdVehiclePartBindingServiceFallbackFactory().create(new RuntimeException("remote down"));

        assertThrows(RuntimeException.class,
                () -> service.getVehicleByDevice("SN1", "IVI"));
    }

    @Test
    @DisplayName("既有契约降级：listActiveBindingsByVin / listBindingsForBootstrap 行为不变")
    void shouldKeepExistingFallbackBehavior() {
        VmdVehiclePartBindingService service =
                new VmdVehiclePartBindingServiceFallbackFactory().create(new RuntimeException("remote down"));

        assertThrows(RuntimeException.class, () -> service.listActiveBindingsByVin("LSVAA1234567890AB"));
        assertThrows(RuntimeException.class, () -> service.listBindingsForBootstrap(null, 100));
    }

}
