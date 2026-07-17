package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox Relay 组件
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 * <p>
 * 定时扫描 vmd_outbox 表中 PENDING/FAILED_RETRYABLE 状态的消息，
 * 发布到 Kafka 并更新发布状态。
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private final VmdOutboxRepository vmdOutboxRepository;
    private final VehImportEventReplayRepository vehImportEventReplayRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 每次扫描的最大消息数
     */
    @Value("${vmd.outbox.relay.batch-size:100}")
    private int batchSize;

    /**
     * 最大重试次数
     */
    @Value("${vmd.outbox.relay.max-retry:3}")
    private int maxRetry;

    /**
     * 重试间隔（秒）
     */
    @Value("${vmd.outbox.relay.retry-interval-seconds:60}")
    private int retryIntervalSeconds;

    /**
     * 定时扫描 Outbox 并发布到 Kafka
     * <p>
     * 每 10 秒执行一次
     */
    @Scheduled(fixedDelayString = "${vmd.outbox.relay.scan-interval-ms:10000}")
    public void relayMessages() {
        List<VmdOutbox> pendingMessages = vmdOutboxRepository.selectPendingMessages(batchSize);
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Outbox Relay 扫描到 {} 条待发布消息", pendingMessages.size());

        for (VmdOutbox outbox : pendingMessages) {
            try {
                publishMessage(outbox);
            } catch (Exception e) {
                log.error("Outbox Relay 发布消息失败: eventId={}, error={}", outbox.getEventId(), e.getMessage(), e);
                handlePublishFailure(outbox, e);
            }
        }
    }

    /**
     * 发布单条消息到 Kafka
     *
     * @param outbox Outbox 消息
     */
    private void publishMessage(VmdOutbox outbox) {
        log.debug("发布消息到 Kafka: eventId={}, topic={}, key={}", outbox.getEventId(), outbox.getTopic(), outbox.getMessageKey());

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                outbox.getTopic(),
                outbox.getMessageKey(),
                outbox.getPayload()
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("消息发布失败: eventId={}, error={}", outbox.getEventId(), ex.getMessage());
                handlePublishFailure(outbox, ex);
            } else {
                log.info("消息发布成功: eventId={}, topic={}, partition={}, offset={}",
                        outbox.getEventId(), outbox.getTopic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                handlePublishSuccess(outbox);
            }
        });
    }

    /**
     * 处理发布成功
     *
     * @param outbox Outbox 消息
     */
    private void handlePublishSuccess(VmdOutbox outbox) {
        outbox.setPublishState("PUBLISHED");
        outbox.setPublishedAt(LocalDateTime.now());
        outbox.setLastError(null);
        vmdOutboxRepository.update(outbox);

        // 检查是否所有关联消息都已发送成功，如果是则更新VehImportEventReplay状态
        checkAndUpdateReplayStatus(outbox);
    }

    /**
     * 检查并更新补发审计记录状态
     * <p>
     * 当Outbox消息发送成功后，检查该replayId下的所有消息是否都已发送成功，
     * 如果是，则更新VehImportEventReplay状态为SUCCESS
     *
     * @param outbox Outbox 消息
     */
    private void checkAndUpdateReplayStatus(VmdOutbox outbox) {
        String sourceRefId = outbox.getSourceRefId();
        if (StrUtil.isBlank(sourceRefId)) {
            return;
        }

        try {
            // 查询关联的补发审计记录
            VehImportEventReplay replay = vehImportEventReplayRepository.selectByReplayId(sourceRefId);
            if (replay == null) {
                log.warn("未找到关联的补发审计记录: sourceRefId={}", sourceRefId);
                return;
            }

            // 如果已经是终态，不再更新
            if ("SUCCESS".equals(replay.getStatus()) || "FAILED".equals(replay.getStatus())
                    || "PARTIAL_FAILED".equals(replay.getStatus())) {
                return;
            }

            // 统计该replayId下还有多少PENDING或FAILED_RETRYABLE的消息
            long pendingCount = vmdOutboxRepository.countBySourceRefIdAndPublishState(sourceRefId, "PENDING");
            long failedRetryableCount = vmdOutboxRepository.countBySourceRefIdAndPublishState(sourceRefId, "FAILED_RETRYABLE");
            long deadCount = vmdOutboxRepository.countBySourceRefIdAndPublishState(sourceRefId, "DEAD");

            // 如果还有待发送或可重试的消息，不更新状态
            if (pendingCount > 0 || failedRetryableCount > 0) {
                log.debug("补发任务[{}]还有{}条待发送、{}条可重试消息，暂不更新状态",
                        sourceRefId, pendingCount, failedRetryableCount);
                return;
            }

            // 所有消息都已发送完成，根据DEAD消息数量确定最终状态
            if (deadCount > 0) {
                replay.setStatus("PARTIAL_FAILED");
                replay.setFailureCount((int) deadCount);
                replay.setFailureDetail(deadCount + "条消息发送失败超过最大重试次数");
                log.warn("补发任务[{}]部分消息发送失败: deadCount={}", sourceRefId, deadCount);
            } else {
                replay.setStatus("SUCCESS");
                log.info("补发任务[{}]所有消息发送成功", sourceRefId);
            }
            replay.setFinishedAt(LocalDateTime.now());
            vehImportEventReplayRepository.update(replay);
        } catch (Exception e) {
            log.error("更新补发审计记录状态失败: sourceRefId={}, error={}", sourceRefId, e.getMessage(), e);
        }
    }

    /**
     * 处理发布失败
     *
     * @param outbox Outbox 消息
     * @param ex     异常
     */
    private void handlePublishFailure(VmdOutbox outbox, Throwable ex) {
        int retryCount = outbox.getRetryCount() != null ? outbox.getRetryCount() + 1 : 1;
        outbox.setRetryCount(retryCount);
        outbox.setLastError(truncateError(ex.getMessage()));

        if (retryCount >= maxRetry) {
            // 超过最大重试次数，标记为 DEAD
            outbox.setPublishState("DEAD");
            log.error("消息超过最大重试次数，标记为 DEAD: eventId={}, retryCount={}", outbox.getEventId(), retryCount);
        } else {
            // 设置为 FAILED_RETRYABLE，安排下次重试
            outbox.setPublishState("FAILED_RETRYABLE");
            outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(retryIntervalSeconds));
            log.warn("消息发布失败，将重试: eventId={}, retryCount={}, nextRetryTime={}",
                    outbox.getEventId(), retryCount, outbox.getNextRetryTime());
        }

        vmdOutboxRepository.update(outbox);
    }

    /**
     * 截断错误信息
     *
     * @param error 原始错误信息
     * @return 截断后的错误信息
     */
    private String truncateError(String error) {
        if (error == null) {
            return null;
        }
        int maxLength = 2000;
        if (error.length() <= maxLength) {
            return error;
        }
        return error.substring(0, maxLength - 3) + "...";
    }
}
