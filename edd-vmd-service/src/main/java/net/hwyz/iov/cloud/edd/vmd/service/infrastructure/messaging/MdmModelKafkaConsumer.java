package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Model事件Kafka消费者
 * <p>
 * 监听MDM Model子域推送的Kafka事件，转换为本地MdmModelEvent并调用
 * MdmSyncAppService.handleModelEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleModelEvent(MdmModelEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.model.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmModelKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Model事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.model.kafka.created-topic:mdm.product.model.created}",
                      "${mdm.sync.model.kafka.updated-topic:mdm.product.model.updated}",
                      "${mdm.sync.model.kafka.deactivated-topic:mdm.product.model.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onModelEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Model事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmModelEvent event = parseEvent(record.value());
            mdmSyncAppService.handleModelEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Model事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Model事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmModelEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmModelEvent
     * @throws Exception 解析异常
     */
    private MdmModelEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmModelEvent.class);
    }
}
