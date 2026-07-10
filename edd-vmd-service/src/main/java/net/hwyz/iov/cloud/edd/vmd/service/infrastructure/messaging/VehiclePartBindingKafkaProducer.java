package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehiclePartBindingChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 车辆-零件绑定变更事件 Kafka 生产者
 * <p>
 * 监听 Spring {@link VehiclePartBindingChangedEvent}（由 {@code VehiclePartBindingPublisher} 发布），
 * 序列化为 JSON 后发送到 Kafka topic {@code vmd-vehicle-binding-changed}，
 * 供下游（TSP 等）消费建立只读投影。
 * <p>
 * 消息 key 为 {@code vin}，保证同一车辆的绑定变更按顺序消费。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.binding.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class VehiclePartBindingKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vmd.binding.kafka.topic:vmd-vehicle-binding-changed}")
    private String topic;

    /**
     * 监听绑定变更事件并发送到 Kafka
     *
     * @param event 车辆-零件绑定变更事件
     */
    @EventListener
    public void onVehiclePartBindingChanged(VehiclePartBindingChangedEvent event) {
        String json;
        try {
            json = serializeEvent(event);
        } catch (JsonProcessingException e) {
            log.error("车辆[{}]绑定变更事件序列化失败: {}", event.getVin(), e.getMessage(), e);
            return;
        }

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, event.getVin(), json);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("车辆[{}]绑定变更事件发送Kafka失败: topic={}, changeType={}, error={}",
                        event.getVin(), topic, event.getChangeType().getValue(), ex.getMessage(), ex);
            } else {
                log.info("车辆[{}]绑定变更事件发送Kafka成功: topic={}, partition={}, offset={}, changeType={}",
                        event.getVin(), topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getChangeType().getValue());
            }
        });
    }

    /**
     * 将事件序列化为 Kafka 消息 JSON
     * <p>
     * 手动构建 ObjectNode 以排除 BaseEvent 及 ApplicationEvent 的无关字段（source / timestamp / id）。
     * 字段格式与下游 TSP 消费端约定一致：
     * <ul>
     *   <li>occurredAt -> ISO-8601 字符串</li>
     *   <li>changeType -> 枚举 value 字符串（BIND / UNBIND / REPLACE）</li>
     * </ul>
     *
     * @param event 绑定变更事件
     * @return JSON 字符串
     * @throws JsonProcessingException 序列化异常
     */
    private String serializeEvent(VehiclePartBindingChangedEvent event) throws JsonProcessingException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("vin", event.getVin());
        node.put("bindingId", event.getBindingId());
        node.put("partCode", event.getPartCode());
        node.put("sn", event.getSn());
        node.put("deviceCategory", event.getDeviceCategory());
        node.put("vehicleNodeCode", event.getVehicleNodeCode());
        node.put("iccid1", event.getIccid1());
        node.put("iccid2", event.getIccid2());
        node.put("changeType", event.getChangeType().getValue());
        node.put("replaceOfBindingId", event.getReplaceOfBindingId());
        node.put("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);
        node.put("seq", event.getSeq());
        return objectMapper.writeValueAsString(node);
    }

}
