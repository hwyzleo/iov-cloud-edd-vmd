package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.VehicleActivationLifecycleWriter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活事实 → 生命周期补偿应用服务单元测试（VMD-DSN-CR-079 / D079-05、D079-06）
 * <p>
 * 覆盖 TC079-07/08/09/10：未建档暂不写、建档后补偿、重复补偿幂等、存量对齐、
 * 既有更早节点不被覆盖、失败不抛出且留待下一轮。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleActivationLifecycleCompensationAppService 测试")
class VehicleActivationLifecycleCompensationAppServiceTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String EVENT_ID = "UVA20261122094501123456";
    private static final String TOPIC = "tsp.user-vehicle-activation-fact";
    private static final LocalDateTime EARLIEST = LocalDateTime.ofInstant(
            Instant.parse("2026-11-20T01:00:00.000Z"), ZoneOffset.UTC);

    @Mock
    private VehicleActivationNodeRepository activationNodeRepository;
    @Mock
    private VehicleActivationFactConsumeRepository activationFactConsumeRepository;
    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;
    @Mock
    private VehicleActivationLifecycleWriter lifecycleWriter;
    @Mock
    private VmdActivationFactMetrics activationFactMetrics;
    @Mock
    private VmdKafkaTopicProperties topicProperties;
    @Mock
    private PlatformTransactionManager transactionManager;

    private VehicleActivationLifecycleCompensationAppService appService;

    @BeforeEach
    void setUp() {
        appService = new VehicleActivationLifecycleCompensationAppService(
                activationNodeRepository, activationFactConsumeRepository, vehBasicInfoRepository,
                lifecycleWriter, activationFactMetrics, topicProperties, transactionManager);
    }

    private VehicleActivationNode projection(Long id, Integer vinResolved) {
        return VehicleActivationNode.builder()
                .id(id)
                .vin(VIN)
                .firstActivatedAt(EARLIEST)
                .occurredAt(EARLIEST)
                .bindingOperationId(OP)
                .eventId(EVENT_ID)
                .vinResolved(vinResolved)
                .build();
    }

    private VehicleBasicInfo vehicle() {
        return VehicleBasicInfo.builder().vin(VIN).build();
    }

    private void stubTopic() {
        when(topicProperties.getUserVehicleActivationFact()).thenReturn(TOPIC);
    }

    @Test
    @DisplayName("TC079-08：已建档 → 写 VEHICLE_ACTIVE 并把 vin_resolved 置 1")
    void compensate_writesLifecycleAndMarksResolved() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(11L, 0));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(lifecycleWriter.write(VIN, EARLIEST))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .applied(EARLIEST.toInstant(ZoneOffset.UTC)));
        stubTopic();

        VehicleActivationLifecycleCompensationAppService.CompensationOutcome outcome =
                appService.compensate(VIN);

        assertEquals("APPLIED", outcome.result());
        verify(activationNodeRepository).updateVinResolved(11L, 1);
        verify(activationFactMetrics).recordLifecycle(TOPIC, "APPLIED");
        verify(activationFactConsumeRepository, never()).update(any());
    }

    @Test
    @DisplayName("TC079-07：未建档 → 不写生命周期、保留 vin_resolved=0、记 PENDING_VIN")
    void compensate_pendingWhenVinMissing() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(11L, 0));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        stubTopic();

        VehicleActivationLifecycleCompensationAppService.CompensationOutcome outcome =
                appService.compensate(VIN);

        assertEquals("PENDING_VIN", outcome.result());
        verify(lifecycleWriter, never()).write(any(), any());
        verify(activationNodeRepository, never()).updateVinResolved(any(), any());
        verify(activationFactMetrics).recordLifecycle(TOPIC, "PENDING_VIN");
    }

    @Test
    @DisplayName("无激活投影 → SKIPPED，不产生任何写入")
    void compensate_skipsWhenNoProjection() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(null);

        VehicleActivationLifecycleCompensationAppService.CompensationOutcome outcome =
                appService.compensate(VIN);

        assertEquals("SKIPPED", outcome.result());
        verify(lifecycleWriter, never()).write(any(), any());
    }

    @Test
    @DisplayName("TC079-08：重复补偿幂等 → DUPLICATE，不新增节点，仍标记已解析")
    void compensate_idempotent() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(11L, 1));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(lifecycleWriter.write(VIN, EARLIEST))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .duplicate(EARLIEST.toInstant(ZoneOffset.UTC)));
        stubTopic();

        VehicleActivationLifecycleCompensationAppService.CompensationOutcome outcome =
                appService.compensate(VIN);

        assertEquals("DUPLICATE", outcome.result());
        verify(activationNodeRepository, never()).updateVinResolved(any(), any());
        verify(activationFactMetrics).recordLifecycle(TOPIC, "DUPLICATE");
    }

    @Test
    @DisplayName("TC079-03：向前纠正 → 消费台账追加 LIFECYCLE_CORRECTED 证据")
    void compensate_corrected_appendsEvidence() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(11L, 0));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(lifecycleWriter.write(VIN, EARLIEST))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome.corrected(
                        Instant.parse("2026-11-22T09:45:01.234Z"),
                        EARLIEST.toInstant(ZoneOffset.UTC)));
        VehicleActivationFactConsume consume = VehicleActivationFactConsume.builder()
                .id(5L)
                .eventId(EVENT_ID)
                .vin(VIN)
                .bindingOperationId(OP)
                .description("CORRECTED")
                .build();
        when(activationFactConsumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(consume);
        stubTopic();

        appService.compensate(VIN);

        ArgumentCaptor<VehicleActivationFactConsume> captor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(activationFactConsumeRepository).update(captor.capture());
        String description = captor.getValue().getDescription();
        assertTrue(description.contains("LIFECYCLE_CORRECTED"), "应留下生命周期纠正证据");
        assertTrue(description.contains("event=" + EVENT_ID));
        assertTrue(description.contains("CORRECTED"), "原有投影纠正备注应保留");
    }

    @Test
    @DisplayName("TC079-10：补偿技术失败 → 静默处理（不抛出）并记 FAILED")
    void compensateQuietly_swallowsFailure() {
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(11L, 0));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));
        stubTopic();

        assertDoesNotThrow(() -> appService.compensateQuietly(VIN));
        verify(activationFactMetrics).recordLifecycle(TOPIC, "FAILED");
    }

    @Test
    @DisplayName("TC079-08：定时对账分页只补偿主档已存在的投影")
    void reconcileUnresolved_scansPagesAndSkipsMissingVehicle() {
        VehicleActivationNode first = projection(11L, 0);
        VehicleActivationNode second = VehicleActivationNode.builder()
                .id(12L)
                .vin("LSVAA1234567890AC")
                .firstActivatedAt(EARLIEST)
                .occurredAt(EARLIEST)
                .bindingOperationId(OP)
                .eventId(EVENT_ID)
                .vinResolved(0)
                .build();
        when(activationNodeRepository.selectPageByVinResolved(0, null, 2))
                .thenReturn(List.of(first, second));
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(first);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(vehBasicInfoRepository.selectByVin("LSVAA1234567890AC")).thenReturn(null);
        when(lifecycleWriter.write(VIN, EARLIEST))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .applied(EARLIEST.toInstant(ZoneOffset.UTC)));
        when(activationNodeRepository.selectPageByVinResolved(0, 12L, 2)).thenReturn(List.of());
        stubTopic();

        int processed = appService.reconcileUnresolved(2, 5);

        assertEquals(1, processed, "只处理主档已存在的投影");
        verify(activationFactMetrics).recordLifecycle(TOPIC, "APPLIED");
        verify(activationNodeRepository, never()).updateVinResolved(eq(12L), any());
    }

    @Test
    @DisplayName("TC079-09：存量对齐分页处理已建档投影（无投影则不处理）")
    void alignResolved_scansResolvedProjections() {
        when(activationNodeRepository.selectPageByVinResolved(1, null, 10))
                .thenReturn(List.of(projection(21L, 1)));
        when(activationNodeRepository.selectByVin(VIN)).thenReturn(projection(21L, 1));
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(vehicle());
        when(lifecycleWriter.write(VIN, EARLIEST))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .duplicate(EARLIEST.toInstant(ZoneOffset.UTC)));
        stubTopic();

        int processed = appService.alignResolved(10, 3);

        assertEquals(1, processed);
        verify(lifecycleWriter).write(VIN, EARLIEST);
        verify(activationNodeRepository, never()).updateVinResolved(any(), any());
    }
}
