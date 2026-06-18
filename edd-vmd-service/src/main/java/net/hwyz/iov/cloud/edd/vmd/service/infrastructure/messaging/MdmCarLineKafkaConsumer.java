package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM CarLine事件Kafka消费者
 * <p>
 * 监听MDM CarLine子域推送的Kafka事件，转换为本地MdmCarLineEvent并调用
 * MdmSyncAppService.handleSeriesEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleSeriesEvent(MdmCarLineEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.car-line.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmCarLineKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM CarLine事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${mdm.sync.car-line.kafka.created-topic:mdm.product.carLine.created}",
                      "${mdm.sync.car-line.kafka.updated-topic:mdm.product.carLine.updated}",
                      "${mdm.sync.car-line.kafka.deactivated-topic:mdm.product.carLine.deactivated}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCarLineEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM CarLine事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmCarLineEvent event = parseEvent(record.value());
            mdmSyncAppService.handleSeriesEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM CarLine事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM CarLine事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmCarLineEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmCarLineEvent
     * @throws Exception 解析异常
     */
    private MdmCarLineEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmCarLineEvent.class);
    }
}
