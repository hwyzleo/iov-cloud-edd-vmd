package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleSoftwareInventoryChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 软件实装清单应用服务类
 * <p>
 * 管理零件软件安装记录，支持软件清单管理和历史追溯
 * 实现版本时序 gate → provisional/confirmed → 来源优先级兜底消解算法
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareInventoryAppService {

    private final PartSoftwareInstallationRepository partSoftwareInstallationRepository;
    private final ApplicationEventPublisher eventPublisher;

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
     * 应用软件清单（CR-045 多来源消解）
     * <p>
     * 消解算法：
     * 1. 幂等去重（source + requestId/sourceEventId）
     * 2. 版本时序 gate（sourceEventTime <= current 则忽略）
     * 3. provisional/confirmed 语义判定
     * 4. 同版本并列→来源优先级兜底
     *
     * @param partId 零件ID
     * @param bindingId 绑定ID（可空）
     * @param vinSnapshot VIN快照
     * @param softwareTargetCode 软件目标代码
     * @param softwarePartNo 软件零件号
     * @param softwareVersion 软件版本
     * @param artifactHash 制品摘要（可空）
     * @param slot 槽位（可空）
     * @param changeType 变更类型
     * @param source 来源
     * @param sourceEventId 来源事件幂等键
     * @param sourceEventTime 来源事件时间（版本时序gate判定用）
     * @param reportedAt 源端观测时间
     * @param isConfirmed 是否已确认
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
            Boolean isConfirmed) {

        log.debug("应用软件清单消解: partId={}, targetCode={}, version={}, source={}, isConfirmed={}",
                partId, softwareTargetCode, softwareVersion, source, isConfirmed);

        // 1. 幂等去重：检查是否已有相同 source + sourceEventId 的记录
        PartSoftwareInstallation existingByEvent = partSoftwareInstallationRepository
                .selectBySourceAndSourceEventId(source, sourceEventId, softwareTargetCode, slot);
        if (existingByEvent != null) {
            log.debug("幂等命中: source={}, sourceEventId={}, targetCode={}", source, sourceEventId, softwareTargetCode);
            return new ApplyManifestResult(false, false, existingByEvent.getInventoryVersion());
        }

        // 2. 获取当前 ACTIVE 记录
        PartSoftwareInstallation currentRecord = partSoftwareInstallationRepository
                .selectActiveByPartIdAndTargetCode(partId, softwareTargetCode);

        // 3. 版本时序 gate：如果来源事件时间 <= 当前记录的来源事件时间，则忽略
        if (currentRecord != null && sourceEventTime != null && currentRecord.getSourceEventTime() != null) {
            if (!sourceEventTime.isAfter(currentRecord.getSourceEventTime())) {
                log.debug("版本时序gate命中: sourceEventTime={}, currentSourceEventTime={}",
                        sourceEventTime, currentRecord.getSourceEventTime());
                return new ApplyManifestResult(false, true, currentRecord.getInventoryVersion());
            }
        }

        // 4. 来源优先级判定
        if (currentRecord != null && currentRecord.getSoftwareVersion().equals(softwareVersion)) {
            // 同版本并列：检查来源优先级
            int currentPriority = SOURCE_PRIORITY.getOrDefault(currentRecord.getSource(), Integer.MAX_VALUE);
            int newPriority = SOURCE_PRIORITY.getOrDefault(source, Integer.MAX_VALUE);

            // 如果新记录优先级不高于当前记录，且当前记录是 confirmed，则忽略
            if (newPriority >= currentPriority && Boolean.TRUE.equals(currentRecord.getIsConfirmed())) {
                log.debug("来源优先级兜底: currentSource={}, newSource={}, currentPriority={}, newPriority={}",
                        currentRecord.getSource(), source, currentPriority, newPriority);
                return new ApplyManifestResult(false, false, currentRecord.getInventoryVersion());
            }
        }

        // 5. 关闭当前 ACTIVE 记录
        if (currentRecord != null) {
            partSoftwareInstallationRepository.deactivateByPartIdAndTargetCode(partId, softwareTargetCode);
        }

        // 6. 获取当前最大 inventory_version
        Long currentVersion = getMaxInventoryVersion(partId);
        Long newVersion = currentVersion + 1;

        // 7. 创建新的 ACTIVE 记录
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
                .build();

        newRecord.init();
        partSoftwareInstallationRepository.insert(newRecord);

        log.info("软件清单已更新: partId={}, targetCode={}, version={}, source={}, isConfirmed={}, inventoryVersion={}",
                partId, softwareTargetCode, softwareVersion, source, isConfirmed, newVersion);

        // 8. 发布领域事件
        eventPublisher.publishEvent(new VehicleSoftwareInventoryChangedEvent(
                vinSnapshot, bindingId, partId,
                softwareTargetCode, softwarePartNo,
                softwareVersion, slot,
                changeType, source,
                isConfirmed, newVersion,
                Instant.now()));

        return new ApplyManifestResult(true, false, newVersion);
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
