package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
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
    private ConfigurationSyncMetrics configurationSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmConfigurationKafkaConsumer kafkaConsumer;

    private MdmConfigurationEvent buildEvent(String eventType, String entityId, Long version, String code) {
        return new MdmConfigurationEvent(eventType, entityId, version, code,
                "配置1", "Config1Local", "VAR001", "desc", LocalDateTime.now());
    }

    @Test
    @DisplayName("onConfigurationEvent应成功处理MDM Configuration事件并调用handleConfigurationEvent")
    void onConfigurationEvent_shouldSuccessfullyProcessEventAndCallHandleConfigurationEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cfg-001\",\"version\":1,\"code\":\"CFG001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", messageJson);

        MdmConfigurationEvent testEvent = buildEvent("CREATED", "mdm-cfg-001", 1L, "CFG001");

        when(objectMapper.readValue(messageJson, MdmConfigurationEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(mdmSyncAppService).handleConfigurationEvent(testEvent);
        verify(configurationSyncMetrics, never()).recordFailure();
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
        verify(configurationSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onConfigurationEvent应处理handleConfigurationEvent失败（如缺variantCode契约错误）并记录失败指标")
    void onConfigurationEvent_shouldHandleHandleConfigurationEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-cfg-001\",\"version\":1,\"code\":\"CFG001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.product.configuration.created", 0, 0L, "key", messageJson);

        MdmConfigurationEvent testEvent = buildEvent("CREATED", "mdm-cfg-001", 1L, "CFG001");

        when(objectMapper.readValue(messageJson, MdmConfigurationEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleConfigurationEvent(testEvent);

        // When
        kafkaConsumer.onConfigurationEvent(record);

        // Then
        verify(configurationSyncMetrics).recordFailure();
    }
}
