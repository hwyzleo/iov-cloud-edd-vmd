package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一导入操作门面单元测试（CR-071 §6 幂等矩阵）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class ImportOperationAppServiceTest {

    @Mock
    private ImportOperationRepository importOperationRepository;
    @Mock
    private VehImportDataRepository vehImportDataRepository;
    @Mock
    private PartImportDataRepository partImportDataRepository;
    @Mock
    private ImportReimportExecutor importReimportExecutor;
    @Mock
    private VehImportEventReplayAppService vehImportEventReplayAppService;
    @Mock
    private PartImportPostProcessReplayAppService partImportPostProcessReplayAppService;

    private ImportOperationAppService service;

    @BeforeEach
    void setUp() {
        service = new ImportOperationAppService(
                importOperationRepository, vehImportDataRepository, partImportDataRepository,
                importReimportExecutor, vehImportEventReplayAppService, partImportPostProcessReplayAppService);
    }

    private VehImportData importData() {
        VehImportData data = new VehImportData();
        data.setId(100L);
        data.setBatchNum("EOL-20260101-001");
        data.setType("EOL");
        data.setVersion("1.0");
        data.setData("{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"VIN001\"},{\"VIN\":\"VIN002\"}]}}}");
        return data;
    }

    private String requestDigestOf(ImportOperationAppService.ExecuteRequest req) {
        String raw = String.join(":",
                req.getDomain(), String.valueOf(req.getImportDataId()), req.getMode(),
                req.getScope() != null ? String.join(",", req.getScope()) : "*",
                req.getRepairPolicy() != null ? req.getRepairPolicy() : "FILL_MISSING",
                req.getReason() != null ? req.getReason() : "",
                req.getOperatorId() != null ? req.getOperatorId() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("preview：识别候选并过滤范围")
    void preview_extractsCandidates() {
        when(vehImportDataRepository.selectById(100L)).thenReturn(importData());

        var result = service.preview(new ImportOperationAppService.PreviewRequest(
                "VEHICLE", 100L, "REIMPORT", List.of("VIN002"), "FILL_MISSING"));

        assertThat(result.getCandidateCount()).isEqualTo(1);
        assertThat(result.getCandidates()).containsExactly("VIN002");
        assertThat(result.getPlanDigest()).isNotBlank();
    }

    @Test
    @DisplayName("execute REIMPORT/VEHICLE：认领执行并收敛终态")
    void execute_reimportRunsExecutor() {
        when(vehImportDataRepository.selectById(100L)).thenReturn(importData());
        when(importOperationRepository.selectActiveByDomainAndImportDataId("VEHICLE", 100L)).thenReturn(null);
        when(importOperationRepository.claim(anyString(), anyString(), any())).thenReturn(1);
        when(importOperationRepository.selectByOperationId(anyString())).thenAnswer(invocation ->
                ImportOperation.builder()
                        .operationId(invocation.getArgument(0))
                        .domain("VEHICLE").operationType("REIMPORT")
                        .importDataId(100L).status("SUCCEEDED")
                        .requestDigest(new byte[32]).build());
        when(importOperationRepository.finish(anyString(), anyString(), anyString(), any())).thenReturn(1);
        when(importReimportExecutor.executeVehicle(any(), anyList(), any()))
                .thenReturn(new ImportReimportExecutor.ReimportOutcome(2, 0, 0, 0));

        ImportOperationAppService.ExecuteRequest req = ImportOperationAppService.ExecuteRequest.builder()
                .domain("VEHICLE").importDataId(100L).mode("REIMPORT")
                .scope(List.of("VIN001", "VIN002")).repairPolicy("FILL_MISSING")
                .reason("补全字段").operatorId("tester").build();

        ImportOperationAppService.ExecuteResult result = service.execute(req);

        assertThat(result.getOperationId()).isNotBlank();
        verify(importReimportExecutor).executeVehicle(any(), anyList(), any());
    }

    @Test
    @DisplayName("同 operationId 同 requestDigest → 返回原结果（幂等）")
    void execute_sameRequestIdempotent() {
        ImportOperationAppService.ExecuteRequest req = ImportOperationAppService.ExecuteRequest.builder()
                .domain("VEHICLE").importDataId(100L).mode("REIMPORT")
                .scope(List.of("VIN001")).repairPolicy("FILL_MISSING")
                .reason("补全字段").operatorId("tester")
                .operationId("op-existing").build();
        ImportOperation existing = ImportOperation.builder()
                .operationId("op-existing").domain("VEHICLE").operationType("REIMPORT")
                .importDataId(100L).status("SUCCEEDED")
                .requestDigest(HexFormat.of().parseHex(requestDigestOf(req)))
                .countersJson("{\"changed\":1,\"unchanged\":0,\"skipped\":0,\"failed\":0}")
                .build();
        when(importOperationRepository.selectByOperationId("op-existing")).thenReturn(existing);

        ImportOperationAppService.ExecuteResult result = service.execute(req);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getOperationId()).isEqualTo("op-existing");
    }

    @Test
    @DisplayName("同 operationId 异 requestDigest → 幂等冲突")
    void execute_sameOperationDifferentDigest_conflict() {
        ImportOperationAppService.ExecuteRequest req = ImportOperationAppService.ExecuteRequest.builder()
                .domain("VEHICLE").importDataId(100L).mode("REIMPORT")
                .scope(List.of("VIN001")).repairPolicy("FILL_MISSING")
                .reason("补全字段").operatorId("tester")
                .operationId("op-existing").build();
        ImportOperation existing = ImportOperation.builder()
                .operationId("op-existing").domain("VEHICLE").operationType("REIMPORT")
                .importDataId(100L).status("SUCCEEDED")
                .requestDigest(new byte[32]) // 与真实摘要不一致
                .build();
        when(importOperationRepository.selectByOperationId("op-existing")).thenReturn(existing);

        assertThatThrownBy(() -> service.execute(req))
                .isInstanceOf(ImportOperationAppService.IdempotencyConflictException.class);
    }

    @Test
    @DisplayName("同 domain+importData 存在 active 操作 → InProgress 互斥")
    void execute_activeExists_inProgress() {
        ImportOperation active = ImportOperation.builder()
                .operationId("op-other").status("RUNNING").build();
        when(importOperationRepository.selectActiveByDomainAndImportDataId("VEHICLE", 100L)).thenReturn(active);

        ImportOperationAppService.ExecuteRequest req = ImportOperationAppService.ExecuteRequest.builder()
                .domain("VEHICLE").importDataId(100L).mode("REIMPORT")
                .reason("补全字段").operatorId("tester").build();

        assertThatThrownBy(() -> service.execute(req))
                .isInstanceOf(ImportOperationAppService.InProgressException.class);
    }

}
