package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.Kl1BatchSweepAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * KL1 批次到期 sweeper（VMD-DSN-CR-069 §4）。
 * <p>
 * 每 ≤5s 扫描 now≥B 的 pending KL1 批次，与 Request/ACK/Status 入口惰性到期共用
 * {@link Kl1BatchSweepAppService#sweep}（同一 CAS FAILED(ACK_TIMEOUT) + 释放 claims 规则）。
 * Provider 处理中超过 120s 且副作用 unknown 只隔离告警，不按可释放失败处理。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.business-key.provisioning.kl1.sweeper.enabled", havingValue = "true", matchIfMissing = true)
public class Kl1BatchSweeper {

    private final Kl1BatchSweepAppService kl1BatchSweepAppService;

    @Value("${vmd.business-key.provisioning.kl1.sweeper.batch-size:200}")
    private int batchSize;

    /**
     * 每 5s 扫批（可配置；设计上限 ≤5s）。
     */
    @Scheduled(fixedDelayString = "${vmd.business-key.provisioning.kl1.sweeper.fixed-delay-ms:5000}")
    public void sweep() {
        try {
            int processed = kl1BatchSweepAppService.sweep(batchSize);
            if (processed > 0) {
                log.info("KL1 批次到期扫批完成: terminated={}", processed);
            }
        } catch (Exception e) {
            log.error("KL1 批次到期扫批异常", e);
        }
    }
}
