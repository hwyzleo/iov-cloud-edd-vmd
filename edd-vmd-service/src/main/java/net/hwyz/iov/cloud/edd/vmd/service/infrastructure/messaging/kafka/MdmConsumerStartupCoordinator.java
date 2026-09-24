package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.MdmConsumerProperties;
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

    @EventListener
    public void onPreflightResult(MdmProjectionPreflightResultEvent event) {
        evaluate();
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // 兜底：预检事件与 Listener 注册顺序不确定时，应用就绪后再次评估
        evaluate();
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
}
