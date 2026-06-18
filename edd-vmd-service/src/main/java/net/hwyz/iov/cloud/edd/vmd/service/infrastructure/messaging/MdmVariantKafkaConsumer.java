package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Variant事件Kafka消费者
 * <p>
 * 监听MDM Variant子域推送的Kafka事件，转换为本地MdmVariantEvent并调用
 * MdmSyncAppService.handleVariantEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleVariantEvent(MdmVariantEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.variant.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmVariantKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Variant事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.variant.kafka.created-topic:mdm.product.variant.created}",
                      "${mdm.sync.variant.kafka.updated-topic:mdm.product.variant.updated}",
                      "${mdm.sync.variant.kafka.deactivated-topic:mdm.product.variant.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVariantEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Variant事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmVariantEvent event = parseEvent(record.value());
            mdmSyncAppService.handleVariantEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Variant事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM Variant事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmVariantEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmVariantEvent
     * @throws Exception 解析异常
     */
    private MdmVariantEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmVariantEvent.class);
    }
}
