package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM 车载节点事件Kafka消费者
 * <p>
 * 监听MDM EEAD子域推送的Kafka事件，转换为本地MdmVehicleNodeEvent并调用
 * MdmSyncAppService.handleVehicleNodeEvent()进行幂等upsert。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#handleVehicleNodeEvent(MdmVehicleNodeEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.vehicle-node.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MdmVehicleNodeKafkaConsumer {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM 车载节点事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = "${mdm.sync.vehicle-node.kafka.topic:mdm.eead.vehicleNode.event}",
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVehicleNodeEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM车载节点事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            MdmVehicleNodeEvent event = parseEvent(record.value());
            mdmSyncAppService.handleVehicleNodeEvent(event);
            mdmSyncMetrics.recordSuccess();
            log.info("MDM车载节点事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            log.error("MDM车载节点事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
        }
    }

    /**
     * 解析Kafka消息为MdmVehicleNodeEvent
     *
     * @param messageJson 消息JSON字符串
     * @return MdmVehicleNodeEvent
     * @throws Exception 解析异常
     */
    private MdmVehicleNodeEvent parseEvent(String messageJson) throws Exception {
        return objectMapper.readValue(messageJson, MdmVehicleNodeEvent.class);
    }
}
