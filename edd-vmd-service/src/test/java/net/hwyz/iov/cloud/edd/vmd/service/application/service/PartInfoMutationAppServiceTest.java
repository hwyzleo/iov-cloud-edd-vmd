package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 零件实例统一变更写服务单元测试（CR-071 §12.1）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class PartInfoMutationAppServiceTest {

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private PartInfoChangeLogRepository partInfoChangeLogRepository;

    private PartInfoMutationAppService service;

    @BeforeEach
    void setUp() {
        service = new PartInfoMutationAppService(partInfoRepository, partInfoChangeLogRepository);
    }

    private PartInfo existingPart(Integer rowVersion, String hardwareVer, String softwareVer, InboundSourceType source) {
        return PartInfo.builder()
                .id(1L)
                .partCode("00000001AA")
                .sn("SN001")
                .hardwareVer(hardwareVer)
                .softwareVer(softwareVer)
                .source(source)
                .rowVersion(rowVersion)
                .build();
    }

    @Test
    @DisplayName("FILL_MISSING：空值可补，非空差异保留")
    void fillMissing_fillsNullKeepsExisting() {
        PartInfo current = existingPart(1, null, "V1.0.0", InboundSourceType.MANUAL);
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .hardwareVer("H1.0")      // 空 → 可补
                .softwareVer("V9.0.0")    // 已有 V1.0.0 → 保留
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .operatorId("tester")
                .operationId("op-1")
                .phase("BASIC")
                .seq(1)
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);

        assertThat(result.isApplied()).isTrue();
        // 仅 hardwareVer 进入变更集
        assertThat(result.changedFields()).containsKey("hardwareVer");
        assertThat(result.changedFields()).doesNotContainKey("softwareVer");
        verify(partInfoChangeLogRepository).insert(any());
    }

    @Test
    @DisplayName("FILL_MISSING：MANUAL 默认来源不视为空，不被覆盖")
    void fillMissing_doesNotOverwriteManualSource() {
        PartInfo current = existingPart(1, "H1", "V1", InboundSourceType.MANUAL);
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .source("EOL") // MANUAL 不视为空 → 不覆盖
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .operatorId("tester")
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);

        assertThat(result.isNoop()).isTrue();
        assertThat(result.changedFields()).isEmpty();
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("CORRECT_EXPLICIT：预期版本一致时覆盖；不一致返回 CONFLICT")
    void correctExplicit_respectsExpectedVersion() {
        // 版本一致 → 覆盖
        PartInfo current = existingPart(5, "H1", "V1", InboundSourceType.MANUAL);
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .softwareVer("V2.0.0")
                .expectedRowVersion(5)
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .reason("治理纠错")
                .operatorId("tester")
                .build();

        PartInfoMutationAppService.MutationResult applied = service.patch(cmd);
        assertThat(applied.isApplied()).isTrue();
        assertThat(applied.changedFields()).containsEntry("softwareVer", "V2.0.0");

        // 版本不一致 → CONFLICT，不写库不写日志
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(existingPart(8, "H1", "V1", InboundSourceType.MANUAL));
        PartInfoPatchCmd staleCmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .softwareVer("V3.0.0")
                .expectedRowVersion(5)
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .reason("治理纠错")
                .operatorId("tester")
                .build();
        PartInfoMutationAppService.MutationResult conflict = service.patch(staleCmd);
        assertThat(conflict.outcome()).isEqualTo("CONFLICT");
        org.mockito.Mockito.clearInvocations(partInfoChangeLogRepository);
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("CAS 更新返回 0 行 → CONFLICT，不写属性历史")
    void casConflict_returnsConflictWithoutLog() {
        PartInfo current = existingPart(3, "H1", "V1", InboundSourceType.MANUAL);
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(0);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .hardwareVer("H2")
                .expectedRowVersion(3)
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .reason("治理纠错")
                .operatorId("tester")
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.outcome()).isEqualTo("CONFLICT");
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("零差异 → NOOP：不写日志、不增版本")
    void noDiff_isNoop() {
        PartInfo current = existingPart(2, "H1", "V1", InboundSourceType.MANUAL);
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA")
                .sn("SN001")
                .hardwareVer("H1") // 与当前一致
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .operatorId("tester")
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isNoop()).isTrue();
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("软件投影列可显式置 null（UNAVAILABLE 清空）")
    void nullFields_clearsProjection() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("P1").sn("S1")
                .softwareVer("V1.0.0").softwarePn("SW1")
                .softwareSnapshotState("CURRENT")
                .softwareSnapshotInstallationId(10L)
                .rowVersion(1)
                .build();
        when(partInfoRepository.selectByPartCodeAndSn("P1", "S1")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("P1")
                .sn("S1")
                .softwareSnapshotState("UNAVAILABLE")
                .nullFields(List.of("softwareVer", "softwarePn",
                        "softwareSnapshotInstallationId", "softwareSnapshotTarget",
                        "softwareSnapshotSlot", "softwareSnapshotInventoryVersion"))
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .changeType("SOFTWARE_SNAPSHOT")
                .operatorId("tester")
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isApplied()).isTrue();
        assertThat(result.changedFields()).containsEntry("softwareSnapshotState", "UNAVAILABLE");
        verify(partInfoChangeLogRepository).insert(any());
    }

}
