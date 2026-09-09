package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
import org.springframework.stereotype.Component;

/**
 * 配置投影完整性检查器（CR-047 §4.2）
 * <p>
 * Bootstrap/entity=all 同步完成后运行，统计有效 Configuration 中缺失 Variant / Model / CarLine /
 * Platform / Brand 引用差异，输出日志并更新监控指标。Configuration 写入不建立数据库强外键阻塞，
 * 由本检查器在同步完成后兜底核对。
 * </p>
 *
 * @author CR-047
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectionIntegrityChecker {

    private final MdmConfigurationRepository mdmConfigurationRepository;
    private final ConfigurationSyncMetrics configurationSyncMetrics;

    /**
     * 执行产品树引用完整性检查
     */
    public void check() {
        long missingVariant = mdmConfigurationRepository.countMissingVariant();
        long missingHierarchy = mdmConfigurationRepository.countMissingHierarchy();
        configurationSyncMetrics.recordMissingVariant(missingVariant);
        configurationSyncMetrics.recordHierarchyMismatch(missingHierarchy);

        if (missingVariant > 0) {
            log.warn("Configuration 投影完整性检查：缺失 Variant 引用 {} 条（variantCode 无法在 tb_mdm_variant 追溯）", missingVariant);
        } else {
            log.info("Configuration 投影完整性检查：Variant 引用完整");
        }
        if (missingHierarchy > 0) {
            log.warn("Configuration 投影完整性检查：产品树层级缺失 {} 条（Variant/Model/CarLine/Platform/Brand 任一层不可达）", missingHierarchy);
        } else {
            log.info("Configuration 投影完整性检查：产品树引用完整");
        }
    }
}
