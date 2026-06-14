package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPartEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Part事件Kafka消费者
 * <p>
 * 监听MDM Part子域推送的Kafka事件，转换为本地MdmPartEvent并调用
 * MdmSyncAppService.handlePartEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handlePartEvent(MdmPartEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.part.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmPartKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Part事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = "${mdm.sync.part.kafka.topic:mdm.material.part.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPartEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Part事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmPartEvent event = parseEvent(record.value());
            mdmSyncAppService.handlePartEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Part事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Part事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmPartEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmPartEvent
     * @throws Exception 解析异常
     */
    private MdmPartEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmPartEvent.class);
    }
}
