package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryConsistencyAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.PartCategoryGovernanceMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 零件派生类别一致性巡检调度单元测试（EDD-VMD-DSN-CR-078 / TC-VMD-78-08～09）
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@ExtendWith(MockitoExtension.class)
class PartCategoryConsistencySchedulerTest {

    @Mock
    private PartCategoryConsistencyAppService consistencyAppService;

    @Mock
    private PartCategoryGovernanceMetrics metrics;

    @InjectMocks
    private PartCategoryConsistencyScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "autoRepairEnabled", true);
        ReflectionTestUtils.setField(scheduler, "intervalMillis", 600000L);
    }

    private static PartCategoryConsistencyAppService.RepairResult result(int applied, int failed) {
        return new PartCategoryConsistencyAppService.RepairResult(applied + failed, applied, 0, 0, failed, List.of());
    }

    @Test
    @DisplayName("TC-VMD-78-09 无不一致：只刷新 gauge，不触发修复")
    void shouldOnlyRefreshGaugeWhenConsistent() {
        when(consistencyAppService.countInconsistent()).thenReturn(0L);

        scheduler.inspectAndConverge();

        verify(metrics).updateInconsistency(0L);
        verify(consistencyAppService, never()).convergeByVehicleNodeCode(anyString(), anyString());
    }

    @Test
    @DisplayName("TC-VMD-78-08 默认自动修复：按不一致节点分组逐个收敛并计数")
    void shouldConvergePerNodeWhenAutoRepairEnabled() {
        when(consistencyAppService.countInconsistent()).thenReturn(2L, 0L);
        when(consistencyAppService.listInconsistentVehicleNodeCodes())
                .thenReturn(List.of("DCU_COCKPIT_SA8295P", "TBOX_5G"));
        when(consistencyAppService.convergeByVehicleNodeCode(eq("DCU_COCKPIT_SA8295P"), anyString()))
                .thenReturn(result(1, 0));
        when(consistencyAppService.convergeByVehicleNodeCode(eq("TBOX_5G"), anyString()))
                .thenReturn(result(1, 0));

        scheduler.inspectAndConverge();

        verify(consistencyAppService).convergeByVehicleNodeCode(
                "DCU_COCKPIT_SA8295P", PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
        verify(consistencyAppService).convergeByVehicleNodeCode(
                "TBOX_5G", PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
        verify(metrics, times(2)).recordRepair(
                eq(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED), eq(1), eq(0), eq(0), eq(0));
        verify(metrics).updateInconsistency(0L);
    }

    @Test
    @DisplayName("TC-VMD-78-08 关闭自动修复：只告警不收敛")
    void shouldOnlyObserveWhenAutoRepairDisabled() {
        ReflectionTestUtils.setField(scheduler, "autoRepairEnabled", false);
        when(consistencyAppService.countInconsistent()).thenReturn(1L);

        scheduler.inspectAndConverge();

        verify(metrics).updateInconsistency(1L);
        verify(consistencyAppService, never()).listInconsistentVehicleNodeCodes();
        verify(consistencyAppService, never()).convergeByVehicleNodeCode(anyString(), anyString());
    }

    @Test
    @DisplayName("TC-VMD-78-06 单节点收敛失败不影响其余节点与后续轮次")
    void shouldContinueOtherNodesWhenOneNodeFails() {
        when(consistencyAppService.countInconsistent()).thenReturn(2L, 1L);
        when(consistencyAppService.listInconsistentVehicleNodeCodes())
                .thenReturn(List.of("NODE_A", "NODE_B"));
        when(consistencyAppService.convergeByVehicleNodeCode(eq("NODE_A"), anyString()))
                .thenThrow(new IllegalStateException("cas exhausted"));
        when(consistencyAppService.convergeByVehicleNodeCode(eq("NODE_B"), anyString()))
                .thenReturn(result(1, 0));

        scheduler.inspectAndConverge();

        verify(metrics).recordRepairFailure(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
        verify(metrics).recordRepair(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED, 1, 0, 0, 0);
        verify(metrics).updateInconsistency(1L);
    }

    @Test
    @DisplayName("巡检查询异常只告警，不抛出异常也不触发修复")
    void shouldNotThrowWhenInspectionFails() {
        when(consistencyAppService.countInconsistent()).thenThrow(new IllegalStateException("db down"));

        scheduler.inspectAndConverge();

        verify(metrics, never()).updateInconsistency(anyLong());
        verify(consistencyAppService, never()).convergeByVehicleNodeCode(anyString(), anyString());
    }
}
