package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEventEnvelope;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayAggregateType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 生产事件补发动作
 * <p>
 * VMD-DSN-CR-057: 封装 CR-039 既有逻辑（PRODUCE_EVENT），不改契约。
 * 读取当前车辆完整快照，构造 VehicleProduceEventEnvelope 写 vmd_outbox，
 * 由 OutboxRelay 发布至 vmd.vehicle.produce.event（Key=VIN）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProduceEventReplayAction implements VehicleImportReplayAction {

    private static final String EVENT_TYPE = "VehicleProduceEvent";
    private static final String SOURCE_TYPE = "IMPORT_EVENT_REPLAY";

    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VmdKafkaTopicRoutes topicRoutes;

    @Override
    public String actionType() {
        return VehicleImportReplayActionType.PRODUCE_EVENT.getValue();
    }

    @Override
    public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
        // 聚合版本取执行时刻时间戳（当前快照版本）
        return List.of(new VehicleImportReplayActionTarget(
                VehicleImportReplayAggregateType.VEHICLE.getValue(),
                context.getVin(),
                System.currentTimeMillis(),
                null));
    }

    @Override
    public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                   VehicleImportReplayActionTarget target) {
        String vin = context.getVin();

        // 读取当前车辆完整快照（payload 从当前事实构造，不是历史原始报文）
        VehicleBasicInfo vehicleInfo = vehBasicInfoRepository.selectByVin(vin);
        if (ObjUtil.isNull(vehicleInfo)) {
            log.warn("补发车辆[{}]生产事件失败: 车辆不存在", vin);
            return VehicleImportReplayActionResult.failedFinal("VEHICLE_NOT_FOUND", "车辆不存在，无法构造当前快照");
        }

        VehicleProduceEventEnvelope.VehicleProducePayload payload = VehicleProduceEventEnvelope.VehicleProducePayload.builder()
                .vin(vin)
                .produceTime(LocalDateTime.now())
                .plantCode(vehicleInfo.getPlantCode())
                .brandCode(vehicleInfo.getBrandCode())
                .platformCode(vehicleInfo.getPlatformCode())
                .carLineCode(vehicleInfo.getCarLineCode())
                .modelCode(vehicleInfo.getModelCode())
                .variantCode(vehicleInfo.getVariantCode())
                .configurationCode(vehicleInfo.getConfigurationCode())
                .orderNum(vehicleInfo.getOrderNum())
                .build();

        VehicleProduceEventEnvelope envelope = VehicleProduceEventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .aggregateType(VehicleImportReplayAggregateType.VEHICLE.getValue())
                .aggregateId(vin)
                .version(target.aggregateVersion())
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
                .eventId(envelope.getEventId())
                .eventType(EVENT_TYPE)
                .aggregateType(VehicleImportReplayAggregateType.VEHICLE.getValue())
                .aggregateId(vin)
                .aggregateVersion(envelope.getVersion())
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.VEHICLE_PRODUCE))
                .messageKey(vin)
                .payload(JSONUtil.toJsonStr(envelope))
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(context.getReplayId())
                .createTime(LocalDateTime.now())
                .build();

        return VehicleImportReplayActionResult.queued(envelope.getEventId(), outbox);
    }
}
