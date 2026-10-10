package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayAggregateType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 软件实装事件重放动作
 * <p>
 * VMD-DSN-CR-057: 仅对候选范围内、当前 ACTIVE 的软件实装记录生成重放事件；
 * 直接构造既有 VehicleSoftwareInventoryChangedEvent Kafka 契约 payload
 * （与 SoftwareInventoryAppService 发布格式一致）写 vmd_outbox。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SoftwareInventoryEventReplayAction implements VehicleImportReplayAction {

    private static final String EVENT_TYPE = "VehicleSoftwareInventoryChangedEvent";
    private static final String SOURCE_TYPE = "IMPORT_EVENT_REPLAY";
    private static final String INSTALL_STATE_ACTIVE = "ACTIVE";

    private final PartInfoRepository partInfoRepository;
    private final PartSoftwareInstallationRepository partSoftwareInstallationRepository;
    private final VehiclePartRepository vehiclePartRepository;
    private final VmdKafkaTopicRoutes topicRoutes;

    @Override
    public String actionType() {
        return VehicleImportReplayActionType.SOFTWARE_INVENTORY_EVENT_REPLAY.getValue();
    }

    @Override
    public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
        List<VehicleImportReplayActionTarget> targets = new ArrayList<>();
        if (context.getCandidates() == null) {
            return targets;
        }
        for (VehImportReplayExtractor.PartCandidate candidate : context.getCandidates()) {
            PartInfo partInfo = partInfoRepository.selectByPartCodeAndSn(candidate.partCode(), candidate.sn());
            if (partInfo == null) {
                continue;
            }
            for (PartSoftwareInstallation record : activeRecordsInScope(partInfo, context.getVin())) {
                targets.add(new VehicleImportReplayActionTarget(
                        VehicleImportReplayAggregateType.PART_SOFTWARE_INSTALLATION.getValue(),
                        String.valueOf(partInfo.getId()),
                        record.getInventoryVersion(),
                        candidate.partCode() + ":" + candidate.sn()));
            }
        }
        return targets;
    }

    @Override
    public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                   VehicleImportReplayActionTarget target) {
        PartInfo partInfo = partInfoRepository.selectById(Long.parseLong(target.aggregateId()));
        if (partInfo == null) {
            return VehicleImportReplayActionResult.skipped("PART_NOT_FOUND: 候选零件实例当前不存在");
        }
        PartSoftwareInstallation record = findRecord(partInfo.getId(), target.aggregateVersion());
        if (record == null) {
            return VehicleImportReplayActionResult.skipped("SOFTWARE_RECORD_NOT_ACTIVE: 当前无该版本ACTIVE实装记录");
        }

        // 当前绑定归属校验：仅重放仍属于本候选车辆的实装事实
        VehiclePart binding = record.getBindingId() != null
                ? vehiclePartRepository.selectById(record.getBindingId()) : null;
        if (binding == null || !Integer.valueOf(1).equals(binding.getBindState())
                || !context.getVin().equals(binding.getVin())) {
            log.warn("软件实装事件重放跳过: 实装记录[id={}]当前绑定归属已变化，不发布历史脏快照", target.aggregateId());
            return VehicleImportReplayActionResult.skipped("BINDING_CHANGED: 当前绑定事实已失效，不发布历史脏快照");
        }

        String vehicleNodeCode = binding.getVehicleNodeCode();

        // 构造与 SoftwareInventoryAppService 发布格式一致的 payload（排除 BaseEvent 无关字段）
        JSONObject payload = JSONUtil.createObj()
                .set("vin", context.getVin())
                .set("bindingId", binding.getId())
                .set("partId", partInfo.getId())
                .set("partCode", partInfo.getPartCode())
                .set("sn", partInfo.getSn())
                .set("vehicleNodeCode", vehicleNodeCode)
                .set("softwareTargetCode", record.getSoftwareTargetCode())
                .set("softwarePartNo", record.getSoftwarePartNo())
                .set("softwareVersion", record.getSoftwareVersion())
                .set("slot", record.getSlot())
                .set("active", record.getIsActiveSlot())
                .set("digest", record.getArtifactHash())
                .set("changeType", record.getChangeType())
                .set("source", record.getSource())
                .set("isConfirmed", record.getIsConfirmed())
                .set("inventoryVersion", record.getInventoryVersion())
                .set("occurredAt", Instant.now().toString())
                // 补发元数据（对下游 JSON 消费向后兼容）
                .set("replay", true)
                .set("batchNum", context.getBatchNum())
                .set("replayId", context.getReplayId())
                .set("replayOperator", context.getOperatorId())
                .set("replayedAt", LocalDateTime.now().toString());

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .aggregateType(VehicleImportReplayAggregateType.PART_SOFTWARE_INSTALLATION.getValue())
                .aggregateId(String.valueOf(partInfo.getId()))
                .aggregateVersion(record.getInventoryVersion())
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED))
                .messageKey(context.getVin())
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
     * 候选零件当前 ACTIVE 且属于该 VIN 的实装记录
     */
    private List<PartSoftwareInstallation> activeRecordsInScope(PartInfo partInfo, String vin) {
        VehiclePart binding = vehiclePartRepository.selectActiveByPartId(partInfo.getId());
        if (binding == null || !vin.equals(binding.getVin())) {
            return List.of();
        }
        List<PartSoftwareInstallation> result = new ArrayList<>();
        for (PartSoftwareInstallation record : partSoftwareInstallationRepository.selectByPartId(partInfo.getId())) {
            if (INSTALL_STATE_ACTIVE.equals(record.getInstallState())
                    && Boolean.TRUE.equals(record.getIsActiveSlot())) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * 按零件与清单版本定位实装记录
     */
    private PartSoftwareInstallation findRecord(Long partId, Long inventoryVersion) {
        for (PartSoftwareInstallation record : partSoftwareInstallationRepository.selectByPartId(partId)) {
            if (inventoryVersion.equals(record.getInventoryVersion())
                    && INSTALL_STATE_ACTIVE.equals(record.getInstallState())
                    && Boolean.TRUE.equals(record.getIsActiveSlot())) {
                return record;
            }
        }
        return null;
    }
}
