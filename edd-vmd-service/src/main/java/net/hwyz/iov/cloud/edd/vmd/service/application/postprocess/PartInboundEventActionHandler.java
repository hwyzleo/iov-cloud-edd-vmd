package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.PartInboundEventEnvelope;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 零件入站跨域事件动作处理器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 从当前 part_info 构造零件实例当前快照，生成新 eventId，
 * 信封携带 replay=true、batchNum、replayId、操作人、重放时间和实例版本，
 * 写入通用 vmd_outbox 由 Relay 至少一次发布。不复用历史 payload。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartInboundEventActionHandler implements PartPostProcessActionHandler {

    private static final String EVENT_TYPE = "PartInboundEvent";
    private static final String AGGREGATE_TYPE = "PART_INSTANCE";
    private static final String SOURCE_TYPE = "PART_POST_PROCESS_REPLAY";

    private final VmdKafkaTopicRoutes topicRoutes;

    @Override
    public String actionType() {
        return PartPostProcessActionType.PART_INBOUND_EVENT.getValue();
    }

    @Override
    public boolean supports(PartPostProcessActionContext context) {
        return context.getPartInfo() != null;
    }

    @Override
    public String idempotencyKey(PartPostProcessActionContext context) {
        return String.join(":", context.getReplayId(), context.getPartCode(), context.getSn(), actionType());
    }

    @Override
    public PartPostProcessActionResult execute(PartPostProcessActionContext context) {
        PartInfo partInfo = context.getPartInfo();
        if (partInfo == null) {
            return PartPostProcessActionResult.failedFinal("PART_NOT_FOUND", "候选实例在当前已不存在，不得依据历史报文重建");
        }

        String aggregateId = context.getPartCode() + ":" + context.getSn();
        long instanceVersion = partInfo.getLastInboundTime() != null
                ? partInfo.getLastInboundTime().toEpochMilli() : System.currentTimeMillis();
        String eventId = UUID.randomUUID().toString();

        // 绑定快照（当前 active 绑定时填充）
        VehiclePart binding = context.getActiveBinding();
        VehicleNode vehicleNode = context.getVehicleNode();
        PartInboundEventEnvelope.PartInboundPayload payload = PartInboundEventEnvelope.PartInboundPayload.builder()
                .partCode(context.getPartCode())
                .sn(context.getSn())
                .vehicleNodeCode(partInfo.getVehicleNodeCode())
                .partType(partInfo.getPartType())
                .supplierCode(partInfo.getSupplierCode())
                .hardwarePn(partInfo.getHardwarePn())
                .hardwareVer(partInfo.getHardwareVer())
                .configWord(partInfo.getConfigWord())
                .extra(partInfo.getExtra())
                .instanceState(partInfo.getInstanceState())
                .batchNum(context.getBatchNum())
                .vin(binding != null ? binding.getVin() : null)
                .bindingId(binding != null ? binding.getId() : null)
                .deviceCategory(vehicleNode != null ? vehicleNode.getDeviceCategory() : null)
                .position(binding != null ? binding.getPosition() : null)
                .bindTime(binding != null && binding.getBindTime() != null ? binding.getBindTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();

        PartInboundEventEnvelope envelope = PartInboundEventEnvelope.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .version(instanceVersion)
                .occurredAt(LocalDateTime.now())
                .producer("vmd-replay")
                .payload(payload)
                .replay(true)
                .batchNum(context.getBatchNum())
                .replayId(context.getReplayId())
                .replayOperator(context.getOperatorId())
                .replayedAt(LocalDateTime.now())
                .build();

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .aggregateVersion(instanceVersion)
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_INBOUND_CHANGED))
                .messageKey(aggregateId)
                .payload(JSONUtil.toJsonStr(envelope))
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(context.getReplayId())
                .createTime(LocalDateTime.now())
                .build();

        log.debug("零件[{}:{}]入站跨域事件已构造, eventId={}", context.getPartCode(), context.getSn(), eventId);
        return PartPostProcessActionResult.queued(eventId, outbox);
    }
}
