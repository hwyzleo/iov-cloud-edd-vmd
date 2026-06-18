package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Platform事件Kafka消费者
 * <p>
 * 监听MDM Platform子域推送的Kafka事件，转换为本地MdmPlatformEvent并调用
 * MdmSyncAppService.handlePlatformEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handlePlatformEvent(MdmPlatformEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.platform.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmPlatformKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Platform事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.platform.kafka.created-topic:mdm.product.platform.created}",
                      "${mdm.sync.platform.kafka.updated-topic:mdm.product.platform.updated}",
                      "${mdm.sync.platform.kafka.deactivated-topic:mdm.product.platform.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPlatformEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Platform事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmPlatformEvent event = parseEvent(record.value());
            mdmSyncAppService.handlePlatformEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Platform事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Platform事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmPlatformEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmPlatformEvent
     * @throws Exception 解析异常
     */
    private MdmPlatformEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmPlatformEvent.class);
    }
}
