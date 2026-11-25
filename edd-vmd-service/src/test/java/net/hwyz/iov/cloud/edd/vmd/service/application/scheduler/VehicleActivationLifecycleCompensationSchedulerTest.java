package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationLifecycleCompensationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活生命周期补偿任务单元测试（VMD-DSN-CR-079 / D079-05、D079-06、D079-07）
 * <p>
 * 覆盖 TC079-08/09/11：未建档对账、存量对齐开关（默认关闭，开启后仅一次）、
 * backlog 指标更新、任务异常不影响消费链路。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleActivationLifecycleCompensationScheduler 测试")
class VehicleActivationLifecycleCompensationSchedulerTest {

    private static final String TOPIC = "tsp.user-vehicle-activation-fact";

    @Mock
    private VehicleActivationLifecycleCompensationAppService compensationAppService;
    @Mock
    private VmdActivationFactMetrics activationFactMetrics;
    @Mock
    private VmdKafkaTopicProperties topicProperties;

    private VehicleActivationLifecycleCompensationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new VehicleActivationLifecycleCompensationScheduler(
                compensationAppService, activationFactMetrics, topicProperties);
        ReflectionTestUtils.setField(scheduler, "compensationBatchSize", 200);
        ReflectionTestUtils.setField(scheduler, "compensationMaxBatches", 20);
        ReflectionTestUtils.setField(scheduler, "backfillEnabled", false);
        ReflectionTestUtils.setField(scheduler, "backfillBatchSize", 200);
        ReflectionTestUtils.setField(scheduler, "backfillMaxBatches", 20);
    }

    @Test
    @DisplayName("TC079-08：默认只跑未建档对账，并更新 backlog gauge")
    void reconcileOnlyByDefault() {
        when(compensationAppService.reconcileUnresolved(200, 20)).thenReturn(3);
        when(compensationAppService.countUnresolved()).thenReturn(3L);

        scheduler.compensateLifecycle();

        verify(compensationAppService).reconcileUnresolved(200, 20);
        verify(activationFactMetrics).updateLifecyclePending("vin_unresolved", 3L);
        verify(compensationAppService, never()).alignResolved(anyInt(), anyInt());
    }

    @Test
    @DisplayName("TC079-09：存量对齐开启后只执行一次（幂等任务不重复跑）")
    void backfillRunsOnceWhenEnabled() {
        ReflectionTestUtils.setField(scheduler, "backfillEnabled", true);
        when(compensationAppService.alignResolved(200, 20)).thenReturn(1);
        when(compensationAppService.reconcileUnresolved(200, 20)).thenReturn(0);
        when(compensationAppService.countUnresolved()).thenReturn(0L);

        scheduler.compensateLifecycle();
        scheduler.compensateLifecycle();

        verify(compensationAppService, times(1)).alignResolved(200, 20);
        verify(compensationAppService, times(2)).reconcileUnresolved(200, 20);
    }

    @Test
    @DisplayName("TC079-11：任务异常不外抛，记 FAILED 指标（不影响消费链路）")
    void failureIsolation() {
        when(topicProperties.getUserVehicleActivationFact()).thenReturn(TOPIC);
        when(compensationAppService.reconcileUnresolved(200, 20))
                .thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> scheduler.compensateLifecycle());
        verify(activationFactMetrics).recordLifecycle(TOPIC, "FAILED");
        verify(activationFactMetrics, never()).updateLifecyclePending(anyString(), anyLong());
    }
}
