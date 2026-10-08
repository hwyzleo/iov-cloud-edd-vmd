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
}
