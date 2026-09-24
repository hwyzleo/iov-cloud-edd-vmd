package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MdmOptionCodeKafkaConsumer单元测试（VMD-DSN-CR-052 新增第 11 个 MDM 消费 Listener）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MdmOptionCodeKafkaConsumer 测试")
class MdmOptionCodeKafkaConsumerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @Mock
    private MdmConsumerMetrics mdmConsumerMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MdmOptionCodeKafkaConsumer kafkaConsumer;

    private MdmOptionCodeEvent event(String code, Long version) {
        return new MdmOptionCodeEvent("CREATED", "mdm-option-code-001", version, code,
                "FAMILY001", "选项值1", "选项值1", LocalDateTime.now());
    }

    @Test
    @DisplayName("成功消费 OptionCode 事件并调用 handleOptionCodeEvent，记录 success 指标")
    void onOptionCodeEvent_shouldSuccessfullyProcessAndRecordConsume() throws Exception {
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-option-code-001\",\"version\":1,\"code\":\"OPT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.option-code", 0, 0L, "key", messageJson);
        MdmOptionCodeEvent testEvent = event("OPT001", 1L);
        when(objectMapper.readValue(messageJson, MdmOptionCodeEvent.class)).thenReturn(testEvent);

        kafkaConsumer.onOptionCodeEvent(record);

        verify(mdmSyncAppService).handleOptionCodeEvent(testEvent);
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmConsumerMetrics).recordConsume(MdmProjectionType.OPTION_CODE, "success");
        verify(mdmConsumerMetrics, never()).recordConsume(MdmProjectionType.OPTION_CODE, "failure");
    }

    @Test
    @DisplayName("解析失败：记录 parse_error 指标，不调用应用服务")
    void onOptionCodeEvent_shouldHandleParseFailure() throws Exception {
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.option-code", 0, 0L, "key", invalidJson);
        when(objectMapper.readValue(invalidJson, MdmOptionCodeEvent.class))
                .thenThrow(new RuntimeException("Parse error"));

        kafkaConsumer.onOptionCodeEvent(record);

        verify(mdmSyncAppService, never()).handleOptionCodeEvent(any());
        verify(mdmSyncMetrics).recordFailure();
        verify(mdmConsumerMetrics).recordConsume(MdmProjectionType.OPTION_CODE, "parse_error");
    }

    @Test
    @DisplayName("业务处理失败：记录 failure 指标")
    void onOptionCodeEvent_shouldHandleBusinessFailure() throws Exception {
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-option-code-001\",\"version\":1,\"code\":\"OPT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.option-code", 0, 0L, "key", messageJson);
        MdmOptionCodeEvent testEvent = event("OPT001", 1L);
        when(objectMapper.readValue(messageJson, MdmOptionCodeEvent.class)).thenReturn(testEvent);
        doThrow(new RuntimeException("Handle error")).when(mdmSyncAppService).handleOptionCodeEvent(testEvent);

        kafkaConsumer.onOptionCodeEvent(record);

        verify(mdmSyncMetrics).recordFailure();
        verify(mdmConsumerMetrics).recordConsume(MdmProjectionType.OPTION_CODE, "failure");
    }

    @Test
    @DisplayName("记录处理耗时")
    void onOptionCodeEvent_shouldRecordDuration() throws Exception {
        String messageJson = "{\"eventType\":\"CREATED\",\"entityId\":\"mdm-option-code-001\",\"version\":1,\"code\":\"OPT001\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("mdm.option-code", 0, 0L, "key", messageJson);
        MdmOptionCodeEvent testEvent = event("OPT001", 1L);
        when(objectMapper.readValue(messageJson, MdmOptionCodeEvent.class)).thenReturn(testEvent);

        kafkaConsumer.onOptionCodeEvent(record);

        verify(mdmSyncMetrics).recordDuration(anyLong());
    }
}
