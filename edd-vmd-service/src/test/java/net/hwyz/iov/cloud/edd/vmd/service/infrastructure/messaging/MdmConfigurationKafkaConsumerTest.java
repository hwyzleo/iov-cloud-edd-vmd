package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
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
 * MdmConfigurationKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmConfigurationKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmConfigurationKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onConfigurationEvent应成功处理MDM Configuration事件并调用handleConfigurationEvent")
    void onConfigurationEvent_shouldSuccessfullyProcessEventAndCallHandleConfigurationEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cfg-001\",\"version\":1,\"code\":\"CFG001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", messageJson);

        MdmConfigurationEvent testEvent = new MdmConfigurationEvent("CREATED", "mdm-cfg-001", 1L, "CFG001",
                "配置1", "Config1", "PF001", "CL001", "MODEL001", "VAR001", "STAGE001", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmConfigurationEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(mdmSyncAppService).handleConfigurationEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onConfigurationEvent应处理解析失败并记录失败指标")
    void onConfigurationEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmConfigurationEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(mdmSyncAppService, never()).handleConfigurationEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onConfigurationEvent应处理handleConfigurationEvent失败并记录失败指标")
    void onConfigurationEvent_shouldHandleHandleConfigurationEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cfg-001\",\"version\":1,\"code\":\"CFG001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", messageJson);

        MdmConfigurationEvent testEvent = new MdmConfigurationEvent("CREATED", "mdm-cfg-001", 1L, "CFG001",
                "配置1", "Config1", "PF001", "CL001", "MODEL001", "VAR001", "STAGE001", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmConfigurationEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleConfigurationEvent(testEvent);

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onConfigurationEvent应记录处理耗时")
    void onConfigurationEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cfg-001\",\"version\":1,\"code\":\"CFG001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", messageJson);

        MdmConfigurationEvent testEvent = new MdmConfigurationEvent("CREATED", "mdm-cfg-001", 1L, "CFG001",
                "配置1", "Config1", "PF001", "CL001", "MODEL001", "VAR001", "STAGE001", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmConfigurationEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
