package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryConsistencyAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.PartCategoryGovernanceMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 零件派生设备类别一致性巡检与收敛（EDD-VMD-DSN-CR-078 / D-VMD-78-05）
 * <p>
 * 定时只读巡检：把 `tb_part_info.device_category` 与 MDM 节点类别不一致的条数写入 gauge，
 * 使不一致可被发现与告警；`auto-repair` 默认开启（用户 2026-11-25 确认）时，
 * 按不一致的车载节点分组执行既有受控修复（写属性变更历史，trigger=SCHEDULED）。
 * </p>
 * <p>
 * 与 MDM 节点类别变更触发的即时收敛（{@link net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryAutoConvergenceService}）
 * 互补：即时收敛追求时效，本巡检负责兜底与可观测。单个节点收敛失败不影响其余节点与后续轮次。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.part-category.consistency.scheduler.enabled",
        havingValue = "true", matchIfMissing = true)
public class PartCategoryConsistencyScheduler {

    private final PartCategoryConsistencyAppService consistencyAppService;
    private final PartCategoryGovernanceMetrics metrics;

    /**
     * 是否在巡检后自动收敛（默认开启；关闭后仅观测与告警）
     */
    @Value("${vmd.part-category.consistency.auto-repair.enabled:true}")
    private boolean autoRepairEnabled;

    /**
     * 巡检间隔（毫秒），默认 10 分钟
     */
    @Value("${vmd.part-category.consistency.scheduler.interval-ms:600000}")
    private long intervalMillis;

    /**
     * 巡检 + （可选）自动收敛
     * <p>
     * 首次执行延迟默认 60s（{@code initial-delay-ms}）：避免启动瞬间与 MDM Bootstrap/消费回暖抢资源，
     * 也避免与启动期的节点投影收敛重复。
     * </p>
     */
    @Scheduled(fixedDelayString = "${vmd.part-category.consistency.scheduler.interval-ms:600000}",
            initialDelayString = "${vmd.part-category.consistency.scheduler.initial-delay-ms:60000}")
    public void inspectAndConverge() {
        long total;
        try {
            total = consistencyAppService.countInconsistent();
        } catch (Exception e) {
            log.warn("零件派生类别一致性巡检失败（不影响业务链路）: {}", e.getMessage(), e);
            return;
        }
        metrics.updateInconsistency(total);
        if (total == 0) {
            log.debug("零件派生类别一致性巡检通过，无不一致记录");
            return;
        }
        log.warn("零件派生类别与 MDM 节点类别不一致: total={}, autoRepair={}, intervalMs={}",
                total, autoRepairEnabled, intervalMillis);
        if (!autoRepairEnabled) {
            return;
        }
        List<String> nodeCodes;
        try {
            nodeCodes = consistencyAppService.listInconsistentVehicleNodeCodes();
        } catch (Exception e) {
            log.warn("查询不一致车载节点失败，跳过本轮自动收敛: {}", e.getMessage(), e);
            return;
        }
        for (String nodeCode : nodeCodes) {
            try {
                PartCategoryConsistencyAppService.RepairResult result = consistencyAppService
                        .convergeByVehicleNodeCode(nodeCode, PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
                metrics.recordRepair(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED,
                        result.getApplied(), result.getSkipped(), result.getConflict(), result.getFailed());
                log.info("定时自动收敛完成: nodeCode={}, total={}, applied={}, skipped={}, conflict={}, failed={}",
                        nodeCode, result.getTotal(), result.getApplied(), result.getSkipped(),
                        result.getConflict(), result.getFailed());
            } catch (Exception e) {
                metrics.recordRepairFailure(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
                log.warn("定时自动收敛失败: nodeCode={}, error={}", nodeCode, e.getMessage(), e);
            }
        }
        try {
            metrics.updateInconsistency(consistencyAppService.countInconsistent());
        } catch (Exception e) {
            log.warn("自动收敛后复算不一致条数失败: {}", e.getMessage());
        }
    }
}
