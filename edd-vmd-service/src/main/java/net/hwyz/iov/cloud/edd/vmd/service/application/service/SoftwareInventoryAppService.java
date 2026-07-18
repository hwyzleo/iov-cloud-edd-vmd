package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 软件实装清单应用服务类
 * <p>
 * 管理零件软件安装记录，支持软件清单管理和历史追溯
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareInventoryAppService {

    private final PartSoftwareInstallationRepository partSoftwareInstallationRepository;

    /**
     * 应用软件清单（CR-041/CR-043）
     * <p>
     * 同一事务内处理软件清单变更：
     * 1. 关闭当前ACTIVE记录
     * 2. 插入新的ACTIVE记录
     * 3. 支持software_type和flash_result字段
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
     * @param reportedAt 源端观测时间
     * @param softwareType 软件类型（CR-043）
     * @param flashResult 刷写结果（CR-043）
     * @return 新增的软件实装记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PartSoftwareInstallation applyManifest(
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
            Instant reportedAt,
            String softwareType,
            String flashResult) {
        
        log.debug("应用软件清单: partId={}, targetCode={}, version={}", partId, softwareTargetCode, softwareVersion);
        
        // 1. 停用当前ACTIVE记录
        partSoftwareInstallationRepository.deactivateByPartIdAndTargetCode(partId, softwareTargetCode);
        
        // 2. 获取当前最大inventory_version
        Long currentVersion = getMaxInventoryVersion(partId);
        Long newVersion = currentVersion + 1;
        
        // 3. 创建新的ACTIVE记录
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
                .reportedAt(reportedAt)
                .inventoryVersion(newVersion)
                .softwareType(softwareType)
                .flashResult(flashResult)
                .build();
        
        newRecord.init();
        partSoftwareInstallationRepository.insert(newRecord);
        
        log.info("软件清单已更新: partId={}, targetCode={}, version={}, inventoryVersion={}", 
                partId, softwareTargetCode, softwareVersion, newVersion);
        
        return newRecord;
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
     * 获取零件的最大inventory_version
     *
     * @param partId 零件ID
     * @return 最大inventory_version（如果没有记录则返回0L）
     */
    private Long getMaxInventoryVersion(Long partId) {
        List<PartSoftwareInstallation> records = partSoftwareInstallationRepository.selectByPartId(partId);
        return records.stream()
                .mapToLong(PartSoftwareInstallation::getInventoryVersion)
                .max()
                .orElse(0L);
    }
}
