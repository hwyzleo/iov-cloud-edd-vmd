package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MdmVehicleNodeKafkaConsumer单元测试
 *
 * @author CR-024
 */
@ExtendWith(MockitoExtension.class)
class MdmVehicleNodeKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmVehicleNodeKafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("onVehicleNodeEvent应成功处理MDM车载节点事件并调用handleVehicleNodeEvent")
    void onVehicleNodeEvent_shouldSuccessfullyProcessEventAndCallHandleVehicleNodeEvent() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-vn-001\",\"version\":1,\"code\":\"CPT_DCU_8295\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.eead.vehicleNode.event", 0, 0L, "key", messageJson);

        MdmVehicleNodeEvent testEvent = new MdmVehicleNodeEvent("CREATED", "mdm-vn-001", 1L, "CPT_DCU_8295",
                "车载节点1", "Vehicle Node 1", "DCU",
                "EEAD", "CONTROLLER", "FOTA", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmVehicleNodeEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onVehicleNodeEvent(record);

        // Then
        verify(mdmSyncAppService).handleVehicleNodeEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("onVehicleNodeEvent应处理解析失败并记录失败指标，异常重新抛出交给框架重试")
    void onVehicleNodeEvent_shouldHandleParseFailureAndRecordFailureMetric() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.eead.vehicleNode.event", 0, 0L, "key", invalidJson);

        when(objectMapper.readValue(invalidJson, MdmVehicleNodeEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        // When / Then
        assertThrows(RuntimeException.class, () -> kafkaConsumer.onVehicleNodeEvent(record));
        verify(mdmSyncAppService, never()).handleVehicleNodeEvent(any());
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onVehicleNodeEvent应处理handleVehicleNodeEvent失败并记录失败指标，异常重新抛出交给框架重试")
    void onVehicleNodeEvent_shouldHandleHandleVehicleNodeEventFailureAndRecordFailureMetric() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-vn-001\",\"version\":1,\"code\":\"CPT_DCU_8295\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.eead.vehicleNode.event", 0, 0L, "key", messageJson);

        MdmVehicleNodeEvent testEvent = new MdmVehicleNodeEvent("CREATED", "mdm-vn-001", 1L, "CPT_DCU_8295",
                "车载节点1", "Vehicle Node 1", "DCU",
                "EEAD", "CONTROLLER", "FOTA", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmVehicleNodeEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleVehicleNodeEvent(testEvent);

        // When / Then
        assertThrows(RuntimeException.class, () -> kafkaConsumer.onVehicleNodeEvent(record));
        verify(mdmSyncMetrics).recordFailure();
    }

    @Test
    @DisplayName("onVehicleNodeEvent应记录处理耗时")
    void onVehicleNodeEvent_shouldRecordProcessingDuration() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-vn-001\",\"version\":1,\"code\":\"CPT_DCU_8295\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.eead.vehicleNode.event", 0, 0L, "key", messageJson);

        MdmVehicleNodeEvent testEvent = new MdmVehicleNodeEvent("CREATED", "mdm-vn-001", 1L, "CPT_DCU_8295",
                "车载节点1", "Vehicle Node 1", "DCU",
                "EEAD", "CONTROLLER", "FOTA", true, 1, LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmVehicleNodeEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onVehicleNodeEvent(record);

        // Then
        verify(mdmSyncMetrics).recordDuration(anyLong());
    }

    @Test
    @DisplayName("onVehicleNodeEvent应解析MDM事件中的hsmCapability并透传（CR-049）")
    void onVehicleNodeEvent_shouldParseAndPassHsmCapability() throws Exception {
        // Given
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-vn-ccu2\",\"version\":12,\"code\":\"CCU_GEN2\","
                + "\"name\":\"中央计算单元GEN2\",\"deviceCategory\":\"CCU\",\"hsmCapability\":\"HSM_FULL\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.eead.vehicleNode.event", 0, 0L, "key", messageJson);

        MdmVehicleNodeEvent testEvent = new MdmVehicleNodeEvent("CREATED", "mdm-vn-ccu2", 12L, "CCU_GEN2",
                "中央计算单元GEN2", "Central Computing Unit Gen2", "CCU",
                "EEAD", "CONTROLLER", "FOTA", true, 1, "HSM_FULL", LocalDateTime.now());

        when(objectMapper.readValue(messageJson, MdmVehicleNodeEvent.class)).thenReturn(testEvent);

        // When
        kafkaConsumer.onVehicleNodeEvent(record);

        // Then
        verify(mdmSyncAppService).handleVehicleNodeEvent(argThat(event ->
                "CCU_GEN2".equals(event.getCode())
                        && "HSM_FULL".equals(event.getHsmCapability())
                        && "CCU".equals(event.getDeviceCategory())));
        verify(mdmSyncMetrics).recordSuccess();
    }
}
