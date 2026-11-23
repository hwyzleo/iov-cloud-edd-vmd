package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

/**
 * 首次激活事实技术失败恢复器（VMD-DSN-CR-077 / D-VMD-077-07）
 * <p>
 * 退避重试耗尽后由 {@code DefaultErrorHandler} 回调：
 * <ol>
 *   <li>台账落 {@code FAILED}（含 {@code retry_count} 与脱敏 {@code last_error}）</li>
 *   <li>不回抛异常 → 位点被提交，<strong>不毒化分区</strong>；失败证据可按
 *       {@code topic/partition/offset} 重放恢复（不建 DLQ Topic，R-18 #6）</li>
 *   <li>台账写入本身失败时仅记 ERROR 日志，仍不阻塞分区（避免无限重试）</li>
 * </ol>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmdActivationFactFailureRecovery {

    private final VehicleActivationFactConsumeAppService consumeAppService;
    private final VmdActivationFactConsumerProperties consumerProperties;

    /**
     * 恢复回调：归档 FAILED 证据并确认位点
     *
     * @param record 失败的 Kafka 记录
     * @param ex     最后一次异常
     */
    public void recover(ConsumerRecord<?, ?> record, Exception ex) {
        String value = record.value() == null ? null : String.valueOf(record.value());
        ActivationFactRecordMeta meta = new ActivationFactRecordMeta(
                record.topic(), record.partition(), record.offset());
        int attempts = Math.max(1, consumerProperties.getRetry().getMaxAttempts());
        String error = ex == null ? "unknown" : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        try {
            consumeAppService.markFailed(value, meta, error, attempts - 1);
            log.error("激活事实退避重试耗尽，已落 FAILED 证据并提交位点（可按位点重放）: location={}, attempts={}",
                    meta, attempts);
        } catch (Exception persistError) {
            // 台账不可用：保留日志证据，绝不因归档失败再次阻塞分区
            log.error("激活事实 FAILED 证据落库失败（位点仍提交，避免毒化分区）: location={}, error={}",
                    meta, persistError.getMessage(), persistError);
        }
    }

    /**
     * 错误摘要截断（脱敏前的兜底长度控制）
     *
     * @param text 原文
     * @return 截断后文本
     */
    static String truncate(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        return text.length() <= 512 ? text : text.substring(0, 509) + "...";
    }

}
