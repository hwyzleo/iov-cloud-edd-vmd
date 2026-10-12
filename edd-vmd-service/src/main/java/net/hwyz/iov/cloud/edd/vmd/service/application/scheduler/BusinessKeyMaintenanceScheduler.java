package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyMaintenanceAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 业务密钥维护定时任务（CR-055 F20 §6.4 步骤7）
 * <p>
 * 定期将超过 decrypt_until 的 DEPRECATED 业务密钥转 EXPIRED 并发布 EXPIRED 事件。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.business-key.maintenance.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessKeyMaintenanceScheduler {

    private final BusinessKeyMaintenanceAppService businessKeyMaintenanceAppService;

    @Value("${vmd.business-key.maintenance.scheduler.batch-size:200}")
    private int batchSize;

    @Value("${vmd.business-key.provisioning.ack-timeout-seconds:900}")
    private long ackTimeoutSeconds;

    @Value("${vmd.business-key.provisioning.reconcile-alert-seconds:86400}")
    private long reconcileAlertSeconds;

    /**
     * 每日凌晨 2 点扫批（可配置）
     */
    @Scheduled(cron = "${vmd.business-key.maintenance.scheduler.cron:0 0 2 * * ?}")
    public void expireDeprecatedKeys() {
        log.info("开始执行业务密钥转EXPIRED定时任务");
        try {
            int processed = businessKeyMaintenanceAppService.expireDeprecatedKeys(batchSize);
            if (processed > 0) {
                log.info("业务密钥转EXPIRED定时任务完成: processed={}", processed);
            }
        } catch (Exception e) {
            log.error("业务密钥转EXPIRED定时任务异常", e);
        }
    }

    /**
     * 周期扫描开通批次：WAIT_DEVICE_ACK 超时转 EXPIRED；RECONCILE_REQUIRED 超阈值告警（CR-058 §8）。
     * <p>两者使用不同 TTL，对账状态不直接过期；不生成伪成功事件。
     */
    @Scheduled(cron = "${vmd.business-key.maintenance.scheduler.provisioning-cron:0 */10 * * * ?}")
    public void scanProvisioningBatches() {
        try {
            java.time.LocalDateTime ackCutoff = java.time.LocalDateTime.now().minusSeconds(ackTimeoutSeconds);
            int expired = businessKeyMaintenanceAppService.expireStaleAckBatches(ackCutoff, batchSize);
            java.time.LocalDateTime reconcileCutoff = java.time.LocalDateTime.now().minusSeconds(reconcileAlertSeconds);
            int alerted = businessKeyMaintenanceAppService.alertStaleReconcileBatches(reconcileCutoff, batchSize);
            if (expired > 0 || alerted > 0) {
                log.info("业务密钥开通批次扫描完成: expired={}, reconcilesAlerted={}", expired, alerted);
            }
        } catch (Exception e) {
            log.error("业务密钥开通批次扫描异常", e);
        }
    }
}
