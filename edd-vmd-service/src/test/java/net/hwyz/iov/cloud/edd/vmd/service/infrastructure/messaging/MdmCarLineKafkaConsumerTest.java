package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
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
 * MdmCarLineKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmCarLineKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmCarLineKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onCarLineEvent应成功处理MDM CarLine事件并调用handleSeriesEvent")
    void onCarLineEvent_shouldSuccessfullyProcessEventAndCallHandleSeriesEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cl-001\",\"version\":1,\"code\":\"CL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.carLine.created", 0, 0L, "key", messageJson);

        MdmCarLineEvent testEvent = new MdmCarLineEvent("CREATED", "mdm-cl-001", 1L, "CL001", "车系1", "BRAND001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmCarLineEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onCarLineEvent(record);

        // Then
        verify(mdmSyncAppService).handleSeriesEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onCarLineEvent应处理解析失败并记录失败指标")
    void onCarLineEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.carLine.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmCarLineEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onCarLineEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handleSeriesEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onCarLineEvent应处理handleSeriesEvent失败并记录失败指标")
    void onCarLineEvent_shouldHandleHandleSeriesEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cl-001\",\"version\":1,\"code\":\"CL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.carLine.created", 0, 0L, "key", messageJson);

        MdmCarLineEvent testEvent = new MdmCarLineEvent("CREATED", "mdm-cl-001", 1L, "CL001", "车系1", "BRAND001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmCarLineEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleSeriesEvent(testEvent);

        // When
        kafkaConsumer.onCarLineEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onCarLineEvent应记录处理耗时")
    void onCarLineEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cl-001\",\"version\":1,\"code\":\"CL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.carLine.created", 0, 0L, "key", messageJson);

        MdmCarLineEvent testEvent = new MdmCarLineEvent("CREATED", "mdm-cl-001", 1L, "CL001", "车系1", "BRAND001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmCarLineEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onCarLineEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
