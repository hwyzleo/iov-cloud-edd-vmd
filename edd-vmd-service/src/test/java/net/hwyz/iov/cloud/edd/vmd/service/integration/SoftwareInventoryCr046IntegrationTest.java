package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SoftwareInventoryConsumeAuditRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.VehicleSoftwareInventoryObservedConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OTA 车辆软件观测消费 真实数据库集成测试
 * <p>
 * VMD-DSN-CR-046: 事件 → 绑定解析 → 消解写入 part_software_installation →
 * 消费审计 SUCCESS → vmd_outbox 事件（VehicleSoftwareInventoryChangedEvent）。
 * 继承 BaseTest 连接 dev 库，@Rollback 自动回滚。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Transactional
class SoftwareInventoryCr046IntegrationTest extends BaseTest {

    @Autowired
    private VehicleSoftwareInventoryObservedConsumer consumer;

    @Autowired
    private PartInfoRepository partInfoRepository;

    @Autowired
    private VehiclePartRepository vehiclePartRepository;

    @Autowired
    private PartSoftwareInstallationRepository partSoftwareInstallationRepository;

    @Autowired
    private SoftwareInventoryConsumeAuditRepository consumeAuditRepository;

    @Autowired
    private VmdOutboxRepository vmdOutboxRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String suffix;
    private String vin;
    private Long partId;
    private Long bindingId;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        vin = "HWYZTEST" + suffix + "X";
        // 准备物理实例 + active 绑定
        PartInfo partInfo = PartInfo.builder()
                .partCode("PN-" + suffix)
                .sn("SN-" + suffix)
                .vehicleNodeCode("ECU_TBOX")
                .build();
        partInfoRepository.insert(partInfo);
        partId = partInfo.getId();

        VehiclePart vehiclePart = VehiclePart.builder()
                .vin(vin)
                .partId(partId)
                .vehicleNodeCode("ECU_TBOX")
                .position("pos-1")
                .bindState(BindState.ACTIVE.value)
                .bindTime(Instant.now())
                .build();
        vehiclePartRepository.insert(vehiclePart);
        bindingId = vehiclePart.getId();
    }

    private String eventJson() throws Exception {
        VehicleSoftwareInventoryObservedEvent event = VehicleSoftwareInventoryObservedEvent.builder()
                .eventId("evt-" + suffix)
                .observationKey("obs-" + suffix)
                .vin(vin)
                .inventoryRevision("rev-1")
                .inventoryModel("MULTI_TARGET")
                .collectedAt(Instant.parse("2026-09-08T10:00:00Z"))
                .acceptedAt(Instant.parse("2026-09-08T10:01:00Z"))
                .canonicalizationVersion(1)
                .canonicalDigest("sha-" + suffix)
                .items(List.of(
                        VehicleSoftwareInventoryObservedEvent.Item.builder()
                                .ecuId("ECU_TBOX")
                                .hardwarePartNumber("HW-01")
                                .hwVersion("1.0")
                                .softwareTargetCode("TBOX_APP")
                                .softwarePartNumber("SW-PN-01")
                                .swVersion("1.2.0")
                                .slot("A")
                                .active(true)
                                .digest("digest-" + suffix)
                                .build()))
                .build();
        return objectMapper.writeValueAsString(event);
    }

    @Test
    @DisplayName("观测事件端到端：写入实装记录 + 消费审计 SUCCESS + Outbox 事件")
    void observedEvent_shouldWriteInventoryAndAuditAndOutbox() throws Exception {
        // Given
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("ota.vehicle-software-inventory.observed", 0, 0L, vin, eventJson());

        // When
        consumer.onObservedEvent(record);

        // Then 1：part_software_installation 写入（含 CR-046 列）
        PartSoftwareInstallation installation = partSoftwareInstallationRepository
                .selectActiveByPartIdTargetCodeAndSlot(partId, "TBOX_APP", "A");
        assertNotNull(installation, "软件实装记录应写入");
        assertEquals("1.2.0", installation.getSoftwareVersion());
        assertEquals("VEHICLE_REPORT", installation.getSource());
        assertTrue(installation.getIsConfirmed());
        assertTrue(installation.getIsActiveSlot());
        assertEquals("obs-" + suffix, installation.getObservationKey());
        assertEquals(1, installation.getCanonicalizationVersion());
        assertEquals("sha-" + suffix, installation.getCanonicalDigest());
        assertNotNull(installation.getSourceAcceptedAt());

        // Then 2：消费审计 SUCCESS
        SoftwareInventoryConsumeAudit audit = consumeAuditRepository.selectByEventId("evt-" + suffix);
        assertNotNull(audit);
        assertEquals("SUCCESS", audit.getStatus());
        assertEquals(1, audit.getItemTotal());
        assertEquals(1, audit.getItemApplied());
        assertEquals(0, audit.getItemQuarantined());

        // Then 3：vmd_outbox 存在软件清单变更事件（Key=VIN，PENDING）
        VmdOutbox query = VmdOutbox.builder().aggregateId(vin)
                .eventType("VehicleSoftwareInventoryChangedEvent").build();
        List<VmdOutbox> outboxList = vmdOutboxRepository.selectList(query);
        assertFalse(outboxList.isEmpty(), "Outbox 应存在软件清单变更事件");
        assertEquals(vin, outboxList.get(0).getMessageKey());
        assertEquals("PENDING", outboxList.get(0).getPublishState());
        assertTrue(outboxList.get(0).getPayload().contains("TBOX_APP"));
        assertTrue(outboxList.get(0).getPayload().contains("\"active\":true"));
    }

    @Test
    @DisplayName("重复 eventId 投递：幂等跳过，不重复写历史")
    void duplicateEvent_shouldIdempotentSkip() throws Exception {
        // Given
        String json = eventJson();
        consumer.onObservedEvent(new ConsumerRecord<>("ota.vehicle-software-inventory.observed", 0, 0L, vin, json));

        // When：重复投递
        consumer.onObservedEvent(new ConsumerRecord<>("ota.vehicle-software-inventory.observed", 0, 1L, vin, json));

        // Then：实装记录仅一条 ACTIVE，审计仅一条
        PartSoftwareInstallation installation = partSoftwareInstallationRepository
                .selectActiveByPartIdTargetCodeAndSlot(partId, "TBOX_APP", "A");
        assertNotNull(installation);
        List<PartSoftwareInstallation> all = partSoftwareInstallationRepository.selectByPartId(partId);
        assertEquals(1, all.size(), "重复投递不应产生重复历史");
        assertEquals(1L, installation.getInventoryVersion());
    }

    @Test
    @DisplayName("无 active 绑定 item：隔离并告警，不自动建绑定")
    void noBindingItem_shouldQuarantine() throws Exception {
        // Given：事件指向不存在的 ECU 节点
        String json = objectMapper.writeValueAsString(VehicleSoftwareInventoryObservedEvent.builder()
                .eventId("evt-nb-" + suffix)
                .observationKey("obs-nb-" + suffix)
                .vin(vin)
                .inventoryRevision("rev-1")
                .collectedAt(Instant.parse("2026-09-08T10:00:00Z"))
                .canonicalizationVersion(1)
                .canonicalDigest("sha-nb-" + suffix)
                .items(List.of(VehicleSoftwareInventoryObservedEvent.Item.builder()
                        .ecuId("ECU_NOT_EXIST")
                        .softwareTargetCode("TBOX_APP")
                        .softwarePartNumber("SW-PN")
                        .swVersion("1.0.0")
                        .build()))
                .build());

        // When
        consumer.onObservedEvent(new ConsumerRecord<>("ota.vehicle-software-inventory.observed", 0, 0L, vin, json));

        // Then：item 隔离，审计 QUARANTINED，无实装记录写入
        SoftwareInventoryConsumeAudit audit = consumeAuditRepository.selectByEventId("evt-nb-" + suffix);
        assertNotNull(audit);
        assertEquals("QUARANTINED", audit.getStatus());
        assertEquals(1, audit.getItemQuarantined());
        assertTrue(audit.getQuarantineDetail().contains("ECU_NOT_EXIST"));
        List<PartSoftwareInstallation> all = partSoftwareInstallationRepository.selectByPartId(partId);
        assertTrue(all.isEmpty(), "隔离 item 不得写入实装记录");
    }
}
