package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 绑定事实事件动作处理器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 查询当前唯一 active vehicle_part；存在时复用 VehiclePartBindingChangedEvent 当前快照契约
 * 写入 Outbox（新 eventId、replay=true、当前 bindingId/seq/version），不存在时 SKIPPED(NO_ACTIVE_BINDING)。
 * 重放只重新陈述当前绑定事实，不创建或切换绑定；下游以绑定业务键与版本收敛。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BindingFactEventActionHandler implements PartPostProcessActionHandler {

    private static final String EVENT_TYPE = "VehiclePartBindingChangedEvent";
    private static final String AGGREGATE_TYPE = "VEHICLE_PART";
    private static final String SOURCE_TYPE = "PART_POST_PROCESS_REPLAY";

    private final VmdKafkaTopicRoutes topicRoutes;

    @Override
    public String actionType() {
        return PartPostProcessActionType.BINDING_FACT_EVENT.getValue();
    }

    @Override
    public boolean supports(PartPostProcessActionContext context) {
        return context.getPartInfo() != null && context.getActiveBinding() != null;
    }

    @Override
    public String idempotencyKey(PartPostProcessActionContext context) {
        return String.join(":", context.getReplayId(), context.getPartCode(), context.getSn(), actionType());
    }

    @Override
    public PartPostProcessActionResult execute(PartPostProcessActionContext context) {
        PartInfo partInfo = context.getPartInfo();
        VehiclePart binding = context.getActiveBinding();
        if (partInfo == null) {
            return PartPostProcessActionResult.failedFinal("PART_NOT_FOUND", "候选实例在当前已不存在，不得依据历史报文重建");
        }
        if (binding == null) {
            return PartPostProcessActionResult.skipped("NO_ACTIVE_BINDING");
        }

        VehicleNode vehicleNode = context.getVehicleNode();
        String deviceCategory = vehicleNode != null ? vehicleNode.getDeviceCategory() : null;

        // 提取 iccid1/iccid2（仅 deviceCategory=TBOX 时从 part_info.extra 取值）
        String iccid1 = null;
        String iccid2 = null;
        if ("TBOX".equals(deviceCategory) && StrUtil.isNotBlank(partInfo.getExtra())) {
            try {
                JSONObject extraJson = JSONUtil.parseObj(partInfo.getExtra());
                iccid1 = extraJson.getStr("iccid1");
                iccid2 = extraJson.getStr("iccid2");
            } catch (Exception e) {
                log.warn("解析零件[{}]extra字段失败: {}", partInfo.getPartCode(), e.getMessage());
            }
        }

        Instant occurredAt = binding.getBindTime() != null ? binding.getBindTime() : Instant.now();
        Long seq = generateSeq(binding);
        String eventId = UUID.randomUUID().toString();
        String vin = binding.getVin();

        // 事件信封：对齐 VehiclePartBindingKafkaProducer.serializeEvent 契约 + 重放扩展字段
        JSONObject node = new JSONObject();
        node.set("eventId", eventId);
        node.set("replay", true);
        node.set("replayId", context.getReplayId());
        node.set("batchNum", context.getBatchNum());
        node.set("replayOperator", context.getOperatorId());
        node.set("replayedAt", LocalDateTime.now().toString());
        node.set("vin", vin);
        node.set("bindingId", binding.getId());
        node.set("partCode", context.getPartCode());
        node.set("sn", context.getSn());
        node.set("deviceCategory", deviceCategory);
        node.set("vehicleNodeCode", binding.getVehicleNodeCode());
        node.set("iccid1", iccid1);
        node.set("iccid2", iccid2);
        node.set("changeType", BindingChangeType.BIND.getValue());
        node.set("replaceOfBindingId", binding.getReplaceOfBindingId());
        node.set("occurredAt", occurredAt.toString());
        node.set("seq", seq);

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(binding.getId()))
                .aggregateVersion(seq)
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_BINDING_CHANGED))
                .messageKey(vin)
                .payload(node.toString())
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(context.getReplayId())
                .createTime(LocalDateTime.now())
                .build();

        log.debug("零件[{}:{}]绑定事实事件已构造, eventId={}, bindingId={}, seq={}",
                context.getPartCode(), context.getSn(), eventId, binding.getId(), seq);
        return PartPostProcessActionResult.queued(eventId, outbox);
    }

    /**
     * 生成事件序（复用 vehicle_part 主键 id + bind_time 组合表达，对齐 VehiclePartBindingPublisher）
     */
    private Long generateSeq(VehiclePart vehiclePart) {
        long base = vehiclePart.getId() != null ? vehiclePart.getId() : 0L;
        Instant time = vehiclePart.getBindTime() != null ? vehiclePart.getBindTime() : Instant.now();
        long timePart = time.toEpochMilli() % 10000;
        return base * 10000 + timePart;
    }
}
