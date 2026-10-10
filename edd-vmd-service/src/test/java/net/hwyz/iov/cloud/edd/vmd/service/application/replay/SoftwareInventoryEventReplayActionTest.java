package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 软件实装事件重放动作单元测试
 * <p>
 * VMD-DSN-CR-057: 仅对候选范围内、当前 ACTIVE 的软件实装记录生成重放事件；
 * payload 与既有 VehicleSoftwareInventoryChangedEvent 契约一致并携带补发元数据。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SoftwareInventoryEventReplayAction 测试")
class SoftwareInventoryEventReplayActionTest {

    @Mock
    private PartInfoRepository partInfoRepository;
    @Mock
    private PartSoftwareInstallationRepository partSoftwareInstallationRepository;
    @Mock
    private VehiclePartRepository vehiclePartRepository;
    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private SoftwareInventoryEventReplayAction action;

    @BeforeEach
    void setUp() {
        action = new SoftwareInventoryEventReplayAction(
                partInfoRepository, partSoftwareInstallationRepository, vehiclePartRepository, topicRoutes);
    }

    private VehicleImportReplayActionContext context(String vin) {
        return VehicleImportReplayActionContext.builder()
                .replayId("R1").vehImportDataId(1L).batchNum("B001").importType("EOL")
                .vin(vin)
                .candidates(List.of(new VehImportReplayExtractor.PartCandidate("17300011AA", "SNB001")))
                .operatorId("op1").operatorName("操作人")
                .build();
    }

    private PartSoftwareInstallation activeRecord(Long id, Long partId, Long version, String targetCode) {
        return PartSoftwareInstallation.builder()
                .id(id).partId(partId).bindingId(100L).vinSnapshot("VIN001")
                .softwareTargetCode(targetCode).softwarePartNo("SPN001").softwareVersion("1.0.0")
                .slot(null).installState("ACTIVE").changeType("INITIAL").source("EOL")
                .isConfirmed(Boolean.TRUE).inventoryVersion(version).isActiveSlot(Boolean.TRUE)
                .build();
    }

    private PartSoftwareInstallation inactiveRecord(Long id, Long partId, Long version) {
        PartSoftwareInstallation record = activeRecord(id, partId, version, "TBOX_APP");
        record.setInstallState("INACTIVE");
        return record;
    }

    @Test
    @DisplayName("plan仅返回候选范围内当前ACTIVE且active槽的实装记录")
    void planReturnsActiveRecords() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("17300011AA").sn("SNB001").build();
        VehiclePart binding = VehiclePart.builder().id(100L).vin("VIN001").partId(10L).bindState(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("17300011AA", "SNB001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(10L)).thenReturn(binding);
        when(partSoftwareInstallationRepository.selectByPartId(10L))
                .thenReturn(List.of(activeRecord(1L, 10L, 1L, "TBOX_BOOT"), activeRecord(2L, 10L, 2L, "TBOX_APP"), inactiveRecord(3L, 10L, 3L)));

        List<VehicleImportReplayActionTarget> targets = action.plan(context("VIN001"));
        assertEquals(2, targets.size());
        assertEquals("PART_SOFTWARE_INSTALLATION", targets.get(0).aggregateType());
        assertEquals("10", targets.get(0).aggregateId());
        assertTrue(targets.stream().anyMatch(t -> 1L == t.aggregateVersion()));
        assertTrue(targets.stream().anyMatch(t -> 2L == t.aggregateVersion()));
    }

    @Test
    @DisplayName("零件无active绑定或归属其他VIN时plan为空")
    void planSkipsWhenBindingChanged() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("17300011AA").sn("SNB001").build();
        VehiclePart binding = VehiclePart.builder().id(100L).vin("VIN_OTHER").partId(10L).bindState(1).build();
        when(partInfoRepository.selectByPartCodeAndSn("17300011AA", "SNB001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(10L)).thenReturn(binding);
        assertTrue(action.plan(context("VIN001")).isEmpty());
    }

    @Test
    @DisplayName("execute应构造软件实装事件Outbox（含补发元数据）")
    void executeBuildsOutbox() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("17300011AA").sn("SNB001").build();
        VehiclePart binding = VehiclePart.builder().id(100L).vin("VIN001").partId(10L)
                .vehicleNodeCode("TBOX_5G").bindState(1).build();
        PartSoftwareInstallation record = activeRecord(1L, 10L, 5L, "TBOX_APP");
        when(partInfoRepository.selectById(10L)).thenReturn(partInfo);
        when(vehiclePartRepository.selectById(100L)).thenReturn(binding);
        when(partSoftwareInstallationRepository.selectByPartId(10L)).thenReturn(List.of(record));
        when(topicRoutes.topicName(VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED))
                .thenReturn("vmd.vehicle-software-inventory.changed");

        VehicleImportReplayActionContext ctx = context("VIN001");
        VehicleImportReplayActionTarget target = new VehicleImportReplayActionTarget(
                "PART_SOFTWARE_INSTALLATION", "10", 5L, "17300011AA:SNB001");
        VehicleImportReplayActionResult result = action.execute(ctx, target);

        assertEquals(VehicleImportReplayActionResult.Outcome.QUEUED, result.getOutcome());
        VmdOutbox outbox = result.getOutbox();
        assertNotNull(outbox);
        assertEquals("VehicleSoftwareInventoryChangedEvent", outbox.getEventType());
        assertEquals("PART_SOFTWARE_INSTALLATION", outbox.getAggregateType());
        assertEquals("10", outbox.getAggregateId());
        assertEquals(5L, outbox.getAggregateVersion());
        assertEquals("VIN001", outbox.getMessageKey());
        assertEquals("IMPORT_EVENT_REPLAY", outbox.getSourceType());
        String payload = outbox.getPayload();
        assertTrue(payload.contains("\"softwareTargetCode\":\"TBOX_APP\""));
        assertTrue(payload.contains("\"inventoryVersion\":5"));
        assertTrue(payload.contains("\"replay\":true"));
        assertTrue(payload.contains("\"replayId\":\"R1\""));
    }

    @Test
    @DisplayName("execute对非ACTIVE记录返回SKIPPED")
    void executeSkipsNonActive() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("17300011AA").sn("SNB001").build();
        when(partInfoRepository.selectById(10L)).thenReturn(partInfo);
        when(partSoftwareInstallationRepository.selectByPartId(10L)).thenReturn(List.of(inactiveRecord(1L, 10L, 5L)));

        VehicleImportReplayActionContext ctx = context("VIN001");
        VehicleImportReplayActionTarget target = new VehicleImportReplayActionTarget(
                "PART_SOFTWARE_INSTALLATION", "10", 5L, "17300011AA:SNB001");
        VehicleImportReplayActionResult result = action.execute(ctx, target);
        assertEquals(VehicleImportReplayActionResult.Outcome.SKIPPED, result.getOutcome());
        assertTrue(result.getSkipReason().startsWith("SOFTWARE_RECORD_NOT_ACTIVE"));
    }
}
