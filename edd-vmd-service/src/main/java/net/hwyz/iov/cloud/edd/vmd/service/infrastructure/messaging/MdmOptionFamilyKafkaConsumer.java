package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM OptionFamily事件Kafka消费者
 * <p>
 * 监听 EDD-MDM 标准 Topic（VMD-DSN-CR-052：topic 统一由
 * {@code vmd.kafka.topics.mdm.option-family} 提供，归 EDD-MDM 管理，VMD 只消费不创建），
 * 转换为本地MdmOptionFamilyEvent并调用 MdmSyncAppService.handleOptionFamilyEvent() 进行幂等upsert。
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
    private final MdmConsumerMetrics mdmConsumerMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM OptionFamily事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            id = MdmProjectionType.ConsumerIds.OPTION_FAMILY,
            topics = {"${vmd.kafka.topics.mdm.option-family:mdm.option-family}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            autoStartup = "${vmd.kafka.mdm-consumer.auto-startup:true}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOptionFamilyEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM OptionFamily事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        MdmOptionFamilyEvent event;
        try {
            event = parseEvent(record.value());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_FAMILY, "parse_error");
            log.error("MDM OptionFamily事件解析失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            return;
        }

        try {
            mdmSyncAppService.handleOptionFamilyEvent(event);
            mdmSyncMetrics.recordSuccess();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_FAMILY, "success");
            log.info("MDM OptionFamily事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.OPTION_FAMILY, "failure");
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
