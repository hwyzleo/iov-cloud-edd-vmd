package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SecurityPresetBizTypeUnresolvedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.SecurityPresetMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityBizTypeResolver 路由单元测试（CR-049 §4.3）
 * <p>
 * deviceCategory 受控映射 → 旧节点码兜底 → 不可解析抛异常（RD-049-4/5）。
 * </p>
 *
 * @author hwyz_leo
 */
@DisplayName("SecurityBizTypeResolver 路由测试")
class SecurityBizTypeResolverTest {

    private VehicleNodeSchemaRegistry legacyRegistry;
    private SecurityPresetMetrics metrics;
    private SecurityBizTypeResolver resolver;

    @BeforeEach
    void setUp() {
        legacyRegistry = new VehicleNodeSchemaRegistry();
        metrics = mock(SecurityPresetMetrics.class);
        resolver = new SecurityBizTypeResolver(legacyRegistry, metrics);
    }

    @Test
    @DisplayName("deviceCategory=TBOX 应路由到 TBOX_DEVICE_ROOT")
    void categoryTbox_shouldRouteToTboxBizType() {
        assertEquals(BizType.TBOX_DEVICE_ROOT, resolver.resolve("TBOX", "TBOX_5G"));
    }

    @Test
    @DisplayName("deviceCategory=CCU 应路由到 CCU_DEVICE_ROOT（新变体无需发版）")
    void categoryCcu_shouldRouteToCcuBizType() {
        assertEquals(BizType.CCU_DEVICE_ROOT, resolver.resolve("CCU", "CCU_GEN2"));
    }

    @Test
    @DisplayName("deviceCategory=BTM 应路由到 PEPS_DEVICE_ROOT（既有 BTM 映射）")
    void categoryBtm_shouldRouteToPepsBizType() {
        assertEquals(BizType.PEPS_DEVICE_ROOT, resolver.resolve("BTM", "BTM"));
    }

    @Test
    @DisplayName("deviceCategory=CGW 应路由到 CGW_DEVICE_ROOT")
    void categoryCgw_shouldRouteToCgwBizType() {
        assertEquals(BizType.CGW_DEVICE_ROOT, resolver.resolve("CGW", "CGW"));
    }

    @Test
    @DisplayName("deviceCategory=DCU_COCKPIT 应路由到 CPT_DCU_DEVICE_ROOT（座舱域）")
    void categoryDcuCockpit_shouldRouteToCockpitBizType() {
        assertEquals(BizType.CPT_DCU_DEVICE_ROOT, resolver.resolve("DCU_COCKPIT", "DCU_COCKPIT"));
    }

    @Test
    @DisplayName("deviceCategory=DCU_ADAS 应路由到 AD_DCU_DEVICE_ROOT（智驾域，新变体无需发版）")
    void categoryDcuAdas_shouldRouteToAdasBizType() {
        assertEquals(BizType.AD_DCU_DEVICE_ROOT, resolver.resolve("DCU_ADAS", "DCU_ADAS_GEN1"));
    }

    @Test
    @DisplayName("deviceCategory=AD_DCU（framework 命名）应路由到 AD_DCU_DEVICE_ROOT")
    void categoryAdDcu_shouldRouteToAdasBizType() {
        assertEquals(BizType.AD_DCU_DEVICE_ROOT, resolver.resolve("AD_DCU", "DCU_ADAS_GEN1"));
    }

    @Test
    @DisplayName("无类别且旧节点码命中应兜底解析")
    void nullCategory_shouldFallbackToLegacyNodeCode() {
        assertEquals(BizType.TBOX_DEVICE_ROOT, resolver.resolve(null, "TBOX_5G"));
    }

    @Test
    @DisplayName("无受控映射类别且旧节点码命中应兜底解析并记录指标")
    void unmappedCategory_shouldFallbackToLegacyNodeCode() {
        // DCU_COCKPIT_SA8295P 旧注册表登记为座舱域
        assertEquals(BizType.CPT_DCU_DEVICE_ROOT, resolver.resolve("SOME_OTHER", "DCU_COCKPIT_SA8295P"));
        verify(metrics).recordBizTypeUnresolved("SOME_OTHER", "DCU_COCKPIT_SA8295P");
    }

    @Test
    @DisplayName("类别与旧节点码均不可解析应抛业务异常（RD-049-5）")
    void unresolved_shouldThrow() {
        assertThrows(SecurityPresetBizTypeUnresolvedException.class, () -> resolver.resolve("XYZ_UNKNOWN", "NODE_XYZ"));
        verify(metrics, atLeastOnce()).recordBizTypeUnresolved(any(), any());
    }
}
