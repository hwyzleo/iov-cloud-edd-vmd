package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationLifecycleCompensationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 首次激活事实 → 生命周期补偿任务（VMD-DSN-CR-079 / D079-05、D079-06、D079-07）
 * <p>
 * 两类有界任务，均复用同一幂等写入算法：
 * <ol>
 *   <li><strong>未建档补偿（默认开启）</strong>：分页扫描 {@code vin_resolved=0}，
 *       只处理主档已存在的投影；无匹配继续保留，<strong>不创建主档</strong>（D079-05）</li>
 *   <li><strong>存量对齐（默认关闭，一次）</strong>：分页扫描 {@code vin_resolved=1}，
 *       把既有激活投影幂等对齐到生命周期；既有更早时间优先保留（D079-06）</li>
 * </ol>
 * 约束：补偿失败只记指标留待下一轮；补偿 backlog 以 Gauge 观测，不参与 Kafka 就绪判定；
 * 补偿任务不可用不阻断既有消费启动与运行（D079-07）。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.activation-fact.lifecycle.compensation.enabled",
        havingValue = "true", matchIfMissing = true)
public class VehicleActivationLifecycleCompensationScheduler {

    /** backlog 指标原因标签：未建档待解析投影 */
    private static final String PENDING_REASON_VIN_UNRESOLVED = "vin_unresolved";

    private final VehicleActivationLifecycleCompensationAppService compensationAppService;
    private final VmdActivationFactMetrics activationFactMetrics;
    private final VmdKafkaTopicProperties topicProperties;

    /** 存量对齐是否已在本次进程内执行（一次性；失败下一轮重试） */
    private final AtomicBoolean backfillCompleted = new AtomicBoolean(false);

    /** 未建档补偿单页大小 */
    @Value("${vmd.activation-fact.lifecycle.compensation.batch-size:200}")
    private int compensationBatchSize;

    /** 未建档补偿单轮最大页数 */
    @Value("${vmd.activation-fact.lifecycle.compensation.max-batches-per-run:20}")
    private int compensationMaxBatches;

    /** 存量对齐开关（默认关闭，需显式开启） */
    @Value("${vmd.activation-fact.lifecycle.backfill.enabled:false}")
    private boolean backfillEnabled;

    /** 存量对齐单页大小 */
    @Value("${vmd.activation-fact.lifecycle.backfill.batch-size:200}")
    private int backfillBatchSize;

    /** 存量对齐单轮最大页数 */
    @Value("${vmd.activation-fact.lifecycle.backfill.max-batches-per-run:20}")
    private int backfillMaxBatches;

    /**
     * 定时补偿（有界）
     * <p>
     * 首次执行延迟默认 60s，避免与启动期 MDM Bootstrap / 建表迁移 / 消费回暖抢资源；
     * 单条失败不影响其余 VIN 与后续轮次。
     */
    @Scheduled(fixedDelayString = "${vmd.activation-fact.lifecycle.compensation.interval-ms:300000}",
            initialDelayString = "${vmd.activation-fact.lifecycle.compensation.initial-delay-ms:60000}")
    public void compensateLifecycle() {
        if (backfillEnabled && backfillCompleted.compareAndSet(false, true)) {
            runBackfill();
        }
        try {
            int processed = compensationAppService.reconcileUnresolved(
                    compensationBatchSize, compensationMaxBatches);
            long backlog = compensationAppService.countUnresolved();
            if (backlog >= 0) {
                activationFactMetrics.updateLifecyclePending(PENDING_REASON_VIN_UNRESOLVED, backlog);
            }
            if (processed > 0) {
                log.info("首次激活生命周期未建档补偿完成: processed={}, backlog={}", processed, backlog);
            }
        } catch (Exception e) {
            activationFactMetrics.recordLifecycle(
                    topicProperties.getUserVehicleActivationFact(), "FAILED");
            log.warn("首次激活生命周期未建档补偿失败（留待下一轮，不影响消费）: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行一次性存量对齐（失败时允许下一轮重试）
     */
    private void runBackfill() {
        try {
            int processed = compensationAppService.alignResolved(backfillBatchSize, backfillMaxBatches);
            log.info("首次激活生命周期存量对齐完成: processed={}", processed);
        } catch (Exception e) {
            backfillCompleted.set(false);
            log.warn("首次激活生命周期存量对齐失败（下一轮重试）: {}", e.getMessage(), e);
        }
    }
}
