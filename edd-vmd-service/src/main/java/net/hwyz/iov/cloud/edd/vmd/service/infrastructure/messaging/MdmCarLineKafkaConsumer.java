package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM CarLine事件Kafka消费者
 * <p>
 * 监听 EDD-MDM 标准 Topic（VMD-DSN-CR-052：topic 统一由
 * {@code vmd.kafka.topics.mdm.car-line} 提供，归 EDD-MDM 管理，VMD 只消费不创建），
 * 转换为本地MdmCarLineEvent并调用 MdmSyncAppService.handleSeriesEvent() 进行幂等upsert。
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
    private final MdmConsumerMetrics mdmConsumerMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM CarLine事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            id = MdmProjectionType.ConsumerIds.CAR_LINE,
            topics = {"${vmd.kafka.topics.mdm.car-line:mdm.car-line}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            autoStartup = "${vmd.kafka.mdm-consumer.auto-startup:true}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCarLineEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM CarLine事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        MdmCarLineEvent event;
        try {
            event = parseEvent(record.value());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CAR_LINE, "parse_error");
            log.error("MDM CarLine事件解析失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            return;
        }

        try {
            mdmSyncAppService.handleSeriesEvent(event);
            mdmSyncMetrics.recordSuccess();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CAR_LINE, "success");
            log.info("MDM CarLine事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CAR_LINE, "failure");
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
