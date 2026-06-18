package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
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
 * MdmPlatformKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmPlatformKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmPlatformKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onPlatformEvent应成功处理MDM Platform事件并调用handlePlatformEvent")
    void onPlatformEvent_shouldSuccessfullyProcessEventAndCallHandlePlatformEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-pf-001\",\"version\":1,\"code\":\"PF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.platform.created", 0, 0L, "key", messageJson);

        MdmPlatformEvent testEvent = new MdmPlatformEvent("CREATED", "mdm-pf-001", 1L, "PF001",
                "平台1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlatformEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPlatformEvent(record);

        // Then
        verify(mdmSyncAppService).handlePlatformEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onPlatformEvent应处理解析失败并记录失败指标")
    void onPlatformEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.platform.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmPlatformEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onPlatformEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handlePlatformEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPlatformEvent应处理handlePlatformEvent失败并记录失败指标")
    void onPlatformEvent_shouldHandleHandlePlatformEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-pf-001\",\"version\":1,\"code\":\"PF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.platform.created", 0, 0L, "key", messageJson);

        MdmPlatformEvent testEvent = new MdmPlatformEvent("CREATED", "mdm-pf-001", 1L, "PF001",
                "平台1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlatformEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handlePlatformEvent(testEvent);

        // When
        kafkaConsumer.onPlatformEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onPlatformEvent应记录处理耗时")
    void onPlatformEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-pf-001\",\"version\":1,\"code\":\"PF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.platform.created", 0, 0L, "key", messageJson);

        MdmPlatformEvent testEvent = new MdmPlatformEvent("CREATED", "mdm-pf-001", 1L, "PF001",
                "平台1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmPlatformEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onPlatformEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
