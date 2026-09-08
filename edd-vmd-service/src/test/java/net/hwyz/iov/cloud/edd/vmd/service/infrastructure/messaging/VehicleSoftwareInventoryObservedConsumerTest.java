package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.InventoryObservedMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ApplySoftwareManifestItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSoftwareBindingResolver;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SoftwareInventoryConsumeAuditRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VehicleSoftwareInventoryObservedConsumer 单元测试
 * <p>
 * VMD-DSN-CR-046: 事件级幂等（eventId + observationKey）、绑定隔离、DLQ
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class VehicleSoftwareInventoryObservedConsumerTest {

    @Mock
    private SoftwareInventoryAppService softwareInventoryAppService;
    @Mock
    private VehicleSoftwareBindingResolver bindingResolver;
    @Mock
    private InventoryObservedMapper inventoryObservedMapper;
    @Mock
    private SoftwareInventoryConsumeAuditRepository consumeAuditRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private VehicleSoftwareInventoryObservedConsumer consumer;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String VIN = "HWYZTEST000000001";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "dlqTopic", "ota.vehicle-software-inventory.observed.dlq");
    }

    private ConsumerRecord<String, String> record(String json) {
        return new ConsumerRecord<>("ota.vehicle-software-inventory.observed", 0, 0L, VIN, json);
    }

    private VehicleSoftwareInventoryObservedEvent validEvent(String eventId, String observationKey) {
        return VehicleSoftwareInventoryObservedEvent.builder()
                .eventId(eventId)
                .observationKey(observationKey)
                .vin(VIN)
                .inventoryRevision("rev-1")
                .inventoryModel("MULTI_TARGET")
                .collectedAt(Instant.parse("2026-09-08T10:00:00Z"))
                .acceptedAt(Instant.parse("2026-09-08T10:01:00Z"))
                .canonicalizationVersion(1)
                .canonicalDigest("sha-001")
                .items(List.of(VehicleSoftwareInventoryObservedEvent.Item.builder()
                        .ecuId("ECU_TBOX")
                        .softwareTargetCode("TBOX_APP")
                        .softwarePartNumber("SW-PN")
                        .swVersion("1.0.0")
                        .slot("A")
                        .active(true)
                        .digest("d-1")
                        .build()))
                .build();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private void stubApplyApplied() {
        when(softwareInventoryAppService.applyManifest(any(), any(), anyString(), anyString(), anyString(),
                anyString(), any(), any(), any(), anyString(), anyString(), any(), any(), any(),
                any(), anyString(), any(), anyString(), any()))
                .thenReturn(new SoftwareInventoryAppService.ApplyManifestResult(true, false, 1L));
    }

    @Test
    void onObservedEvent_normal_shouldApplyAndAuditSuccess() throws Exception {
        // Given
        var event = validEvent("evt-1", "obs-1");
        when(consumeAuditRepository.selectByEventId("evt-1")).thenReturn(null);
        when(consumeAuditRepository.selectByObservationKey("obs-1")).thenReturn(null);
        when(bindingResolver.resolve(VIN, "ECU_TBOX"))
                .thenReturn(new VehicleSoftwareBindingResolver.BindingResolution(100L, 200L));
        when(inventoryObservedMapper.toManifestItem(any(), any(), any()))
                .thenReturn(ApplySoftwareManifestItemCmd.builder().partId(200L).bindingId(100L)
                        .vehicleNodeCode("ECU_TBOX").softwareTargetCode("TBOX_APP")
                        .softwarePartNo("SW-PN").softwareVersion("1.0.0").slot("A")
                        .isActiveSlot(true).changeType("INITIAL")
                        .observationKey("obs-1").canonicalizationVersion(1).canonicalDigest("sha-001")
                        .build());
        stubApplyApplied();

        // When
        consumer.onObservedEvent(record(toJson(event)));

        // Then
        ArgumentCaptor<SoftwareInventoryConsumeAudit> captor = ArgumentCaptor.forClass(SoftwareInventoryConsumeAudit.class);
        verify(consumeAuditRepository).update(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getStatus());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void onObservedEvent_duplicateEventId_shouldIdempotentSkip() throws Exception {
        // Given
        var event = validEvent("evt-1", "obs-1");
        when(consumeAuditRepository.selectByEventId("evt-1"))
                .thenReturn(SoftwareInventoryConsumeAudit.builder()
                        .eventId("evt-1").observationKey("obs-1").status("SUCCESS").build());

        // When
        consumer.onObservedEvent(record(toJson(event)));

        // Then
        verify(softwareInventoryAppService, never()).applyManifest(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(consumeAuditRepository, never()).insert(any());
    }

    @Test
    void onObservedEvent_observationKeyConflict_shouldSendDlq() throws Exception {
        // Given
        var event = validEvent("evt-2", "obs-1");
        when(consumeAuditRepository.selectByEventId("evt-2")).thenReturn(null);
        // 同 observationKey 已被不同 eventId 占用 → 摘要不一致 → DLQ
        when(consumeAuditRepository.selectByObservationKey("obs-1"))
                .thenReturn(SoftwareInventoryConsumeAudit.builder()
                        .eventId("evt-1").observationKey("obs-1").status("SUCCESS").build());

        // When
        consumer.onObservedEvent(record(toJson(event)));

        // Then
        verify(kafkaTemplate).send(eq("ota.vehicle-software-inventory.observed.dlq"), eq(VIN), anyString());
        verify(softwareInventoryAppService, never()).applyManifest(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onObservedEvent_noActiveBinding_shouldQuarantineItem() throws Exception {
        // Given
        var event = validEvent("evt-3", "obs-3");
        when(consumeAuditRepository.selectByEventId("evt-3")).thenReturn(null);
        when(consumeAuditRepository.selectByObservationKey("obs-3")).thenReturn(null);
        when(bindingResolver.resolve(VIN, "ECU_TBOX"))
                .thenThrow(new VehicleSoftwareBindingResolver.ActiveBindingNotFoundException(VIN, "ECU_TBOX"));

        // When
        consumer.onObservedEvent(record(toJson(event)));

        // Then：item 被隔离，不自动建绑定，审计 QUARANTINED，不发 DLQ
        ArgumentCaptor<SoftwareInventoryConsumeAudit> captor = ArgumentCaptor.forClass(SoftwareInventoryConsumeAudit.class);
        verify(consumeAuditRepository).update(captor.capture());
        assertEquals("QUARANTINED", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getItemQuarantined());
        verify(softwareInventoryAppService, never()).applyManifest(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void onObservedEvent_invalidContract_shouldSendDlq() throws Exception {
        // Given：缺 eventId
        String json = "{\"observationKey\":\"obs-x\",\"vin\":\"" + VIN + "\"}";

        // When
        consumer.onObservedEvent(record(json));

        // Then
        verify(kafkaTemplate).send(eq("ota.vehicle-software-inventory.observed.dlq"), eq(VIN), anyString());
        verify(consumeAuditRepository, never()).insert(any());
    }
}
