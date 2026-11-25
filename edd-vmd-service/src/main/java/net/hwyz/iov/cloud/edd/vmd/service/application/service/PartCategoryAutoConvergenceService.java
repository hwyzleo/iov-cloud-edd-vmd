package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.PartCategoryGovernanceMetrics;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Service;

/**
 * 零件派生设备类别自动收敛（EDD-VMD-DSN-CR-078 / D-VMD-78-03、D-VMD-78-04）
 * <p>
 * MDM 车载节点设备类别发生变化时，按 {@code vehicle_node_code} 把该节点下零件实例的派生类别
 * 收敛到节点类别，复用既有受控修复语义（MDM 值比对 + rowVersion 乐观锁 + 属性变更历史）。
 * </p>
 * <p>
 * best-effort 语义：调用方（MDM 节点投影内核）的上游事实优先级高于派生列，
 * 因此本方法吞掉全部异常，只计数与告警，不向上抛出、不影响节点投影与消费位点；
 * 未被收敛的条目由定时巡检兜住。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartCategoryAutoConvergenceService {

    private final PartCategoryConsistencyAppService consistencyAppService;
    private final PartCategoryGovernanceMetrics metrics;

    /**
     * 节点类别变更后触发收敛（best-effort，不抛异常）
     *
     * @param vehicleNodeCode 车载节点代码
     * @param previousCategory 变更前类别（观测用）
     * @param currentCategory  变更后类别（观测用）
     */
    public void onNodeCategoryChanged(String vehicleNodeCode, String previousCategory, String currentCategory) {
        if (StrUtil.isBlank(vehicleNodeCode) || StrUtil.isBlank(currentCategory)) {
            return;
        }
        try {
            PartCategoryConsistencyAppService.RepairResult result = consistencyAppService
                    .convergeByVehicleNodeCode(vehicleNodeCode.trim(),
                            PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED);
            metrics.recordRepair(PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED,
                    result.getApplied(), result.getSkipped(), result.getConflict(), result.getFailed());
            log.info("节点类别变更自动收敛完成: nodeCode={}, {} -> {}, total={}, applied={}, skipped={}, conflict={}, failed={}",
                    vehicleNodeCode, previousCategory, currentCategory, result.getTotal(),
                    result.getApplied(), result.getSkipped(), result.getConflict(), result.getFailed());
        } catch (Exception e) {
            metrics.recordRepairFailure(PartCategoryConsistencyAppService.TRIGGER_NODE_CHANGED);
            log.warn("节点[{}]类别变更自动收敛失败（不影响节点投影，由定时巡检兜底）: {}",
                    vehicleNodeCode, e.getMessage(), e);
        }
    }
}
