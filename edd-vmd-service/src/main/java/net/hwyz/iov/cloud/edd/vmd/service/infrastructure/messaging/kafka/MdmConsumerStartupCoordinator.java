package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.MdmConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * MDM 消费 Listener 启动协调器（VMD-DSN-CR-052 §4）
 * <p>
 * 当 {@code vmd.kafka.mdm-consumer.auto-startup=false} 时，MDM 消费 Listener
 * 的启动顺序由预检 Readiness 控制：
 * <ol>
 *   <li>预检全部通过（且 Bootstrap 基线条件满足）→ 启动全部 MDM Listener</li>
 *   <li>预检失败且 fail-fast=true → 拒绝启动（健康保持 DOWN，后台持续重试预检）</li>
 *   <li>预检失败但允许降级启动（fail-fast=false）→ 启动但健康保持 DOWN/DEGRADED</li>
 * </ol>
 * auto-startup 默认 true（保持 Spring 自动启动行为）；本协调器仅在显式开启门禁时生效。
 * 预检从未执行（如 Topic Provisioning 未启用）时按既有行为启动，避免非托管环境回归。
 * <p>
 * VMD-DSN-CR-077：新增 VMD 自有消费容器分支（{@link VmdConsumeType}）。与 MDM 分支的差异：
 * <ul>
 *   <li>不纳入 MDM Bootstrap 基线判定，只参考自身 Topic 预检</li>
 *   <li>Topic 缺失（{@code NOT_PRESENT}）<strong>不阻塞启动</strong>：默认（门禁关闭）直接启动容器，
 *       由 Kafka 客户端在 Topic 就绪后接续消费；门禁开启时延迟启动，探针就绪后自动启动</li>
 *   <li>启动失败（容器异常）只记录日志，绝不阻断应用启动</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmConsumerStartupCoordinator {

    private final KafkaListenerEndpointRegistry registry;
    private final MdmConsumerProperties consumerProperties;
    private final MdmProjectionReadiness readiness;
    private final VmdActivationFactConsumerProperties activationFactProperties;
    private final VmdActivationFactTopicReadiness activationFactReadiness;

    @EventListener
    public void onPreflightResult(MdmProjectionPreflightResultEvent event) {
        evaluate();
    }

    @EventListener
    public void onActivationFactTopicProbe(VmdActivationFactTopicProbeEvent event) {
        // 探针就绪（或状态变化）后评估 VMD 自有容器启动，实现「Topic 就绪后自恢复」
        evaluateVmdConsumers();
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // 兜底：预检事件与 Listener 注册顺序不确定时，应用就绪后再次评估
        evaluate();
        evaluateVmdConsumers();
    }

    private void evaluate() {
        if (consumerProperties.isAutoStartup()) {
            return;
        }
        if (readiness.allReady() || readiness.allInit()) {
            startAll("预检通过（或未启用预检），启动 MDM 消费 Listener");
            return;
        }
        if (!consumerProperties.isFailFast()) {
            startAll("预检失败但允许降级启动，启动 MDM 消费 Listener（健康保持 DOWN/DEGRADED）");
            return;
        }
        log.warn("MDM 消费 Topic 预检未通过且 fail-fast=true，拒绝启动 MDM 消费 Listener；"
                + "后台持续重试预检，通过后自动启动");
    }

    private void startAll(String reason) {
        log.info("{}", reason);
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            MessageListenerContainer container = registry.getListenerContainer(projection.consumerId());
            if (container == null) {
                // 该投影消费者未装配（enabled=false）或未注册
                continue;
            }
            if (!container.isRunning()) {
                container.start();
            }
            readiness.markConsumerRunning(projection, true);
        }
    }

    /**
     * 评估并启动 VMD 自有消费容器（CR-077）。
     * <p>任何异常只记日志，绝不阻断应用启动（Topic 缺失属上游未就绪，不是 VMD 故障）。
     */
    private void evaluateVmdConsumers() {
        if (!activationFactProperties.isEnabled()) {
            return;
        }
        try {
            if (activationFactProperties.isStartupGateEnabled()
                    && activationFactReadiness.state() != VmdActivationFactTopicReadiness.State.UP) {
                log.warn("首次激活事实 Topic 未就绪（state={}, reason={}），门禁开启暂不启动消费容器；"
                                + "探针就绪后自动启动（不阻塞应用启动）",
                        activationFactReadiness.state(), activationFactReadiness.reason());
                return;
            }
            for (VmdConsumeType consumeType : VmdConsumeType.values()) {
                MessageListenerContainer container = registry.getListenerContainer(consumeType.consumerId());
                if (container == null) {
                    // 消费者未装配（enabled=false）或未注册
                    continue;
                }
                if (!container.isRunning()) {
                    container.start();
                }
                activationFactReadiness.markConsumerRunning(true);
                log.info("VMD 自有消费容器已启动: consumeType={}, consumerId={}, topic={}, probeState={}",
                        consumeType.name(), consumeType.consumerId(),
                        consumeType.directoryTopic(), activationFactReadiness.state());
            }
        } catch (Exception e) {
            log.error("VMD 自有消费容器启动失败（不阻塞应用，探针/后台重试将再次尝试）: {}", e.getMessage(), e);
        }
    }
}
