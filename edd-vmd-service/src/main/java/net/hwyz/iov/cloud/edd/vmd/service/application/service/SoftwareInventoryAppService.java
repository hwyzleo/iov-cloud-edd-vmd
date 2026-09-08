package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleSoftwareInventoryChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareSourceUnsupportedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareSourceVersionMissingException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 软件实装清单应用服务类
 * <p>
 * 管理零件软件安装记录，支持软件清单管理和历史追溯
 * 实现版本时序 gate → provisional/confirmed → 来源优先级兜底消解算法
 * <p>
 * VMD-DSN-CR-046: 消解锚点按 (partId, softwareTargetCode, slot) 逐 Slot 维护，
 * is_active_slot 与 install_state 分离；SSOT 实际变化时经 vmd_outbox 发布
 * VehicleSoftwareInventoryChangedEvent（Key=VIN）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareInventoryAppService {

    private final PartSoftwareInstallationRepository partSoftwareInstallationRepository;
    private final VmdOutboxRepository vmdOutboxRepository;
    private final PartInfoRepository partInfoRepository;
    private final VehiclePartRepository vehiclePartRepository;

    /**
     * 软件清单变更事件 Kafka topic（CR-046 起经 Outbox Relay 发布）
     */
    @Value("${vmd.software-inventory.changed.kafka.topic:vmd-vehicle-software-inventory-changed}")
    private String changedEventTopic;

    /**
     * 来源优先级（数值越小优先级越高）
     * MANUAL/AFTER_SALES > VEHICLE_REPORT > OTA > EOL
     */
    private static final Map<String, Integer> SOURCE_PRIORITY = new HashMap<>();

    static {
        SOURCE_PRIORITY.put("MANUAL", 1);
        SOURCE_PRIORITY.put("AFTER_SALES", 1);
        SOURCE_PRIORITY.put("VEHICLE_REPORT", 2);
        SOURCE_PRIORITY.put("OTA", 3);
        SOURCE_PRIORITY.put("EOL", 4);
    }

    /**
     * 应用软件清单（CR-045 兼容签名，委托全量签名）
     *
     * @see #applyManifest(Long, Long, String, String, String, String, String, String, String, String, String, Instant, Instant, Boolean, Boolean, String, Integer, String, Instant)
     */
    @Transactional(rollbackFor = Exception.class)
    public ApplyManifestResult applyManifest(
            Long partId,
            Long bindingId,
            String vinSnapshot,
            String softwareTargetCode,
            String softwarePartNo,
            String softwareVersion,
            String artifactHash,
            String slot,
            String changeType,
            String source,
            String sourceEventId,
            Instant sourceEventTime,
            Instant reportedAt,
            Boolean isConfirmed) {

        return applyManifest(partId, bindingId, vinSnapshot,
                softwareTargetCode, softwarePartNo, softwareVersion,
                artifactHash, slot, changeType, source, sourceEventId,
                sourceEventTime, reportedAt, isConfirmed,
                null, null, null, null, null);
    }

    /**
     * 应用软件清单（CR-046 全量签名）
     * <p>
     * 消解算法：
     * 1. 幂等去重（source + sourceEventId + softwareTargetCode + slot）
     * 2. 版本时序 gate（sourceEventTime <= 当前则忽略）
     * 3. provisional/confirmed 语义判定
     * 4. 同版本并列→来源优先级兜底（active 槽切换除外）
     * 5. SSOT 实际变化 → 经 vmd_outbox 发布 VehicleSoftwareInventoryChangedEvent
     *
     * @param partId 零件ID
     * @param bindingId 绑定ID（可空）
     * @param vinSnapshot VIN快照
     * @param softwareTargetCode 软件目标代码
     * @param softwarePartNo 软件零件号
     * @param softwareVersion 软件版本
     * @param artifactHash 制品摘要（可空）
     * @param slot 槽位（可空，SINGLE_IMAGE）
     * @param changeType 变更类型
     * @param source 来源
     * @param sourceEventId 来源事件幂等键
     * @param sourceEventTime 来源事件时间（版本时序gate判定用）
     * @param reportedAt 源端观测时间
     * @param isConfirmed 是否已确认
     * @param isActiveSlot 是否当前启动槽（可空，SINGLE_IMAGE 默认 true）
     * @param observationKey OTA FULL 观测身份（CR-046，可空）
     * @param canonicalizationVersion canonicalization 版本（CR-046，可空）
     * @param canonicalDigest canonical 摘要（CR-046，可空）
     * @param sourceAcceptedAt IOV-OTA 成功受理时间（CR-046，仅审计）
     * @return 消解结果：applied=是否写入, ignoredByVersionGate=是否被版本gate忽略
     */
    @Transactional(rollbackFor = Exception.class)
    public ApplyManifestResult applyManifest(
            Long partId,
            Long bindingId,
            String vinSnapshot,
            String softwareTargetCode,
            String softwarePartNo,
            String softwareVersion,
            String artifactHash,
            String slot,
            String changeType,
            String source,
            String sourceEventId,
            Instant sourceEventTime,
            Instant reportedAt,
            Boolean isConfirmed,
            Boolean isActiveSlot,
            String observationKey,
            Integer canonicalizationVersion,
            String canonicalDigest,
            Instant sourceAcceptedAt) {

        log.debug("应用软件清单消解: partId={}, targetCode={}, slot={}, version={}, source={}, isConfirmed={}, isActiveSlot={}",
                partId, softwareTargetCode, slot, softwareVersion, source, isConfirmed, isActiveSlot);

        // 0. 基础校验
        if (!SOURCE_PRIORITY.containsKey(source)) {
            throw new SoftwareSourceUnsupportedException(source);
        }
        if (sourceEventId == null || sourceEventId.isBlank() || sourceEventTime == null) {
            throw new SoftwareSourceVersionMissingException(
                    "缺少 sourceEventId/sourceEventTime，无法做版本时序判定");
        }
        boolean activeSlot = isActiveSlot == null || isActiveSlot;

        // 1. 幂等去重：检查是否已有相同 source + sourceEventId + targetCode + slot 的记录
        PartSoftwareInstallation existingByEvent = partSoftwareInstallationRepository
                .selectBySourceAndSourceEventId(source, sourceEventId, softwareTargetCode, slot);
        if (existingByEvent != null) {
            log.debug("幂等命中: source={}, sourceEventId={}, targetCode={}, slot={}", source, sourceEventId, softwareTargetCode, slot);
            return new ApplyManifestResult(false, false, existingByEvent.getInventoryVersion());
        }

        // 2. 获取当前 ACTIVE 记录（按 Target + Slot 锚点）
        PartSoftwareInstallation currentRecord = partSoftwareInstallationRepository
                .selectActiveByPartIdTargetCodeAndSlot(partId, softwareTargetCode, slot);

        // 3. 版本时序 gate：如果来源事件时间 <= 当前记录的来源事件时间，则忽略
        if (currentRecord != null && sourceEventTime != null && currentRecord.getSourceEventTime() != null) {
            if (!sourceEventTime.isAfter(currentRecord.getSourceEventTime())) {
                log.debug("版本时序gate命中: sourceEventTime={}, currentSourceEventTime={}",
                        sourceEventTime, currentRecord.getSourceEventTime());
                return new ApplyManifestResult(false, true, currentRecord.getInventoryVersion());
            }
        }

        // 4. 同版本并列仲裁：来源优先级兜底（active 槽切换视为 SSOT 变化，不按同版本忽略）
        if (currentRecord != null && currentRecord.getSoftwareVersion().equals(softwareVersion)) {
            boolean activeSlotSwitch = activeSlot && !Boolean.TRUE.equals(currentRecord.getIsActiveSlot());
            if (!activeSlotSwitch) {
                int currentPriority = SOURCE_PRIORITY.getOrDefault(currentRecord.getSource(), Integer.MAX_VALUE);
                int newPriority = SOURCE_PRIORITY.getOrDefault(source, Integer.MAX_VALUE);
                if (newPriority >= currentPriority && Boolean.TRUE.equals(currentRecord.getIsConfirmed())) {
                    log.debug("来源优先级兜底: currentSource={}, newSource={}, currentPriority={}, newPriority={}",
                            currentRecord.getSource(), source, currentPriority, newPriority);
                    return new ApplyManifestResult(false, false, currentRecord.getInventoryVersion());
                }
            } else {
                log.debug("active槽切换: currentSlot={} standby→active, targetCode={}", slot, softwareTargetCode);
            }
        }

        // 5. 关闭当前 ACTIVE 记录（按 Target + Slot 锚点）
        if (currentRecord != null) {
            partSoftwareInstallationRepository.deactivateByPartIdTargetCodeAndSlot(partId, softwareTargetCode, slot);
        }

        // 6. active 槽切换：新记录为启动槽时，重置同一实例+Target 下其他 ACTIVE 槽的 active 标记
        if (activeSlot) {
            partSoftwareInstallationRepository.resetActiveSlotByPartIdAndTargetCode(partId, softwareTargetCode);
        }

        // 7. 获取当前最大 inventory_version
        Long currentVersion = getMaxInventoryVersion(partId);
        Long newVersion = currentVersion + 1;

        // 8. 创建新的 ACTIVE 记录
        PartSoftwareInstallation newRecord = PartSoftwareInstallation.builder()
                .partId(partId)
                .bindingId(bindingId)
                .vinSnapshot(vinSnapshot)
                .softwareTargetCode(softwareTargetCode)
                .softwarePartNo(softwarePartNo)
                .softwareVersion(softwareVersion)
                .artifactHash(artifactHash)
                .slot(slot)
                .installState("ACTIVE")
                .changeType(changeType)
                .effectiveFrom(Instant.now())
                .effectiveTo(null)
                .source(source)
                .sourceEventId(sourceEventId)
                .sourceEventTime(sourceEventTime)
                .reportedAt(reportedAt)
                .inventoryVersion(newVersion)
                .isConfirmed(isConfirmed)
                .isActiveSlot(activeSlot)
                .observationKey(observationKey)
                .canonicalizationVersion(canonicalizationVersion)
                .canonicalDigest(canonicalDigest)
                .sourceAcceptedAt(sourceAcceptedAt)
                .build();

        newRecord.init();
        partSoftwareInstallationRepository.insert(newRecord);

        log.info("软件清单已更新: partId={}, targetCode={}, slot={}, version={}, source={}, isConfirmed={}, isActiveSlot={}, inventoryVersion={}",
                partId, softwareTargetCode, slot, softwareVersion, source, isConfirmed, activeSlot, newVersion);

        // 9. SSOT 实际变化 → 经 vmd_outbox 发布 VehicleSoftwareInventoryChangedEvent（Key=VIN）
        publishChangedEventOutbox(newRecord, vinSnapshot, bindingId, partId, source, isConfirmed, newVersion);

        return new ApplyManifestResult(true, false, newVersion);
    }

    /**
     * 发布软件清单变更事件到 VMD Outbox（由 OutboxRelay 至少一次发布到 Kafka）
     *
     * @param record 新写入的 ACTIVE 记录
     * @param vin VIN
     * @param bindingId 绑定ID
     * @param partId 零件ID
     * @param source 来源
     * @param isConfirmed 是否已确认
     * @param inventoryVersion 清单版本
     */
    private void publishChangedEventOutbox(PartSoftwareInstallation record, String vin,
                                           Long bindingId, Long partId,
                                           String source, Boolean isConfirmed, Long inventoryVersion) {
        try {
            // 补齐 partCode/sn（part_info）与 vehicleNodeCode（vehicle_part）
            String partCode = null;
            String sn = null;
            String vehicleNodeCode = null;
            PartInfo partInfo = partInfoRepository.selectById(partId);
            if (partInfo != null) {
                partCode = partInfo.getPartCode();
                sn = partInfo.getSn();
            }
            if (bindingId != null) {
                VehiclePart binding = vehiclePartRepository.selectById(bindingId);
                if (binding != null) {
                    vehicleNodeCode = binding.getVehicleNodeCode();
                }
            }

            VehicleSoftwareInventoryChangedEvent event = new VehicleSoftwareInventoryChangedEvent(
                    vin, bindingId, partId, partCode, sn, vehicleNodeCode,
                    record.getSoftwareTargetCode(), record.getSoftwarePartNo(),
                    record.getSoftwareVersion(), record.getSlot(), record.getIsActiveSlot(),
                    record.getArtifactHash(), record.getChangeType(), source,
                    isConfirmed, inventoryVersion, Instant.now());

            // 手工构造 payload，排除 BaseEvent/ApplicationEvent 无关字段
            JSONObject payload = JSONUtil.createObj()
                    .set("vin", event.getVin())
                    .set("bindingId", event.getBindingId())
                    .set("partId", event.getPartId())
                    .set("partCode", event.getPartCode())
                    .set("sn", event.getSn())
                    .set("vehicleNodeCode", event.getVehicleNodeCode())
                    .set("softwareTargetCode", event.getSoftwareTargetCode())
                    .set("softwarePartNo", event.getSoftwarePartNo())
                    .set("softwareVersion", event.getSoftwareVersion())
                    .set("slot", event.getSlot())
                    .set("active", event.getActive())
                    .set("digest", event.getDigest())
                    .set("changeType", event.getChangeType())
                    .set("source", event.getSource())
                    .set("isConfirmed", event.getIsConfirmed())
                    .set("inventoryVersion", event.getInventoryVersion())
                    .set("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);

            VmdOutbox outbox = VmdOutbox.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("VehicleSoftwareInventoryChangedEvent")
                    .aggregateType("PART_SOFTWARE_INSTALLATION")
                    .aggregateId(vin)
                    .aggregateVersion(inventoryVersion)
                    .topic(changedEventTopic)
                    .messageKey(vin)
                    .payload(payload.toString())
                    .publishState("PENDING")
                    .retryCount(0)
                    .sourceType("SOFTWARE_INVENTORY")
                    .createTime(LocalDateTime.now())
                    .build();
            vmdOutboxRepository.insert(outbox);
            log.info("软件清单变更事件写入Outbox: vin={}, targetCode={}, slot={}, inventoryVersion={}",
                    vin, record.getSoftwareTargetCode(), record.getSlot(), inventoryVersion);
        } catch (Exception e) {
            // 事件发布失败不影响软件实装事实写入（SSOT 已更新），仅告警
            log.error("软件清单变更事件写入Outbox失败: vin={}, error={}", vin, e.getMessage(), e);
        }
    }

    /**
     * 查询零件的当前软件清单
     *
     * @param partId 零件ID
     * @return 软件实装记录列表
     */
    public List<PartSoftwareInstallation> getCurrentInventory(Long partId) {
        return partSoftwareInstallationRepository.selectByPartId(partId);
    }

    /**
     * 查询零件指定目标代码的当前软件版本
     *
     * @param partId 零件ID
     * @param softwareTargetCode 软件目标代码
     * @return 软件实装记录（可能为null）
     */
    public PartSoftwareInstallation getCurrentVersion(Long partId, String softwareTargetCode) {
        return partSoftwareInstallationRepository.selectActiveByPartIdAndTargetCode(partId, softwareTargetCode);
    }

    /**
     * 获取零件的最大 inventory_version
     *
     * @param partId 零件ID
     * @return 最大 inventory_version（如果没有记录则返回 0L）
     */
    private Long getMaxInventoryVersion(Long partId) {
        List<PartSoftwareInstallation> records = partSoftwareInstallationRepository.selectByPartId(partId);
        return records.stream()
                .mapToLong(PartSoftwareInstallation::getInventoryVersion)
                .max()
                .orElse(0L);
    }

    /**
     * 消解结果
     */
    public record ApplyManifestResult(
            boolean applied,
            boolean ignoredByVersionGate,
            Long currentInventoryVersion
    ) {}
}
