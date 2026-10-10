package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayAggregateType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 绑定变更事件重放动作
 * <p>
 * VMD-DSN-CR-057: 仅对原批次候选范围内、当前仍 active 的绑定生成重放事件；
 * 直接构造既有 VehiclePartBindingChangedEvent Kafka 契约 payload（与
 * VehiclePartBindingKafkaProducer 序列化格式一致）写 vmd_outbox，不发布进程内 Spring 事件。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BindingEventReplayAction implements VehicleImportReplayAction {

    private static final String EVENT_TYPE = "VehiclePartBindingChangedEvent";
    private static final String SOURCE_TYPE = "IMPORT_EVENT_REPLAY";

    private final PartInfoRepository partInfoRepository;
    private final VehiclePartRepository vehiclePartRepository;
    private final MdmVehicleNodeRepository vehicleNodeRepository;
    private final VmdKafkaTopicRoutes topicRoutes;

    @Override
    public String actionType() {
        return VehicleImportReplayActionType.BINDING_EVENT_REPLAY.getValue();
    }

    @Override
    public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
        List<VehicleImportReplayActionTarget> targets = new ArrayList<>();
        if (context.getCandidates() == null) {
            return targets;
        }
        for (VehImportReplayExtractor.PartCandidate candidate : context.getCandidates()) {
            VehiclePart binding = findActiveBindingInScope(candidate, context.getVin());
            if (binding == null) {
                continue;
            }
            targets.add(new VehicleImportReplayActionTarget(
                    VehicleImportReplayAggregateType.VEHICLE_PART.getValue(),
                    String.valueOf(binding.getId()),
                    generateSeq(binding),
                    candidate.partCode() + ":" + candidate.sn()));
        }
        return targets;
    }

    @Override
    public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                   VehicleImportReplayActionTarget target) {
        VehiclePart binding = vehiclePartRepository.selectById(Long.parseLong(target.aggregateId()));
        if (binding == null || !Integer.valueOf(1).equals(binding.getBindState())) {
            log.warn("绑定事件重放跳过: 绑定[id={}]当前非active", target.aggregateId());
            return VehicleImportReplayActionResult.skipped("BINDING_NOT_ACTIVE: 当前绑定已失效，不发布历史脏快照");
        }
        if (!context.getVin().equals(binding.getVin())) {
            log.warn("绑定事件重放跳过: 绑定[id={}]已不属于本候选车辆[{}]", target.aggregateId(), context.getVin());
            return VehicleImportReplayActionResult.skipped("BINDING_VIN_CHANGED: 绑定归属已变化，不发布历史脏快照");
        }

        PartInfo partInfo = binding.getPartId() != null ? partInfoRepository.selectById(binding.getPartId()) : null;
        String partCode = partInfo != null ? partInfo.getPartCode() : null;
        String sn = partInfo != null ? partInfo.getSn() : null;

        VehicleNode vehicleNode = StrUtil.isNotBlank(binding.getVehicleNodeCode())
                ? vehicleNodeRepository.selectByCode(binding.getVehicleNodeCode()) : null;
        String deviceCategory = vehicleNode != null ? vehicleNode.getDeviceCategory() : null;

        // 提取 iccid1/iccid2（仅 deviceCategory=TBOX 时从 part_info.extra 取值）
        String iccid1 = null;
        String iccid2 = null;
        if ("TBOX".equals(deviceCategory) && partInfo != null && StrUtil.isNotBlank(partInfo.getExtra())) {
            try {
                JSONObject extraJson = JSONUtil.parseObj(partInfo.getExtra());
                iccid1 = extraJson.getStr("iccid1");
                iccid2 = extraJson.getStr("iccid2");
            } catch (Exception e) {
                log.warn("解析零件[{}]extra字段失败: {}", partCode, e.getMessage());
            }
        }

        Instant occurredAt = binding.getBindTime();
        Long seq = target.aggregateVersion();

        // 构造与 VehiclePartBindingKafkaProducer 序列化一致的 payload（排除 BaseEvent 无关字段）
        JSONObject payload = JSONUtil.createObj()
                .set("vin", binding.getVin())
                .set("bindingId", binding.getId())
                .set("partCode", partCode)
                .set("sn", sn)
                .set("deviceCategory", deviceCategory)
                .set("vehicleNodeCode", binding.getVehicleNodeCode())
                .set("iccid1", iccid1)
                .set("iccid2", iccid2)
                .set("changeType", BindingChangeType.BIND.getValue())
                .set("replaceOfBindingId", binding.getReplaceOfBindingId())
                .set("occurredAt", occurredAt != null ? occurredAt.toString() : null)
                .set("seq", seq)
                // 补发元数据（对下游 JSON 消费向后兼容）
                .set("replay", true)
                .set("batchNum", context.getBatchNum())
                .set("replayId", context.getReplayId())
                .set("replayOperator", context.getOperatorId())
                .set("replayedAt", LocalDateTime.now().toString());

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .aggregateType(VehicleImportReplayAggregateType.VEHICLE_PART.getValue())
                .aggregateId(String.valueOf(binding.getId()))
                .aggregateVersion(seq)
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_BINDING_CHANGED))
                .messageKey(binding.getVin())
                .payload(payload.toString())
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(context.getReplayId())
                .createTime(LocalDateTime.now())
                .build();

        return VehicleImportReplayActionResult.queued(outbox.getEventId(), outbox);
    }

    /**
     * 在候选范围内查找当前仍 active 的绑定（未命中返回 null）
     */
    private VehiclePart findActiveBindingInScope(VehImportReplayExtractor.PartCandidate candidate, String vin) {
        PartInfo partInfo = partInfoRepository.selectByPartCodeAndSn(candidate.partCode(), candidate.sn());
        if (partInfo == null) {
            return null;
        }
        VehiclePart binding = vehiclePartRepository.selectActiveByPartId(partInfo.getId());
        if (binding == null || !vin.equals(binding.getVin())) {
            return null;
        }
        return binding;
    }

    /**
     * 生成事件序（复用 vehicle_part 主键 id + bind_time 组合表达，与既有发布器一致）
     */
    private Long generateSeq(VehiclePart vehiclePart) {
        long base = vehiclePart.getId() != null ? vehiclePart.getId() : 0L;
        Instant time = vehiclePart.getBindTime() != null ? vehiclePart.getBindTime() : Instant.now();
        long timePart = time.toEpochMilli() % 10000;
        return base * 10000 + timePart;
    }
}
