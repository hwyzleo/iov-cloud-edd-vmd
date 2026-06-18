package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Plant事件Kafka消费者
 * <p>
 * 监听MDM Plant子域推送的Kafka事件，转换为本地MdmPlantEvent并调用
 * MdmSyncAppService.handlePlantEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handlePlantEvent(MdmPlantEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.plant.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmPlantKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Plant事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = "${mdm.sync.plant.kafka.topic:mdm.org.plant.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPlantEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Plant事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmPlantEvent event = parseEvent(record.value());
            mdmSyncAppService.handlePlantEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Plant事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Plant事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmPlantEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmPlantEvent
     * @throws Exception 解析异常
     */
    private MdmPlantEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmPlantEvent.class);
    }
}
