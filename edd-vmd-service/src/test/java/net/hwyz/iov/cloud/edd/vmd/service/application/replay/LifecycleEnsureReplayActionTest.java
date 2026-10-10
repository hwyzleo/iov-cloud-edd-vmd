package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TOL/EOL 生命周期节点补齐动作单元测试
 * <p>
 * VMD-DSN-CR-057: 生命周期补齐幂等；已有节点不覆盖原时间返回 SKIPPED_ALREADY_EXISTS。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LifecycleEnsureReplayAction 测试")
class LifecycleEnsureReplayActionTest {

    @Mock
    private VehicleLifecycleAppService lifecycleAppService;

    private TolLifecycleEnsureReplayAction tolAction;
    private EolLifecycleEnsureReplayAction eolAction;

    @BeforeEach
    void setUp() {
        tolAction = new TolLifecycleEnsureReplayAction(lifecycleAppService);
        eolAction = new EolLifecycleEnsureReplayAction(lifecycleAppService);
    }

    private VehicleImportReplayActionContext context(Instant batchTime) {
        return VehicleImportReplayActionContext.builder()
                .replayId("R1")
                .vehImportDataId(1L)
                .batchNum("B001")
                .importType("TOL")
                .vin("VIN001")
                .candidates(List.of(new VehImportReplayExtractor.PartCandidate("PN001", "SN001")))
                .batchTime(batchTime)
                .build();
    }

    @Test
    @DisplayName("TOL节点缺失时补齐成功返回SUCCESS")
    void tolEnsureWritesNode() {
        when(lifecycleAppService.ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.TOL), any())).thenReturn(true);
        VehicleImportReplayActionContext ctx = context(null);
        VehicleImportReplayActionResult result = tolAction.execute(ctx, tolAction.plan(ctx).get(0));
        assertEquals(VehicleImportReplayActionResult.Outcome.SUCCESS, result.getOutcome());
        verify(lifecycleAppService).ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.TOL), any());
    }

    @Test
    @DisplayName("TOL节点已存在时返回SKIPPED且不覆盖原时间")
    void tolEnsureAlreadyExistsSkipped() {
        when(lifecycleAppService.ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.TOL), any())).thenReturn(false);
        VehicleImportReplayActionContext ctx = context(null);
        VehicleImportReplayActionResult result = tolAction.execute(ctx, tolAction.plan(ctx).get(0));
        assertEquals(VehicleImportReplayActionResult.Outcome.SKIPPED, result.getOutcome());
        assertTrue(result.getSkipReason().startsWith("ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("EOL节点使用原批次EOL时间补齐")
    void eolEnsureUsesBatchTime() {
        Instant eolTime = Instant.ofEpochMilli(1784391702000L);
        when(lifecycleAppService.ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.EOL), eq(eolTime))).thenReturn(true);
        VehicleImportReplayActionContext ctx = VehicleImportReplayActionContext.builder()
                .replayId("R1").vehImportDataId(1L).batchNum("B001").importType("EOL")
                .vin("VIN001").candidates(List.of()).batchTime(eolTime).build();
        VehicleImportReplayActionResult result = eolAction.execute(ctx, eolAction.plan(ctx).get(0));
        assertEquals(VehicleImportReplayActionResult.Outcome.SUCCESS, result.getOutcome());
        verify(lifecycleAppService).ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.EOL), eq(eolTime));
    }

    @Test
    @DisplayName("EOL节点已存在时返回SKIPPED且不重复写入")
    void eolEnsureAlreadyExistsSkipped() {
        when(lifecycleAppService.ensureNode(eq("VIN001"), eq(VehicleLifecycleNodeEnum.EOL), any())).thenReturn(false);
        VehicleImportReplayActionContext ctx = context(Instant.now());
        VehicleImportReplayActionResult result = eolAction.execute(ctx, eolAction.plan(ctx).get(0));
        assertEquals(VehicleImportReplayActionResult.Outcome.SKIPPED, result.getOutcome());
        verify(lifecycleAppService, never()).recordEolNode(any(), any());
    }

    @Test
    @DisplayName("plan应返回单一VEHICLE_LIFECYCLE目标且版本为0")
    void planSingleTarget() {
        VehicleImportReplayActionContext ctx = context(null);
        List<VehicleImportReplayActionTarget> targets = tolAction.plan(ctx);
        assertEquals(1, targets.size());
        assertEquals("VEHICLE_LIFECYCLE", targets.get(0).aggregateType());
        assertEquals("VIN001", targets.get(0).aggregateId());
        assertEquals(0L, targets.get(0).aggregateVersion());
    }
}
