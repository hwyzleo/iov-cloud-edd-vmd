package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM OptionCode事件Kafka消费者（VMD-DSN-CR-052 新增第 11 个 MDM 消费 Listener）
 * <p>
 * 监听 EDD-MDM 标准 Topic（topic 统一由 {@code vmd.kafka.topics.mdm.option-code} 提供，
 * 归 EDD-MDM 管理，VMD 只消费不创建），转换为本地MdmOptionCodeEvent并调用
 * MdmSyncAppService.handleOptionCodeEvent() 进行幂等upsert。
 * </p>
 *
 * @author hwyz_leo
 * @see MdmSyncAppService#handleOptionCodeEvent(MdmOptionCodeEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.option-code.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmOptionCodeKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final MdmConsumerMetrics mdmConsumerMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM OptionCode事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            id = MdmProjectionType.ConsumerIds.OPTION_CODE,
            topics = {"${vmd.kafka.topics.mdm.option-code:mdm.option-code}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            autoStartup = "${vmd.kafka.mdm-consumer.auto-startup:true}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOptionCodeEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM OptionCode事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        MdmOptionCodeEvent event;
        try {
            event = parseEvent(record.value());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_CODE, "parse_error");
            log.error("MDM OptionCode事件解析失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            return;
        }

        try {
            mdmSyncAppService.handleOptionCodeEvent(event);
            mdmSyncMetrics.recordSuccess();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_CODE, "success");
            log.info("MDM OptionCode事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_CODE, "failure");
            log.error("MDM OptionCode事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmOptionCodeEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmOptionCodeEvent
     * @throws Exception 解析异常
     */
    private MdmOptionCodeEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmOptionCodeEvent.class);
    }
}
