package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
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
 * MdmOptionFamilyKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmOptionFamilyKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmOptionFamilyKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onOptionFamilyEvent应成功处理MDM OptionFamily事件并调用handleOptionFamilyEvent")
    void onOptionFamilyEvent_shouldSuccessfullyProcessEventAndCallHandleOptionFamilyEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-of-001\",\"version\":1,\"code\":\"OF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.optionFamily.created", 0, 0L, "key", messageJson);

        MdmOptionFamilyEvent testEvent = new MdmOptionFamilyEvent("CREATED", "mdm-of-001", 1L, "OF001",
                "选项族1", "OptionFamily1", "EXTERIOR", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmOptionFamilyEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onOptionFamilyEvent(record);

        // Then
        verify(mdmSyncAppService).handleOptionFamilyEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onOptionFamilyEvent应处理解析失败并记录失败指标")
    void onOptionFamilyEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.optionFamily.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmOptionFamilyEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onOptionFamilyEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handleOptionFamilyEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onOptionFamilyEvent应处理handleOptionFamilyEvent失败并记录失败指标")
    void onOptionFamilyEvent_shouldHandleHandleOptionFamilyEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-of-001\",\"version\":1,\"code\":\"OF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.optionFamily.created", 0, 0L, "key", messageJson);

        MdmOptionFamilyEvent testEvent = new MdmOptionFamilyEvent("CREATED", "mdm-of-001", 1L, "OF001",
                "选项族1", "OptionFamily1", "EXTERIOR", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmOptionFamilyEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleOptionFamilyEvent(testEvent);

        // When
        kafkaConsumer.onOptionFamilyEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onOptionFamilyEvent应记录处理耗时")
    void onOptionFamilyEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-of-001\",\"version\":1,\"code\":\"OF001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.optionFamily.created", 0, 0L, "key", messageJson);

        MdmOptionFamilyEvent testEvent = new MdmOptionFamilyEvent("CREATED", "mdm-of-001", 1L, "OF001",
                "选项族1", "OptionFamily1", "EXTERIOR", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmOptionFamilyEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onOptionFamilyEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
