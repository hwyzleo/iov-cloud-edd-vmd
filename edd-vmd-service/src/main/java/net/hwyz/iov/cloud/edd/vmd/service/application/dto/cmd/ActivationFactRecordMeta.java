package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

/**
 * 激活事实来源位点（VMD-DSN-CR-077 §6.3）
 * <p>
 * 隔离与失败证据必须保留原始 {@code topic/partition/offset}，用于按位点人工重放
 * （不建 DLQ Topic，R-18 #6）；原始载荷不落库，避免体积与合规风险。
 *
 * @param topic     来源 Topic
 * @param partition 来源分区
 * @param offset    来源位点
 * @author hwyz_leo
 */
public record ActivationFactRecordMeta(String topic, int partition, long offset) {

    @Override
    public String toString() {
        return topic + "-" + partition + "@" + offset;
    }

}
