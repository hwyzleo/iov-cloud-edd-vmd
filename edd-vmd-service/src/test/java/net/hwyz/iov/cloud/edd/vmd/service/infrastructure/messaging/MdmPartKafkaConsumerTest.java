package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPartEvent;
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
 * MdmPartKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmPartKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmPartKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onPartEvent应成功处理MDM Part事件并调用handlePartEvent")
    void onPartEvent_shouldSuccessfullyProcessEventAndCallHandlePartEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-part-001\",\"version\":1,\"code\":\"PART001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.material.part.event", 0, 0L, "key", messageJson);

        MdmPartEvent testEvent = new MdmPartEvent("CREATED", "mdm-part-001", 1L, "PART001",
                "零件1", "NORMAL", "NODE001", "SUPPLIER001", true, true, true, "PRODUCTION", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPartEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPartEvent(record);

        // Then
        verify(mdmSyncAppService).handlePartEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onPartEvent应处理解析失败并记录失败指标")
    void onPartEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.material.part.event", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmPartEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onPartEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handlePartEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPartEvent应处理handlePartEvent失败并记录失败指标")
    void onPartEvent_shouldHandleHandlePartEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-part-001\",\"version\":1,\"code\":\"PART001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.material.part.event", 0, 0L, "key", messageJson);

        MdmPartEvent testEvent = new MdmPartEvent("CREATED", "mdm-part-001", 1L, "PART001",
                "零件1", "NORMAL", "NODE001", "SUPPLIER001", true, true, true, "PRODUCTION", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPartEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handlePartEvent(testEvent);

        // When
        kafkaConsumer.onPartEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPartEvent应记录处理耗时")
    void onPartEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-part-001\",\"version\":1,\"code\":\"PART001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.material.part.event", 0, 0L, "key", messageJson);

        MdmPartEvent testEvent = new MdmPartEvent("CREATED", "mdm-part-001", 1L, "PART001",
                "零件1", "NORMAL", "NODE001", "SUPPLIER001", true, true, true, "PRODUCTION", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPartEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPartEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
