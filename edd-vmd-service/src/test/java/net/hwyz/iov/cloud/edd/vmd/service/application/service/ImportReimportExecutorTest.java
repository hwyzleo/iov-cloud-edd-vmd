package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehiclePartBinder;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 导入重导执行器单元测试（CR-071 §3.4 扁平键契约 / §6 幂等）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class ImportReimportExecutorTest {

    @Mock
    private VehImportDataRepository vehImportDataRepository;
    @Mock
    private PartImportDataRepository partImportDataRepository;
    @Mock
    private VehiclePartBinder vehiclePartBinder;
    @Mock
    private SoftwareInventoryAppService softwareInventoryAppService;
    @Mock
    private VehicleInfoMutationAppService vehicleInfoMutationAppService;
    @Mock
    private ImportOperationRepository importOperationRepository;
    @Mock
    private VehiclePartAppService vehiclePartAppService;
    @Mock
    private PartInboundAppService partInboundAppService;
    @Mock
    private PartSoftwareSnapshotProjectionService softwareSnapshotProjectionService;
    @Mock
    private PartInfoRepository partInfoRepository;

    private ImportReimportExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ImportReimportExecutor(
                vehImportDataRepository, partImportDataRepository, vehiclePartBinder,
                softwareInventoryAppService, vehicleInfoMutationAppService, importOperationRepository,
                vehiclePartAppService, partInboundAppService, softwareSnapshotProjectionService, partInfoRepository);
    }

    private ImportOperation operation() {
        return ImportOperation.builder()
                .operationId("op-part-1").domain("PART").operationType("REIMPORT")
                .importDataId(200L).operationSource("MPT_REIMPORT")
                .operatorId("tester").build();
    }

    private PartImportData importData(String itemsJson) {
        PartImportData data = new PartImportData();
        data.setId(200L);
        data.setBatchNum("PART-20261029-001");
        data.setVersion("v1.0");
        data.setData("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":" + itemsJson + "}}}");
        return data;
    }

    private PartInboundAppService.PartInboundResult inboundResult(int failed, int invalid) {
        return PartInboundAppService.PartInboundResult.builder()
                .totalCount(1).successCount(failed == 0 && invalid == 0 ? 1 : 0)
                .failureCount(failed).invalidCount(invalid).build();
    }

    @Test
    @DisplayName("PART 整批重导：扁平键入站成功 + 投影刷新 + 逐项审计")
    void executePart_success() {
        String items = "[{\"PART_CODE\":\"PN001\",\"SN\":\"SN001\",\"VEHICLE_NODE_CODE\":\"TBOX_M\","
                + "\"HARDWARE_PART_NO\":\"HW1\",\"HARDWARE_VERSION\":\"H1\",\"SOFTWARE_PART_NO\":\"SW1\","
                + "\"SOFTWARE_VERSION\":\"V1.0.0\",\"SOURCE_EVENT_ID\":\"evt-1\"}]";
        when(partImportDataRepository.selectById(200L)).thenReturn(importData(items));
        when(partInboundAppService.processInbound(anyList(), any(), any())).thenReturn(inboundResult(0, 0));
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001"))
                .thenReturn(PartInfo.builder().id(1L).partCode("PN001").sn("SN001").build());
        when(softwareSnapshotProjectionService.refreshFromCurrentSsot(any(), any()))
                .thenReturn(new PartSoftwareSnapshotProjectionService.ProjectionResult("NOOP", "UNAVAILABLE", null, 1));

        ImportReimportExecutor.ReimportOutcome outcome = executor.executePart(operation(), null,
                ImportReimportExecutor.ExecuteContext.builder().operationId("op-part-1").operatorId("tester").build());

        assertThat(outcome.changed()).isEqualTo(1);
        assertThat(outcome.failed()).isZero();
        verify(partInboundAppService).processInbound(anyList(), any(), any());
        verify(softwareSnapshotProjectionService).refreshFromCurrentSsot(any(), any());
        verify(importOperationRepository).insertItem(any());
    }

    @Test
    @DisplayName("PART scope 过滤：只处理候选内")
    void executePart_scopeFilter() {
        String items = "[{\"PART_CODE\":\"PN001\",\"SN\":\"SN001\"},{\"PART_CODE\":\"PN002\",\"SN\":\"SN002\"}]";
        when(partImportDataRepository.selectById(200L)).thenReturn(importData(items));

        ImportReimportExecutor.ReimportOutcome outcome = executor.executePart(operation(),
                List.of("PN003:SN003"),
                ImportReimportExecutor.ExecuteContext.builder().operationId("op-part-1").operatorId("tester").build());

        assertThat(outcome.changed()).isZero();
        assertThat(outcome.failed()).isZero();
        assertThat(outcome.skipped()).isEqualTo(2); // 候选内无匹配，全部跳过
        verify(partInboundAppService, org.mockito.Mockito.never()).processInbound(anyList(), any(), any());
    }

    @Test
    @DisplayName("PART 大小写别名冲突 → 该候选失败")
    void executePart_caseConflict_fails() {
        String items = "[{\"PART_CODE\":\"PN001\",\"SN\":\"SN001\",\"SOFTWARE_VERSION\":\"V1\",\"software_version\":\"V2\"}]";
        when(partImportDataRepository.selectById(200L)).thenReturn(importData(items));

        ImportReimportExecutor.ReimportOutcome outcome = executor.executePart(operation(), null,
                ImportReimportExecutor.ExecuteContext.builder().operationId("op-part-1").operatorId("tester").build());

        assertThat(outcome.failed()).isEqualTo(1);
        verify(importOperationRepository).insertItem(any());
    }

    @Test
    @DisplayName("PART MDM 校验失败（入站 failure）→ 该候选失败")
    void executePart_inboundFailure_fails() {
        String items = "[{\"PART_CODE\":\"UNKNOWN\",\"SN\":\"SN001\"}]";
        when(partImportDataRepository.selectById(200L)).thenReturn(importData(items));
        when(partInboundAppService.processInbound(anyList(), any(), any())).thenReturn(inboundResult(1, 0));

        ImportReimportExecutor.ReimportOutcome outcome = executor.executePart(operation(), null,
                ImportReimportExecutor.ExecuteContext.builder().operationId("op-part-1").operatorId("tester").build());

        assertThat(outcome.failed()).isEqualTo(1);
        assertThat(outcome.changed()).isZero();
    }

}
