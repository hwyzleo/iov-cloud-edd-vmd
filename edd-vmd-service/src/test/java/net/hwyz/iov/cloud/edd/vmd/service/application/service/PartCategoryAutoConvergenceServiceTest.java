package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.PartCategoryGovernanceMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 零件派生类别自动收敛单元测试（EDD-VMD-DSN-CR-078 / TC-VMD-78-04～07）
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@ExtendWith(MockitoExtension.class)
class PartCategoryAutoConvergenceServiceTest {

    @Mock
    private PartCategoryConsistencyAppService consistencyAppService;

    @Mock
    private PartCategoryGovernanceMetrics metrics;

    @InjectMocks
    private PartCategoryAutoConvergenceService service;

    private static PartCategoryConsistencyAppService.RepairResult result(int total, int applied,
                                                                        int skipped, int conflict, int failed) {
        return new PartCategoryConsistencyAppService.RepairResult(total, applied, skipped, conflict, failed,
                List.of());
    }

    @Test
    @DisplayName("TC-VMD-78-04 节点类别变更应触发一次按节点收敛并记录 NODE_CHANGED 计数")
    void shouldConvergeByNodeCodeWhenCategoryChanged() {
        when(consistencyAppService.convergeByVehicleNodeCode(
                eq("DCU_COCKPIT_SA8295P"), eq(PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED)))
                .thenReturn(result(1, 1, 0, 0, 0));

        service.onNodeCategoryChanged("DCU_COCKPIT_SA8295P", "DCU", "IVI");

        verify(consistencyAppService).convergeByVehicleNodeCode(
                "DCU_COCKPIT_SA8295P", PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED);
        verify(metrics).recordRepair(PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED, 1, 0, 0, 0);
        verify(metrics, never()).recordRepairFailure(anyString());
    }

    @Test
    @DisplayName("TC-VMD-78-06 收敛异常必须被吞掉并计失败，不向上抛出")
    void shouldSwallowExceptionAndRecordFailure() {
        when(consistencyAppService.convergeByVehicleNodeCode(anyString(), anyString()))
                .thenThrow(new IllegalStateException("db down"));

        service.onNodeCategoryChanged("DCU_COCKPIT_SA8295P", "DCU", "IVI");

        verify(metrics).recordRepairFailure(PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED);
    }

    @Test
    @DisplayName("TC-VMD-78-07 节点码或新类别为空不触发收敛")
    void shouldSkipWhenNodeCodeOrCategoryBlank() {
        service.onNodeCategoryChanged("  ", "DCU", "IVI");
        service.onNodeCategoryChanged("DCU_COCKPIT_SA8295P", "IVI", null);
        service.onNodeCategoryChanged(null, "DCU", "IVI");

        verifyNoInteractions(consistencyAppService, metrics);
    }
}
