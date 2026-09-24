package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MDM 车载节点事件Kafka消费者
 * <p>
 * 监听 EDD-MDM 标准 Topic（VMD-DSN-CR-052：topic 统一由
 * {@code vmd.kafka.topics.mdm.vehicle-node} 提供，归 EDD-MDM 管理，VMD 只消费不创建），
 * 转换为本地MdmVehicleNodeEvent并调用 MdmSyncAppService.handleVehicleNodeEvent() 进行幂等upsert。
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
    private final MdmConsumerMetrics mdmConsumerMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费MDM 车载节点事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            id = MdmProjectionType.ConsumerIds.VEHICLE_NODE,
            topics = {"${vmd.kafka.topics.mdm.vehicle-node:mdm.vehicle-node}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            autoStartup = "${vmd.kafka.mdm-consumer.auto-startup:true}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVehicleNodeEvent(ConsumerRecord<String, String> record) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("收到MDM车载节点事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        MdmVehicleNodeEvent event;
        try {
            event = parseEvent(record.value());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.VEHICLE_NODE, "parse_error");
            log.error("MDM车载节点事件解析失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            // 重新抛出交由框架 ErrorHandler 处理（重试/退避/DLQ），
            // 避免吞异常导致失败消息 offset 被静默提交而永久丢失
            throw e;
        }

        try {
            mdmSyncAppService.handleVehicleNodeEvent(event);
            mdmSyncMetrics.recordSuccess();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.VEHICLE_NODE, "success");
            log.info("MDM车载节点事件处理成功: entityId={}, eventType={}",
                    event.getEntityId(), event.getEventType());
        } catch (Exception e) {
            mdmSyncMetrics.recordFailure();
            mdmConsumerMetrics.recordConsume(MdmProjectionType.VEHICLE_NODE, "failure");
            log.error("MDM车载节点事件处理失败: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            // 重新抛出交由框架 ErrorHandler 处理（重试/退避/DLQ）
            throw e;
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
