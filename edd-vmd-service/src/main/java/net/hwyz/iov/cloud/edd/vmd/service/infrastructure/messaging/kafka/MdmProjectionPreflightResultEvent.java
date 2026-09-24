package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import java.util.List;
import java.util.Map;

/**
 * MDM 消费 Topic 预检结果事件（VMD-DSN-CR-052 §5）
 * <p>
 * 预检完成后发布，供 {@link MdmConsumerStartupCoordinator} 等监听者
 * 按投影决定 Listener 启动顺序（配置校验 → Topic 预检 → Bootstrap 基线 → 增量消费）。
 *
 * @param specs     11 个 CONSUMER_ONLY 预检规格
 * @param results   按投影类型的预检结果
 * @param allPassed 是否全部通过
 * @author hwyz_leo
 */
public record MdmProjectionPreflightResultEvent(
        List<MdmConsumerTopicSpec> specs,
        Map<MdmProjectionType, MdmPreflightResult> results,
        boolean allPassed) {
}
