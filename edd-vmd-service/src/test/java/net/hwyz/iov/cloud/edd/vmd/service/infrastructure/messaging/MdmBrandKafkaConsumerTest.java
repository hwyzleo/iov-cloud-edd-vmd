package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
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
 * MdmBrandKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmBrandKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmBrandKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onBrandEvent应成功处理MDM Brand事件并调用handleBrandEvent")
    void onBrandEvent_shouldSuccessfullyProcessEventAndCallHandleBrandEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-brand-001\",\"version\":1,\"code\":\"BRAND001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.brand.created", 0, 0L, "key", messageJson);

        MdmBrandEvent testEvent = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "品牌1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmBrandEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onBrandEvent(record);

        // Then
        verify(mdmSyncAppService).handleBrandEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onBrandEvent应处理解析失败并记录失败指标")
    void onBrandEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.brand.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmBrandEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onBrandEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handleBrandEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onBrandEvent应处理handleBrandEvent失败并记录失败指标")
    void onBrandEvent_shouldHandleHandleBrandEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-brand-001\",\"version\":1,\"code\":\"BRAND001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.brand.created", 0, 0L, "key", messageJson);

        MdmBrandEvent testEvent = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "品牌1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmBrandEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleBrandEvent(testEvent);

        // When
        kafkaConsumer.onBrandEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onBrandEvent应记录处理耗时")
    void onBrandEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-brand-001\",\"version\":1,\"code\":\"BRAND001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.brand.created", 0, 0L, "key", messageJson);

        MdmBrandEvent testEvent = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "品牌1", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmBrandEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onBrandEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
