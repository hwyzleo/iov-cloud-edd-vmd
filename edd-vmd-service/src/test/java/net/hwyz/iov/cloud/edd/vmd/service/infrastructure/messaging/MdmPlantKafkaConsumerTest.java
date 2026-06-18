package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MdmPlantKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmPlantKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmPlantKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onPlantEvent应成功处理MDM Plant事件并调用handlePlantEvent")
    void onPlantEvent_shouldSuccessfullyProcessEventAndCallHandlePlantEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-plant-001\",\"version\":1,\"code\":\"PLANT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.org.plant.event", 0, 0L, "key", messageJson);

        MdmPlantEvent testEvent = new MdmPlantEvent("CREATED", "mdm-plant-001", 1L, "PLANT001",
                "工厂1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlantEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPlantEvent(record);

        // Then
        verify(mdmSyncAppService).handlePlantEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onPlantEvent应处理解析失败并记录失败指标")
    void onPlantEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.org.plant.event", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmPlantEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onPlantEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handlePlantEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPlantEvent应处理handlePlantEvent失败并记录失败指标")
    void onPlantEvent_shouldHandleHandlePlantEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-plant-001\",\"version\":1,\"code\":\"PLANT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.org.plant.event", 0, 0L, "key", messageJson);

        MdmPlantEvent testEvent = new MdmPlantEvent("CREATED", "mdm-plant-001", 1L, "PLANT001",
                "工厂1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlantEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handlePlantEvent(testEvent);

        // When
        kafkaConsumer.onPlantEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPlantEvent应记录处理耗时")
    void onPlantEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-plant-001\",\"version\":1,\"code\":\"PLANT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.org.plant.event", 0, 0L, "key", messageJson);

        MdmPlantEvent testEvent = new MdmPlantEvent("CREATED", "mdm-plant-001", 1L, "PLANT001",
                "工厂1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlantEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPlantEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
