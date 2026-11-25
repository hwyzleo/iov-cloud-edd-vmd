package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartCategoryConsistencyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartCategoryInconsistencyPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 零件类别一致性巡检与修复单元测试（SEC-VIAM-DSN-CR-006 / D-VIAM-036）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class PartCategoryConsistencyAppServiceTest {

    @Mock
    private PartCategoryConsistencyMapper consistencyMapper;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;

    @Mock
    private PartInfoChangeLogRepository partInfoChangeLogRepository;

    @InjectMocks
    private PartCategoryConsistencyAppService appService;

    private static PartCategoryInconsistencyPo po() {
        PartCategoryInconsistencyPo po = new PartCategoryInconsistencyPo();
        po.setId(2L);
        po.setPartCode("00000004AA");
        po.setSn("00000005AA00000001");
        po.setVehicleNodeCode("DCU_COCKPIT_SA8295P");
        po.setCurrentCategory("DCU");
        po.setExpectedCategory("IVI");
        po.setRowVersion(13);
        return po;
    }

    private static PartInfo part(String category, Integer rowVersion) {
        return PartInfo.builder()
                .id(2L)
                .partCode("00000004AA")
                .sn("00000005AA00000001")
                .vehicleNodeCode("DCU_COCKPIT_SA8295P")
                .deviceCategory(category)
                .rowVersion(rowVersion)
                .build();
    }

    private static VehicleNode node(String category) {
        return VehicleNode.builder()
                .code("DCU_COCKPIT_SA8295P")
                .deviceCategory(category)
                .build();
    }

    private static PartCategoryConsistencyAppService.RepairCommand command(String... partCodeSn) {
        PartCategoryConsistencyAppService.RepairCommand command =
                new PartCategoryConsistencyAppService.RepairCommand();
        PartCategoryConsistencyAppService.RepairItemCmd item =
                new PartCategoryConsistencyAppService.RepairItemCmd();
        item.setPartCode("00000004AA");
        item.setSn("00000005AA00000001");
        item.setExpectedCategory("IVI");
        item.setExpectedRowVersion(13);
        command.setItems(List.of(item));
        command.setReason("节点类别已调整为IVI");
        command.setTicketNo("T-1");
        return command;
    }

    @Test
    @DisplayName("巡检：存在不一致时返回总数与清单")
    void preview_shouldReturnInconsistentItems() {
        when(consistencyMapper.countInconsistent()).thenReturn(1L);
        when(consistencyMapper.selectInconsistent(100)).thenReturn(List.of(po()));

        PartCategoryConsistencyAppService.PreviewResult result = appService.preview(null);

        assertEquals(1L, result.getTotal());
        assertEquals(100, result.getLimit());
        assertEquals(1, result.getItems().size());
        assertEquals("IVI", result.getItems().get(0).getExpectedCategory());
    }

    @Test
    @DisplayName("巡检：无差异时不查清单")
    void preview_shouldSkipQueryWhenNoInconsistency() {
        when(consistencyMapper.countInconsistent()).thenReturn(0L);

        PartCategoryConsistencyAppService.PreviewResult result = appService.preview(50);

        assertTrue(result.getItems().isEmpty());
        verify(consistencyMapper, never()).selectInconsistent(anyInt());
    }

    @Test
    @DisplayName("巡检：limit 上限收敛为 500")
    void preview_shouldCapLimit() {
        when(consistencyMapper.countInconsistent()).thenReturn(0L);

        assertEquals(500, appService.preview(9999).getLimit());
    }

    @Test
    @DisplayName("修复：期望类别等于节点类别时按乐观锁更新并写变更历史")
    void repair_shouldApplyAndWriteChangeLog() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(1);

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getApplied());
        assertEquals(0, result.getFailed());
        assertEquals("APPLIED", result.getItems().get(0).getResult());
        assertEquals(14, result.getItems().get(0).getNewRowVersion());

        ArgumentCaptor<PartInfo> partCaptor = ArgumentCaptor.forClass(PartInfo.class);
        verify(partInfoRepository).patchByExpectedVersion(partCaptor.capture(), eq(13), anyList());
        assertEquals("IVI", partCaptor.getValue().getDeviceCategory());
        assertEquals(2L, partCaptor.getValue().getId());

        ArgumentCaptor<PartInfoChangeLog> logCaptor = ArgumentCaptor.forClass(PartInfoChangeLog.class);
        verify(partInfoChangeLogRepository).insert(logCaptor.capture());
        PartInfoChangeLog changeLog = logCaptor.getValue();
        assertEquals("ATTRIBUTE_UPDATE", changeLog.getChangeType());
        assertTrue(changeLog.getBeforeJson().contains("DCU"));
        assertTrue(changeLog.getAfterJson().contains("IVI"));
        assertEquals(14, changeLog.getRowVersion());
        assertEquals(32, changeLog.getMutationKey().length);
    }

    @Test
    @DisplayName("修复：已一致时跳过（不写库、不写历史）")
    void repair_shouldSkipWhenAlreadyConsistent() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("IVI", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getSkipped());
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), anyInt(), anyList());
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("修复：期望类别与当前 MDM 节点不一致时失败")
    void repair_shouldFailWhenExpectedCategoryStale() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("DCU"));

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getFailed());
        assertEquals("FAILED", result.getItems().get(0).getResult());
        assertTrue(result.getItems().get(0).getMessage().contains("期望类别"));
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), anyInt(), anyList());
    }

    @Test
    @DisplayName("修复：rowVersion 不一致时冲突且无副作用")
    void repair_shouldConflictOnVersionMismatch() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 12));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getConflict());
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), anyInt(), anyList());
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("修复：CASS 更新 0 行时按冲突处理")
    void repair_shouldConflictWhenCasUpdateReturnsZero() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(0);

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getConflict());
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("修复：MDM 节点缺失时失败")
    void repair_shouldFailWhenNodeMissing() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(null);

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "1", "admin");

        assertEquals(1, result.getFailed());
        assertTrue(result.getItems().get(0).getMessage().contains("MDM 节点类别不可用"));
    }

    @Test
    @DisplayName("修复：仅给 vehicleNodeCode 时按节点批量推导条目")
    void repair_shouldResolveItemsByVehicleNodeCode() {
        PartCategoryConsistencyAppService.RepairCommand command =
                new PartCategoryConsistencyAppService.RepairCommand();
        command.setVehicleNodeCode("DCU_COCKPIT_SA8295P");
        command.setReason("节点类别批量重算");
        command.setTicketNo("T-2");

        when(consistencyMapper.selectInconsistentByVehicleNodeCode("DCU_COCKPIT_SA8295P"))
                .thenReturn(List.of(po()));
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(1);

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command, "1", "admin");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getApplied());
    }

    @Test
    @DisplayName("修复：items 与 vehicleNodeCode 同时为空时拒绝")
    void repair_shouldRejectEmptyScope() {
        PartCategoryConsistencyAppService.RepairCommand command =
                new PartCategoryConsistencyAppService.RepairCommand();
        command.setReason("r");
        command.setTicketNo("t");

        assertThrows(IllegalArgumentException.class, () -> appService.repair(command, "1", "admin"));
    }

    @Test
    @DisplayName("CR-078 TC-VMD-78-04：自动触发（NODE_CHANGED）审计为 SYSTEM 并记录 trigger/nodeCode")
    void repair_shouldMarkAuditAsSystemWhenTriggeredAutomatically() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(1);

        PartCategoryConsistencyAppService.RepairResult result = appService.repair(command(), "SYSTEM", "SYSTEM",
                PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED);

        assertEquals(1, result.getApplied());
        ArgumentCaptor<PartInfoChangeLog> logCaptor = ArgumentCaptor.forClass(PartInfoChangeLog.class);
        verify(partInfoChangeLogRepository).insert(logCaptor.capture());
        PartInfoChangeLog changeLog = logCaptor.getValue();
        assertEquals("SYSTEM", changeLog.getSource());
        assertEquals("SYSTEM", changeLog.getOperationSource());
        assertEquals("SYSTEM", changeLog.getOperatorId());
        assertTrue(changeLog.getMetadataJson().contains("NODE_CHANGED"));
        assertTrue(changeLog.getMetadataJson().contains("DCU_COCKPIT_SA8295P"));
    }

    @Test
    @DisplayName("CR-078：人工触发审计仍为 MANUAL/MPT")
    void repair_shouldKeepManualAuditForManualTrigger() {
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(1);

        appService.repair(command(), "1", "admin");

        ArgumentCaptor<PartInfoChangeLog> logCaptor = ArgumentCaptor.forClass(PartInfoChangeLog.class);
        verify(partInfoChangeLogRepository).insert(logCaptor.capture());
        assertEquals("MANUAL", logCaptor.getValue().getSource());
        assertEquals("MPT", logCaptor.getValue().getOperationSource());
        assertTrue(logCaptor.getValue().getMetadataJson().contains("MANUAL"));
    }

    @Test
    @DisplayName("CR-078 TC-VMD-78-08：按节点收敛入口走 NODE_CHANGED 触发来源并写 SYSTEM 审计")
    void recomputeByVehicleNodeCode_shouldUseNodeChangedTrigger() {
        PartCategoryInconsistencyPo po = po();
        when(consistencyMapper.selectInconsistentByVehicleNodeCode("DCU_COCKPIT_SA8295P"))
                .thenReturn(List.of(po));
        when(partInfoRepository.selectByPartCodeAndSn("00000004AA", "00000005AA00000001"))
                .thenReturn(part("DCU", 13));
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(node("IVI"));
        when(partInfoRepository.patchByExpectedVersion(any(PartInfo.class), eq(13), anyList())).thenReturn(1);

        PartCategoryConsistencyAppService.RepairResult result = appService.recomputeByVehicleNodeCode(
                "DCU_COCKPIT_SA8295P", "SYSTEM", "SYSTEM", "MDM 节点类别变更");

        assertEquals(1, result.getApplied());
        ArgumentCaptor<PartInfoChangeLog> logCaptor = ArgumentCaptor.forClass(PartInfoChangeLog.class);
        verify(partInfoChangeLogRepository).insert(logCaptor.capture());
        assertEquals("SYSTEM", logCaptor.getValue().getSource());
        assertTrue(logCaptor.getValue().getMetadataJson().contains("NODE_CHANGED"));
    }

    @Test
    @DisplayName("CR-078：count/list 只读入口透传统计与节点分组")
    void countAndListInconsistent_shouldDelegateToMapper() {
        when(consistencyMapper.countInconsistent()).thenReturn(3L);
        when(consistencyMapper.selectInconsistentVehicleNodeCodes()).thenReturn(List.of("NODE_A"));

        assertEquals(3L, appService.countInconsistent());
        assertEquals(List.of("NODE_A"), appService.listInconsistentVehicleNodeCodes());
    }

    @Test
    @DisplayName("CR-078：list 返回 null 时降级为空列表，不抛 NPE")
    void listInconsistentVehicleNodeCodes_shouldHandleNull() {
        when(consistencyMapper.selectInconsistentVehicleNodeCodes()).thenReturn(null);

        assertTrue(appService.listInconsistentVehicleNodeCodes().isEmpty());
    }
}
