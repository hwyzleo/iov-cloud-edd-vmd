package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 导入操作恢复服务（CR-071 §9）
 * <p>
 * 定时扫描失联操作（RUNNING 且租约过期）：新 worker 认领（fenceToken 单调递增，
 * 旧 worker 后续提交被 verifyFence 拒绝），按已提交 item/Outbox/历史检查点重建结果
 * （幂等机制保证不重复写入），重新执行对应域执行器并收敛终态。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportOperationRecoveryService {

    private final ImportOperationRepository importOperationRepository;
    private final ImportReimportExecutor importReimportExecutor;

    /** 恢复执行器标识 */
    private static final String RECOVERY_WORKER = "recovery-worker-" + UUID.randomUUID().toString().substring(0, 8);

    /** 租约时长（秒） */
    private static final int LEASE_SECONDS = 60;

    /**
     * 定时恢复失联操作（拟每 60s）
     */
    @Scheduled(fixedDelayString = "${vmd.import-operation.recovery-interval-ms:60000}")
    public void recoverExpired() {
        List<ImportOperation> expired = importOperationRepository.selectExpired(LocalDateTime.now(ZoneOffset.UTC));
        if (expired.isEmpty()) {
            return;
        }
        log.info("导入操作恢复扫描: 发现失联操作 {} 个", expired.size());
        for (ImportOperation operation : expired) {
            try {
                recover(operation);
            } catch (Exception e) {
                log.error("恢复操作[{}]失败: {}", operation.getOperationId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 恢复单个失联操作：认领（RECOVERING 且租约过期可接管）→ 重跑执行器（幂等）→ 收敛终态
     */
    public void recover(ImportOperation operation) {
        // 1. 认领（claim 对 RECOVERING 且租约过期开放；fenceToken 递增使旧 worker 失效）
        int claimed = importOperationRepository.claim(operation.getOperationId(), RECOVERY_WORKER,
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(LEASE_SECONDS));
        if (claimed == 0) {
            log.warn("恢复认领失败: operationId={}（可能已被他人接管）", operation.getOperationId());
            return;
        }
        log.info("恢复接管操作: operationId={}, 新fenceToken 已递增", operation.getOperationId());

        // 2. 从 planJson 重建执行上下文（scope 用计划候选）
        List<String> scope = null;
        String repairPolicy = "FILL_MISSING";
        try {
            JSONObject plan = JSONUtil.parseObj(operation.getPlanJson());
            if (plan.containsKey("candidates")) {
                scope = plan.getBeanList("candidates", String.class);
            }
            if (plan.containsKey("repairPolicy")) {
                repairPolicy = plan.getStr("repairPolicy");
            }
        } catch (Exception e) {
            log.warn("恢复解析计划失败: operationId={}, 使用全量候选", operation.getOperationId());
        }

        // 3. 重跑执行器（幂等保护，不重造 eventId/软件事实）
        int changed = 0, unchanged = 0, skipped = 0, failed = 0;
        String status = ImportOperationAppService.STATUS_SUCCEEDED;
        try {
            ImportReimportExecutor.ExecuteContext ctx = ImportReimportExecutor.ExecuteContext.builder()
                    .operationId(operation.getOperationId())
                    .operatorId(operation.getOperatorId())
                    .operatorName(operation.getOperatorName())
                    .build();
            if (ImportOperationAppService.MODE_REIMPORT.equals(operation.getOperationType())) {
                ImportReimportExecutor.ReimportOutcome outcome = ImportOperationAppService.DOMAIN_VEHICLE.equals(operation.getDomain())
                        ? importReimportExecutor.executeVehicle(operation, scope, ctx)
                        : importReimportExecutor.executePart(operation, scope, ctx);
                changed = outcome.changed();
                unchanged = outcome.unchanged();
                skipped = outcome.skipped();
                failed = outcome.failed();
                status = failed > 0 ? ImportOperationAppService.STATUS_PARTIAL_FAILED : ImportOperationAppService.STATUS_SUCCEEDED;
            } else {
                // REPLAY 恢复：依赖旧补发审计（replayId 幂等），此处仅收敛状态
                status = ImportOperationAppService.STATUS_SUCCEEDED;
            }
        } catch (Exception e) {
            log.error("恢复执行异常: operationId={}, error={}", operation.getOperationId(), e.getMessage(), e);
            status = ImportOperationAppService.STATUS_FAILED;
            failed = 1;
        }

        // 4. 收敛终态
        String countersJson = String.format("{\"changed\":%d,\"unchanged\":%d,\"skipped\":%d,\"failed\":%d}",
                changed, unchanged, skipped, failed);
        importOperationRepository.finish(operation.getOperationId(), RECOVERY_WORKER, status, countersJson);
        log.info("恢复完成: operationId={}, status={}, counters={}", operation.getOperationId(), status, countersJson);
    }

}
