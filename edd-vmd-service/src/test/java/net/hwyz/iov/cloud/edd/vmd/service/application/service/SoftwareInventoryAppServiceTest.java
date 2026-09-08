package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SoftwareInventoryAppService 单元测试
 * <p>
 * VMD-DSN-CR-046: 多 Slot 消解、active 槽切换、幂等、版本 gate、Outbox 事件发布
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class SoftwareInventoryAppServiceTest {

    @Mock
    private PartSoftwareInstallationRepository repository;

    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private VehiclePartRepository vehiclePartRepository;

    @InjectMocks
    private SoftwareInventoryAppService appService;

    private static final Long PART_ID = 200L;
    private static final Long BINDING_ID = 100L;
    private static final String VIN = "HWYZTEST000000001";
    private static final String TARGET = "TBOX_APP";
    private static final String SOURCE = "VEHICLE_REPORT";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appService, "changedEventTopic", "vmd-vehicle-software-inventory-changed");
    }

    private PartSoftwareInstallation currentRecord(String version, String slot, Boolean activeSlot,
                                                   Instant sourceEventTime, String source) {
        return PartSoftwareInstallation.builder()
                .id(1L)
                .partId(PART_ID)
                .bindingId(BINDING_ID)
                .vinSnapshot(VIN)
                .softwareTargetCode(TARGET)
                .softwareVersion(version)
                .slot(slot)
                .installState("ACTIVE")
                .source(source)
                .sourceEventId("prev-event")
                .sourceEventTime(sourceEventTime)
                .inventoryVersion(1L)
                .isConfirmed(true)
                .isActiveSlot(activeSlot)
                .build();
    }

    @Test
    void applyManifest_firstWrite_shouldAppliedAndPublishOutbox() {
        // Given
        when(repository.selectBySourceAndSourceEventId(any(), any(), any(), any())).thenReturn(null);
        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, null)).thenReturn(null);
        when(repository.selectByPartId(PART_ID)).thenReturn(List.of());

        // When
        SoftwareInventoryAppService.ApplyManifestResult result = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                "digest1", null, "INITIAL",
                SOURCE, "evt-001", Instant.parse("2026-09-08T10:00:00Z"),
                Instant.parse("2026-09-08T10:00:00Z"), true,
                true, "obs-001", 1, "sha-001", Instant.parse("2026-09-08T10:01:00Z"));

        // Then
        assertTrue(result.applied());
        assertFalse(result.ignoredByVersionGate());
        assertEquals(1L, result.currentInventoryVersion());
        verify(repository).insert(any(PartSoftwareInstallation.class));
        verify(vmdOutboxRepository).insert(any(VmdOutbox.class));
    }

    @Test
    void applyManifest_idempotentHit_shouldSkip() {
        // Given
        when(repository.selectBySourceAndSourceEventId(SOURCE, "evt-001", TARGET, null))
                .thenReturn(currentRecord("1.0.0", null, true,
                        Instant.parse("2026-09-08T10:00:00Z"), SOURCE));

        // When
        SoftwareInventoryAppService.ApplyManifestResult result = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                null, null, "INITIAL", SOURCE, "evt-001",
                Instant.parse("2026-09-08T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z"), true);

        // Then
        assertFalse(result.applied());
        verify(repository, never()).insert(any());
        verify(vmdOutboxRepository, never()).insert(any());
    }

    @Test
    void applyManifest_staleVersionGate_shouldIgnore() {
        // Given
        when(repository.selectBySourceAndSourceEventId(any(), any(), any(), any())).thenReturn(null);
        Instant currentTime = Instant.parse("2026-09-08T11:00:00Z");
        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, null))
                .thenReturn(currentRecord("2.0.0", null, true, currentTime, SOURCE));

        // When：新事件时间早于当前
        SoftwareInventoryAppService.ApplyManifestResult result = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "2.0.0",
                null, null, "UPGRADE", SOURCE, "evt-002",
                Instant.parse("2026-09-08T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z"), true);

        // Then
        assertTrue(result.ignoredByVersionGate());
        verify(repository, never()).insert(any());
    }

    @Test
    void applyManifest_activeSlotSwitch_sameVersion_shouldWrite() {
        // Given
        when(repository.selectBySourceAndSourceEventId(any(), any(), any(), any())).thenReturn(null);
        // 当前 slot=B standby，incoming slot=B active，版本相同 → 属 SSOT 变化
        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, "B"))
                .thenReturn(currentRecord("1.0.0", "B", false,
                        Instant.parse("2026-09-08T10:00:00Z"), SOURCE));
        when(repository.selectByPartId(PART_ID))
                .thenReturn(List.of(currentRecord("1.0.0", "B", false,
                        Instant.parse("2026-09-08T10:00:00Z"), SOURCE)));

        // When
        SoftwareInventoryAppService.ApplyManifestResult result = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                null, "B", "UPGRADE", SOURCE, "evt-003",
                Instant.parse("2026-09-08T12:00:00Z"), Instant.parse("2026-09-08T12:00:00Z"), true,
                true, "obs-002", 1, "sha-002", null);

        // Then：active 槽切换不被同版本忽略，执行写入并重置其他槽 active 标记
        assertTrue(result.applied());
        verify(repository).deactivateByPartIdTargetCodeAndSlot(PART_ID, TARGET, "B");
        verify(repository).resetActiveSlotByPartIdAndTargetCode(PART_ID, TARGET);
        verify(repository).insert(any(PartSoftwareInstallation.class));
        verify(vmdOutboxRepository).insert(any(VmdOutbox.class));
    }

    @Test
    void applyManifest_sameVersionSameSourceConfirmed_shouldIgnore() {
        // Given
        when(repository.selectBySourceAndSourceEventId(any(), any(), any(), any())).thenReturn(null);
        Instant currentTime = Instant.parse("2026-09-08T10:00:00Z");
        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, "A"))
                .thenReturn(currentRecord("1.0.0", "A", true, currentTime, SOURCE));

        // When：同版本、同来源(VEHICLE_REPORT)、当前 confirmed → 来源优先级兜底忽略
        SoftwareInventoryAppService.ApplyManifestResult result = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                null, "A", "INITIAL", SOURCE, "evt-004",
                Instant.parse("2026-09-08T11:00:00Z"), Instant.parse("2026-09-08T11:00:00Z"), true,
                true, "obs-003", 1, "sha-003", null);

        // Then
        assertFalse(result.applied());
        verify(repository, never()).insert(any());
    }

    @Test
    void applyManifest_multiSlot_shouldWriteEachSlotHistory() {
        // Given
        when(repository.selectBySourceAndSourceEventId(any(), any(), any(), any())).thenReturn(null);
        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, "A")).thenReturn(null);
        when(repository.selectByPartId(PART_ID)).thenReturn(List.of());

        // When：先写 A（active），再写 B（standby）
        SoftwareInventoryAppService.ApplyManifestResult resultA = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                null, "A", "INITIAL", SOURCE, "evt-A",
                Instant.parse("2026-09-08T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z"), true,
                true, "obs-A", 1, "sha-A", null);

        when(repository.selectActiveByPartIdTargetCodeAndSlot(PART_ID, TARGET, "B")).thenReturn(null);
        SoftwareInventoryAppService.ApplyManifestResult resultB = appService.applyManifest(
                PART_ID, BINDING_ID, VIN, TARGET, "SW-PN", "1.0.0",
                null, "B", "INITIAL", SOURCE, "evt-B",
                Instant.parse("2026-09-08T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z"), true,
                false, "obs-B", 1, "sha-B", null);

        // Then：A active 写入时不 reset（无其他槽），B standby 写入时不 reset
        assertTrue(resultA.applied());
        assertTrue(resultB.applied());
        verify(repository, times(2)).insert(any(PartSoftwareInstallation.class));
        verify(repository, times(1)).resetActiveSlotByPartIdAndTargetCode(PART_ID, TARGET);
    }
}
