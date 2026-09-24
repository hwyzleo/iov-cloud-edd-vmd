package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM Configuration事件Kafka消费者
 * <p>
 * 监听 EDD-MDM 标准 Topic（VMD-DSN-CR-052：topic 统一由
 * {@code vmd.kafka.topics.mdm.configuration} 提供，归 EDD-MDM 管理，VMD 只消费不创建），
 * 转换为本地MdmConfigurationEvent并调用 MdmSyncAppService.handleConfigurationEvent() 进行幂等upsert。
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
    private final ConfigurationSyncMetrics configurationSyncMetrics;
    private final MdmConsumerMetrics mdmConsumerMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM Configuration事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            id = MdmProjectionType.ConsumerIds.CONFIGURATION,
            topics = {"${vmd.kafka.topics.mdm.configuration:mdm.configuration}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            autoStartup = "${vmd.kafka.mdm-consumer.auto-startup:true}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onConfigurationEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM Configuration事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        MdmConfigurationEvent event;
        try {
            event = parseEvent(record.value());
        } catch (Exception e) {
            configurationSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CONFIGURATION, "parse_error");
            log.error("MDM Configuration事件解析失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            return;
        }

        try {
            mdmSyncAppService.handleConfigurationEvent(event);
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CONFIGURATION, "success");
            log.info("MDM Configuration事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            // 契约错误（缺 variantCode 等）与处理失败统一计入失败指标，进入现有重试/DLQ（CR-047 §4.3）
            configurationSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.CONFIGURATION, "failure");
            log.error("MDM Configuration事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
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
