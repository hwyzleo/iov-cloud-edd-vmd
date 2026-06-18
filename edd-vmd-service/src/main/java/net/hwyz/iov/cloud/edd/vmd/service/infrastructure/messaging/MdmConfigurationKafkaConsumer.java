package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Configuration事件Kafka消费者
 * <p>
 * 监听MDM Configuration子域推送的Kafka事件，转换为本地MdmConfigurationEvent并调用
 * MdmSyncAppService.handleConfigurationEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleConfigurationEvent(MdmConfigurationEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.configuration.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmConfigurationKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Configuration事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.configuration.kafka.created-topic:mdm.product.configuration.created}",
                      "${mdm.sync.configuration.kafka.updated-topic:mdm.product.configuration.updated}",
                      "${mdm.sync.configuration.kafka.deactivated-topic:mdm.product.configuration.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onConfigurationEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Configuration事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmConfigurationEvent event = parseEvent(record.value());
            mdmSyncAppService.handleConfigurationEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Configuration事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Configuration事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmConfigurationEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmConfigurationEvent
     * @throws Exception 解析异常
     */
    private MdmConfigurationEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmConfigurationEvent.class);
    }
}
