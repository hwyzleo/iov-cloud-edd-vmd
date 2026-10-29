package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.PrimarySoftwareTargetPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 软件兼容投影服务单元测试（CR-071 §5.1：按 SOFTWARE_TYPE 优先级的状态矩阵）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class PartSoftwareSnapshotProjectionServiceTest {

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private PartSoftwareInstallationRepository partSoftwareInstallationRepository;

    @Mock
    private PartInfoMutationAppService partInfoMutationAppService;

    @Mock
    private PrimarySoftwareTargetPolicy primarySoftwareTargetPolicy;

    private PartSoftwareSnapshotProjectionService service;

    @BeforeEach
    void setUp() {
        service = new PartSoftwareSnapshotProjectionService(
                partInfoRepository, partSoftwareInstallationRepository,
                partInfoMutationAppService, primarySoftwareTargetPolicy);
        // 默认类型优先级：APP(0) > BOOT(1)（部分用例不触发，lenient 避免 UnnecessaryStubbing）
        org.mockito.Mockito.lenient().when(primarySoftwareTargetPolicy.priorityOf("APP")).thenReturn(0);
        org.mockito.Mockito.lenient().when(primarySoftwareTargetPolicy.priorityOf("BOOT")).thenReturn(1);
        org.mockito.Mockito.lenient().when(primarySoftwareTargetPolicy.priorityOf(null)).thenReturn(-1);
        org.mockito.Mockito.lenient().when(primarySoftwareTargetPolicy.priorityOf("")).thenReturn(-1);
    }

    private PartInfo partInfo() {
        return PartInfo.builder().id(1L).partCode("00000001AA").vehicleNodeCode("TBOX_5G").build();
    }

    private PartSoftwareInstallation record(String type, String target, String version, String state,
                                            Boolean activeSlot, Boolean confirmed, Long id) {
        return PartSoftwareInstallation.builder()
                .id(id)
                .partId(1L)
                .softwareType(type)
                .softwareTargetCode(target)
                .softwarePartNo("SW-" + target)
                .softwareVersion(version)
                .installState(state)
                .effectiveTo(null)
                .isActiveSlot(activeSlot)
                .isConfirmed(confirmed)
                .slot("SINGLE_IMAGE")
                .inventoryVersion(id)
                .build();
    }

    private PartSoftwareSnapshotProjectionService.ProjectionContext ctx() {
        return PartSoftwareSnapshotProjectionService.ProjectionContext.builder()
                .operationId("op-1").operatorId("tester").source("EOL").build();
    }

    @Test
    @DisplayName("唯一 APP confirmed active → CURRENT，成对投影")
    void singleAppCandidate_current() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(record("APP", "00000003AA", "V1.0.0", "ACTIVE", true, true, 10L)));
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of("softwareVer", "V1.0.0")));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("CURRENT");
        assertThat(result.outcome()).isEqualTo("APPLIED");
    }

    @Test
    @DisplayName("APP 与 BOOT 并存 → 按优先级选 APP → CURRENT")
    void multipleTypes_pickHighestPriority() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(
                        record("APP", "00000003AA", "V1.0.0", "ACTIVE", true, true, 10L),
                        record("BOOT", "00000004AA", "V0.1.0", "ACTIVE", true, true, 11L)));
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("CURRENT");
    }

    @Test
    @DisplayName("confirmed 候选类型均未配置 → AMBIGUOUS(PRIMARY_TYPE_UNMAPPED)")
    void unmappedType_ambiguous() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(record("CALIBRATION", "00000005AA", "V1.0.0", "ACTIVE", true, true, 10L)));
        when(primarySoftwareTargetPolicy.priorityOf("CALIBRATION")).thenReturn(-1);
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("AMBIGUOUS");
        assertThat(result.reason()).isEqualTo("PRIMARY_TYPE_UNMAPPED");
    }

    @Test
    @DisplayName("同优先级多候选 → AMBIGUOUS(MULTIPLE_CURRENT_CANDIDATES)")
    void samePriorityMultiple_ambiguous() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(
                        record("APP", "00000003AA", "V1.0.0", "ACTIVE", true, true, 10L),
                        record("APP", "00000003AB", "V1.1.0", "ACTIVE", true, true, 11L)));
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("AMBIGUOUS");
        assertThat(result.reason()).isEqualTo("MULTIPLE_CURRENT_CANDIDATES");
    }

    @Test
    @DisplayName("仅 provisional → PROVISIONAL")
    void onlyProvisional_provisional() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(record("APP", "00000003AA", "V1.0.0", "ACTIVE", true, false, 10L)));
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("PROVISIONAL");
    }

    @Test
    @DisplayName("无数据 → UNAVAILABLE")
    void noData_unavailable() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L)).thenReturn(List.of());
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.state()).isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("重算结果与当前一致 → NOOP，不写日志")
    void noChange_noop() {
        when(partInfoRepository.selectById(1L)).thenReturn(partInfo());
        when(partSoftwareInstallationRepository.selectByPartId(1L))
                .thenReturn(List.of(record("APP", "00000003AA", "V1.0.0", "ACTIVE", true, true, 10L)));
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("NOOP", 1, Map.of()));

        var result = service.refreshFromCurrentSsot(1L, ctx());

        assertThat(result.outcome()).isEqualTo("NOOP");
    }

}
