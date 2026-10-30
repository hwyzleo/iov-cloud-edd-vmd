package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 车载节点设备类别快照解析职责单元测试（CR-072 UT-072-06）
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@ExtendWith(MockitoExtension.class)
class PartDeviceCategorySnapshotResolverTest {

    @Mock
    private VehicleNodeAppService vehicleNodeAppService;

    private PartDeviceCategorySnapshotResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PartDeviceCategorySnapshotResolver(vehicleNodeAppService);
    }

    private VehicleNode node(String code, String category, String externalRefId, Long externalVersion) {
        return VehicleNode.builder()
                .code(code)
                .deviceCategory(category)
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();
    }

    @Test
    @DisplayName("UT-072-06：本地投影有效 → 返回类别与来源标识/版本（同一读视图）")
    void resolve_success() {
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU1"))
                .thenReturn(node("DCU1", "DCU", "ext-1", 5L));

        PartDeviceCategorySnapshotResolver.ResolvedCategory result = resolver.resolve("DCU1");

        assertThat(result.deviceCategory()).isEqualTo("DCU");
        assertThat(result.externalRefId()).isEqualTo("ext-1");
        assertThat(result.externalVersion()).isEqualTo(5L);
    }

    @Test
    @DisplayName("UT-072-06：节点缺失（投影未到达）→ 可重试，不直接认定永久非法")
    void resolve_nodeMissing_retryable() {
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU1")).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve("DCU1"))
                .isInstanceOf(DeviceCategoryResolveException.class)
                .satisfies(e -> assertThat(((DeviceCategoryResolveException) e).isRetryable()).isTrue());
    }

    @Test
    @DisplayName("UT-072-06：类别为空 → 数据治理问题，不可重试")
    void resolve_categoryBlank_nonRetryable() {
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU1")).thenReturn(node("DCU1", "  ", "ext-1", 5L));

        assertThatThrownBy(() -> resolver.resolve("DCU1"))
                .isInstanceOf(DeviceCategoryResolveException.class)
                .satisfies(e -> assertThat(((DeviceCategoryResolveException) e).isRetryable()).isFalse());
    }

    @Test
    @DisplayName("UT-072-06：类别 trim 后超长 → 拒绝、不截断")
    void resolve_categoryTooLong_rejected() {
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU1"))
                .thenReturn(node("DCU1", "X".repeat(21), "ext-1", 5L));

        assertThatThrownBy(() -> resolver.resolve("DCU1"))
                .isInstanceOf(DeviceCategoryResolveException.class)
                .satisfies(e -> assertThat(((DeviceCategoryResolveException) e).isRetryable()).isFalse());
    }

    @Test
    @DisplayName("UT-072-06：trim 后合法长度 ≤20 → 通过，动态 code 不被 PartType 枚举拒绝")
    void resolve_trimWithinLimit() {
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU1"))
                .thenReturn(node("DCU1", "  DCU  ", "ext-1", null));

        PartDeviceCategorySnapshotResolver.ResolvedCategory result = resolver.resolve("DCU1");
        assertThat(result.deviceCategory()).isEqualTo("DCU");
        assertThat(result.externalVersion()).isNull(); // 不可得不编造
    }

}
