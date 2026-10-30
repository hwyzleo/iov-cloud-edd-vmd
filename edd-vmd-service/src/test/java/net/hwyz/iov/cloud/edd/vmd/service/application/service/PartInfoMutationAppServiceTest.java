package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNodeConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PreviewStaleException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private PartDeviceCategorySnapshotResolver deviceCategorySnapshotResolver;

    private PartInfoMutationAppService service;

    @BeforeEach
    void setUp() {
        service = new PartInfoMutationAppService(
                partInfoRepository, partInfoChangeLogRepository, deviceCategorySnapshotResolver);
        // CR-072 单元测试显式开启 device_category 功能开关（生产默认关闭）
        ReflectionTestUtils.setField(service, "deviceCategoryEnabled", true);
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

    // ==================== CR-072 deviceCategory 派生治理（UT-072） ====================

    @Test
    @DisplayName("UT-072-04：类别空白 + 节点合法 → FILL_MISSING 补一次，证据进审计 metadata；再重试 No-op")
    void deriveCategory_fillsBlankOnce_withMetadataEvidence() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);
        when(deviceCategorySnapshotResolver.resolve("DCU1"))
                .thenReturn(new PartDeviceCategorySnapshotResolver.ResolvedCategory("DCU", "ext-ref-1", 5L));

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1")
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operationSource("MPT_REIMPORT")
                .operatorId("tester").reason("补全数据")
                .phase("BASIC").seq(1)
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);

        assertThat(result.isApplied()).isTrue();
        assertThat(result.changedFields()).containsEntry("deviceCategory", "DCU");
        // 证据由服务器构造：节点码/类别/externalVersion/操作来源/原因，且客户端不可控
        ArgumentCaptor<PartInfoChangeLog> logCaptor = ArgumentCaptor.forClass(PartInfoChangeLog.class);
        verify(partInfoChangeLogRepository).insert(logCaptor.capture());
        String meta = logCaptor.getValue().getMetadataJson();
        assertThat(meta).contains("\"vehicleNodeCode\":\"DCU1\"");
        assertThat(meta).contains("\"deviceCategory\":\"DCU\"");
        assertThat(meta).contains("\"externalRefId\":\"ext-ref-1\"");
        assertThat(meta).contains("\"externalVersion\":5");
        assertThat(meta).contains("\"operationSource\":\"MPT_REIMPORT\"");

        // 已有类别非空：再次入站 No-op（不重复补、不增版本/历史）
        PartInfo filled = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory("DCU")
                .rowVersion(2).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(filled);
        org.mockito.Mockito.clearInvocations(partInfoChangeLogRepository);
        PartInfoMutationAppService.MutationResult second = service.patch(cmd);
        assertThat(second.isNoop()).isTrue();
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("UT-072-07：MDM 后变不自动刷新——已有非空快照保留，解析出不同值仅告警")
    void deriveCategory_keepsNonBlankSnapshot_onMdmChange() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory("TBOX")
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1")
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isNoop()).isTrue();
        assertThat(result.changedFields()).doesNotContainKey("deviceCategory");
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("UT-072-05：节点冲突——当前与输入均非空且不同 → 拒绝，无部分提交")
    void deriveCategory_nodeConflict_rejectsNoPartialWrite() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory(null)
                .hardwareVer(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU2")            // 与当前 DCU1 不同
                .hardwareVer("H2")                  // 其他字段也不得部分提交
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        assertThatThrownBy(() -> service.patch(cmd))
                .isInstanceOf(PartNodeConflictException.class);
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), any(), any());
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("UT-072-05：输入节点为空 = 未提供，不清节点；按当前节点派生类别")
    void deriveCategory_blankInputKeepsCurrentNode() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);
        when(deviceCategorySnapshotResolver.resolve("DCU1"))
                .thenReturn(new PartDeviceCategorySnapshotResolver.ResolvedCategory("DCU", null, null));

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isApplied()).isTrue();
        assertThat(result.changedFields()).containsEntry("deviceCategory", "DCU");
        assertThat(result.changedFields()).doesNotContainKey("vehicleNodeCode"); // 节点不变
    }

    @Test
    @DisplayName("UT-072-06：节点补全但类别解析失败 → 异常，节点不半更新")
    void deriveCategory_resolveFailure_noNodeHalfUpdate() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode(null).deviceCategory(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(deviceCategorySnapshotResolver.resolve("DCU1"))
                .thenThrow(new DeviceCategoryResolveException("DCU1", true, "NODE_PROJECTION_NOT_READY_OR_INVALID"));

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1")
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        assertThatThrownBy(() -> service.patch(cmd))
                .isInstanceOf(DeviceCategoryResolveException.class);
        // 节点/类别均不落库（无半更新）
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), any(), any());
        verify(partInfoChangeLogRepository, never()).insert(any());
    }

    @Test
    @DisplayName("UT-072-04：派生结果与计划候选类别不一致 → PREVIEW_STALE")
    void deriveCategory_plannedCategoryMismatch_stale() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(deviceCategorySnapshotResolver.resolve("DCU1"))
                .thenReturn(new PartDeviceCategorySnapshotResolver.ResolvedCategory("DCU", "ext-ref-1", 5L));

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1")
                .deriveDeviceCategory(true)
                .expectedPlannedCategory("TBOX")
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        assertThatThrownBy(() -> service.patch(cmd))
                .isInstanceOf(PreviewStaleException.class);
        verify(partInfoRepository, never()).patchByExpectedVersion(any(), any(), any());
    }

    @Test
    @DisplayName("UT-072-05：计划要求新实例但已被并发创建 → PREVIEW_STALE，不降级为补丁")
    void deriveCategory_requireNewInstance_concurrentCreate_stale() {
        PartInfo existing = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(existing);

        PartInfo partInfo = PartInfo.builder().partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").source(InboundSourceType.MANUAL).build();
        PartInfoPatchCmd ctx = PartInfoPatchCmd.builder()
                .deriveDeviceCategory(true).requireNewInstance(true)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1).build();

        assertThatThrownBy(() -> service.create(partInfo, ctx))
                .isInstanceOf(PreviewStaleException.class);
        verify(partInfoRepository, never()).insert(any());
    }

    @Test
    @DisplayName("UT-072：开关关闭 → 不新增类别写入、不清已有（节点行为不变）")
    void deriveCategory_switchOff_noCategoryWrite() {
        ReflectionTestUtils.setField(service, "deviceCategoryEnabled", false);
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory(null).hardwareVer(null)
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1")
                .hardwareVer("H2")
                .deriveDeviceCategory(true)
                .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                .changeType("INBOUND").operatorId("tester")
                .phase("BASIC").seq(1)
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isApplied()).isTrue();
        assertThat(result.changedFields()).containsKey("hardwareVer");
        assertThat(result.changedFields()).doesNotContainKey("deviceCategory");
        verify(deviceCategorySnapshotResolver, never()).resolve(any());
    }

    @Test
    @DisplayName("UT-072：deviceCategory 显式置 NULL 仅限内部受控补偿（nullFields）")
    void nullFields_deviceCategory_internalCompensation() {
        PartInfo current = PartInfo.builder()
                .id(1L).partCode("00000001AA").sn("SN001")
                .vehicleNodeCode("DCU1").deviceCategory("TBOX")
                .rowVersion(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("00000001AA", "SN001")).thenReturn(current);
        when(partInfoRepository.patchByExpectedVersion(any(), any(), any())).thenReturn(1);

        PartInfoPatchCmd cmd = PartInfoPatchCmd.builder()
                .partCode("00000001AA").sn("SN001")
                .nullFields(List.of("deviceCategory"))
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .changeType("ATTRIBUTE_UPDATE").operatorId("ops")
                .phase("BASIC").seq(1).reason("受控补偿")
                .build();

        PartInfoMutationAppService.MutationResult result = service.patch(cmd);
        assertThat(result.isApplied()).isTrue();
        assertThat(result.changedFields()).containsEntry("deviceCategory", null);
    }

}
