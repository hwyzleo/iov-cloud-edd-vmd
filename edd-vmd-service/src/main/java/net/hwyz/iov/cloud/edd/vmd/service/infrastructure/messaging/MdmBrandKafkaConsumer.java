package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Brand事件Kafka消费者
 * <p>
 * 监听MDM Brand子域推送的Kafka事件，转换为本地MdmBrandEvent并调用
 * MdmSyncAppService.handleBrandEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleBrandEvent(MdmBrandEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.brand.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmBrandKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Brand事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.brand.kafka.created-topic:mdm.product.brand.created}",
                      "${mdm.sync.brand.kafka.updated-topic:mdm.product.brand.updated}",
                      "${mdm.sync.brand.kafka.deactivated-topic:mdm.product.brand.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBrandEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Brand事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmBrandEvent event = parseEvent(record.value());
            mdmSyncAppService.handleBrandEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Brand事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Brand事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmBrandEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmBrandEvent
     * @throws Exception 解析异常
     */
    private MdmBrandEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmBrandEvent.class);
    }
}
