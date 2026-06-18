package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
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
 * MdmModelKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmModelKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmModelKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onModelEvent应成功处理MDM Model事件并调用handleModelEvent")
    void onModelEvent_shouldSuccessfullyProcessEventAndCallHandleModelEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-model-001\",\"version\":1,\"code\":\"MODEL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.model.created", 0, 0L, "key", messageJson);

        MdmModelEvent testEvent = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001",
                "车型1", "PF001", "CL001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmModelEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onModelEvent(record);

        // Then
        verify(mdmSyncAppService).handleModelEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onModelEvent应处理解析失败并记录失败指标")
    void onModelEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.model.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmModelEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onModelEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handleModelEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onModelEvent应处理handleModelEvent失败并记录失败指标")
    void onModelEvent_shouldHandleHandleModelEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-model-001\",\"version\":1,\"code\":\"MODEL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.model.created", 0, 0L, "key", messageJson);

        MdmModelEvent testEvent = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001",
                "车型1", "PF001", "CL001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmModelEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleModelEvent(testEvent);

        // When
        kafkaConsumer.onModelEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onModelEvent应记录处理耗时")
    void onModelEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-model-001\",\"version\":1,\"code\":\"MODEL001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.model.created", 0, 0L, "key", messageJson);

        MdmModelEvent testEvent = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001",
                "车型1", "PF001", "CL001", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmModelEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onModelEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
