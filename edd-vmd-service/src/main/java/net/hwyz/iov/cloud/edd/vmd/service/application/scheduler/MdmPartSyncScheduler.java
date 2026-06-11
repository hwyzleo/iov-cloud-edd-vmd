package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MDM Part定时同步任务
 * <p>
 * 定期调用MDM全量快照接口同步Part数据，作为Kafka事件同步的补充和兜底。
 * 复用现有MdmSyncAppService.bootstrapPart()逻辑。
 * </p>
 *
 * @author CR-024
 * @see MdmSyncAppService#bootstrapPart()
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.sync.part.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MdmPartSyncScheduler {

    private final MdmSyncAppService mdmSyncAppService;
    private final MdmSyncMetrics mdmSyncMetrics;

    /**
     * 定时同步MDM Part数据
     * <p>
     * 按配置的cron表达式执行，默认每小时执行一次。
     * 基于externalRefId和externalVersion进行幂等upsert，不删除本地已有记录。
     * </p>
     */
    @Scheduled(cron = "${mdm.sync.part.scheduler.cron:0 0 */1 * * ?}")
    public void syncPartData() {
        log.info("开始执行MDM Part定时同步任务");
        long startTime = System.currentTimeMillis();

        try {
            // 复用现有bootstrapPart()逻辑
            mdmSyncAppService.bootstrapPart();

            // 记录成功指标
            mdmSyncMetrics.recordSuccess();
            log.info("MDM Part定时同步任务执行成功");

        } catch (Exception e) {
            // 记录失败指标
            mdmSyncMetrics.recordFailure();
            log.error("MDM Part定时同步任务执行失败: {}", e.getMessage(), e);

            // TODO: 发送告警通知（邮件、短信、钉钉等）
            // 当前实现：记录错误日志和失败指标

        } finally {
            // 记录执行耗时
            long duration = System.currentTimeMillis() - startTime;
            mdmSyncMetrics.recordDuration(duration);
            log.info("MDM Part定时同步任务完成，耗时: {}ms", duration);
        }
    }

    /**
     * 手动触发同步
     * <p>
     * 供运维人员通过API手动触发同步，用于数据修复或紧急同步场景。
     * </p>
     */
    public void manualSync() {
        log.info("手动触发MDM Part同步");
        syncPartData();
    }
}
