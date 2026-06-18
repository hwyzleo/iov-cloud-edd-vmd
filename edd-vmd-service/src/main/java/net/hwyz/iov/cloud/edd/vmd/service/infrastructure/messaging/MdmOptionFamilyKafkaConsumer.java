package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM OptionFamily事件Kafka消费者
 * <p>
 * 监听MDM OptionFamily子域推送的Kafka事件，转换为本地MdmOptionFamilyEvent并调用
 * MdmSyncAppService.handleOptionFamilyEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleOptionFamilyEvent(MdmOptionFamilyEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.option-family.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmOptionFamilyKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM OptionFamily事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.option-family.kafka.created-topic:mdm.product.optionFamily.created}",
                      "${mdm.sync.option-family.kafka.updated-topic:mdm.product.optionFamily.updated}",
                      "${mdm.sync.option-family.kafka.deactivated-topic:mdm.product.optionFamily.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOptionFamilyEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM OptionFamily事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmOptionFamilyEvent event = parseEvent(record.value());
            mdmSyncAppService.handleOptionFamilyEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM OptionFamily事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM OptionFamily事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmOptionFamilyEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmOptionFamilyEvent
     * @throws Exception 解析异常
     */
    private MdmOptionFamilyEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmOptionFamilyEvent.class);
    }
}
