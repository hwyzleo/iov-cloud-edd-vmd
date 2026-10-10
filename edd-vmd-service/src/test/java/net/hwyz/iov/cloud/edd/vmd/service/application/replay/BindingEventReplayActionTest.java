package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
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
 * 绑定变更事件重放动作单元测试
 * <p>
 * VMD-DSN-CR-057: 仅对原批次候选范围内、当前仍 active 的绑定生成重放事件；
 * payload 与既有 VehiclePartBindingChangedEvent 契约一致并携带补发元数据。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BindingEventReplayAction 测试")
class BindingEventReplayActionTest {

    @Mock
    private PartInfoRepository partInfoRepository;
    @Mock
    private VehiclePartRepository vehiclePartRepository;
    @Mock
    private MdmVehicleNodeRepository vehicleNodeRepository;
    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private BindingEventReplayAction action;

    @BeforeEach
    void setUp() {
        action = new BindingEventReplayAction(partInfoRepository, vehiclePartRepository, vehicleNodeRepository, topicRoutes);
    }

    private VehicleImportReplayActionContext context(String vin) {
        return VehicleImportReplayActionContext.builder()
                .replayId("R1").vehImportDataId(1L).batchNum("B001").importType("TOL")
                .vin(vin)
                .candidates(List.of(new VehImportReplayExtractor.PartCandidate("PN001", "SN001")))
                .operatorId("op1").operatorName("操作人")
                .build();
    }

    private VehiclePart activeBinding(Long id, String vin, Long partId) {
        return VehiclePart.builder()
                .id(id).vin(vin).partId(partId).vehicleNodeCode("TBOX_5G")
                .bindState(1).bindTime(Instant.parse("2026-10-01T00:00:00Z")).build();
    }

    @Test
    @DisplayName("plan仅返回候选范围内当前active绑定")
    void planReturnsActiveBinding() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("PN001").sn("SN001").build();
        VehiclePart binding = activeBinding(100L, "VIN001", 10L);
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(10L)).thenReturn(binding);

        List<VehicleImportReplayActionTarget> targets = action.plan(context("VIN001"));
        assertEquals(1, targets.size());
        assertEquals("VEHICLE_PART", targets.get(0).aggregateType());
        assertEquals("100", targets.get(0).aggregateId());
        assertEquals("PN001:SN001", targets.get(0).sourceRecordId());
        assertTrue(targets.get(0).aggregateVersion() > 0);
    }

    @Test
    @DisplayName("候选零件无active绑定或不属于该VIN时plan为空")
    void planSkipsNonActiveOrWrongVin() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("PN001").sn("SN001").build();
        VehiclePart bindingOtherVin = activeBinding(100L, "VIN_OTHER", 10L);
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(10L)).thenReturn(bindingOtherVin);

        assertTrue(action.plan(context("VIN001")).isEmpty());

        // partInfo 不存在也返回空
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(null);
        assertTrue(action.plan(context("VIN001")).isEmpty());
    }

    @Test
    @DisplayName("execute应构造绑定事件Outbox（含补发元数据与TBOX iccid）")
    void executeBuildsOutbox() {
        PartInfo partInfo = PartInfo.builder().id(10L).partCode("PN001").sn("SN001")
                .extra("{\"iccid1\":\"898601\",\"iccid2\":\"898602\"}").build();
        VehiclePart binding = activeBinding(100L, "VIN001", 10L);
        VehicleNode node = VehicleNode.builder().code("TBOX_5G").deviceCategory("TBOX").build();

        when(vehiclePartRepository.selectById(100L)).thenReturn(binding);
        when(partInfoRepository.selectById(10L)).thenReturn(partInfo);
        when(vehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(node);
        when(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_BINDING_CHANGED)).thenReturn("vmd.vehicle-part-binding.changed");

        VehicleImportReplayActionContext ctx = context("VIN001");
        VehicleImportReplayActionTarget target = new VehicleImportReplayActionTarget(
                "VEHICLE_PART", "100", 1000000L, "PN001:SN001");
        VehicleImportReplayActionResult result = action.execute(ctx, target);

        assertEquals(VehicleImportReplayActionResult.Outcome.QUEUED, result.getOutcome());
        VmdOutbox outbox = result.getOutbox();
        assertNotNull(outbox);
        assertEquals("VehiclePartBindingChangedEvent", outbox.getEventType());
        assertEquals("VEHICLE_PART", outbox.getAggregateType());
        assertEquals("VIN001", outbox.getMessageKey());
        assertEquals("vmd.vehicle-part-binding.changed", outbox.getTopic());
        assertEquals("IMPORT_EVENT_REPLAY", outbox.getSourceType());
        assertEquals("R1", outbox.getSourceRefId());
        String payload = outbox.getPayload();
        assertTrue(payload.contains("\"changeType\":\"BIND\""));
        assertTrue(payload.contains("\"iccid1\":\"898601\""));
        assertTrue(payload.contains("\"iccid2\":\"898602\""));
        assertTrue(payload.contains("\"replay\":true"));
        assertTrue(payload.contains("\"replayId\":\"R1\""));
    }

    @Test
    @DisplayName("execute对已失效绑定返回SKIPPED")
    void executeSkipsInactiveBinding() {
        VehiclePart binding = activeBinding(100L, "VIN001", 10L);
        binding.setBindState(0);
        when(vehiclePartRepository.selectById(100L)).thenReturn(binding);

        VehicleImportReplayActionContext ctx = context("VIN001");
        VehicleImportReplayActionTarget target = new VehicleImportReplayActionTarget(
                "VEHICLE_PART", "100", 1000000L, "PN001:SN001");
        VehicleImportReplayActionResult result = action.execute(ctx, target);
        assertEquals(VehicleImportReplayActionResult.Outcome.SKIPPED, result.getOutcome());
        assertTrue(result.getSkipReason().startsWith("BINDING_NOT_ACTIVE"));
    }
}
