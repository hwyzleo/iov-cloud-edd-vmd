package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.MdmConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MDM 消费 Listener 启动协调器单元测试（VMD-DSN-CR-052 §4）
 * <p>
 * 验证 auto-startup 默认不拦截、预检通过 / 未执行 → 启动全部容器、
 * 预检失败且 fail-fast → 拒绝启动、降级模式 → 启动但健康保持不可用。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MdmConsumerStartupCoordinator 测试")
class MdmConsumerStartupCoordinatorTest {

    @Mock
    private KafkaListenerEndpointRegistry registry;

    private MdmConsumerProperties properties;
    private MdmProjectionReadiness readiness;
    private VmdActivationFactConsumerProperties activationFactProperties;
    private VmdActivationFactTopicReadiness activationFactReadiness;
    private MdmConsumerStartupCoordinator coordinator;

    @BeforeEach
    void setUp() {
        properties = new MdmConsumerProperties();
        readiness = new MdmProjectionReadiness();
        activationFactProperties = new VmdActivationFactConsumerProperties();
        activationFactReadiness = new VmdActivationFactTopicReadiness();
        coordinator = new MdmConsumerStartupCoordinator(registry, properties, readiness,
                activationFactProperties, activationFactReadiness);
    }

    private MessageListenerContainer stoppedContainer() {
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        when(container.isRunning()).thenReturn(false);
        return container;
    }

    /**
     * 注册 BRAND / PART 两个停止态容器，其余投影容器为 null（未装配/未注册）。
     */
    private void stubContainersStopped() {
        when(registry.getListenerContainer(anyString())).thenReturn(null);
        MessageListenerContainer brand = stoppedContainer();
        when(registry.getListenerContainer(MdmProjectionType.ConsumerIds.BRAND)).thenReturn(brand);
        MessageListenerContainer part = stoppedContainer();
        when(registry.getListenerContainer(MdmProjectionType.ConsumerIds.PART)).thenReturn(part);
    }

    @Test
    @DisplayName("auto-startup=true（默认）：协调器不拦截，不启动容器")
    void autoStartupEnabled_doesNotStart() {
        properties.setAutoStartup(true);
        readiness.markPreflightPassed(MdmProjectionType.BRAND, "mdm.brand");
        coordinator.onPreflightResult(new MdmProjectionPreflightResultEvent(Collections.emptyList(),
                Map.of(), true));
        verify(registry, never()).getListenerContainer(anyString());
    }

    @Test
    @DisplayName("auto-startup=false 且预检通过：启动全部已注册容器")
    void gated_preflightPassed_starts() {
        properties.setAutoStartup(false);
        for (MdmProjectionType p : MdmProjectionType.values()) {
            readiness.markPreflightPassed(p, p.directoryTopic());
        }
        stubContainersStopped();
        coordinator.onPreflightResult(new MdmProjectionPreflightResultEvent(Collections.emptyList(),
                Map.of(), true));

        verify(registry).getListenerContainer(MdmProjectionType.ConsumerIds.BRAND);
        verify(registry).getListenerContainer(MdmProjectionType.ConsumerIds.PART);
        assertTrue(readiness.status(MdmProjectionType.BRAND).consumerRunning());
        assertTrue(readiness.status(MdmProjectionType.PART).consumerRunning());
    }

    @Test
    @DisplayName("auto-startup=false 且预检失败 + fail-fast：拒绝启动")
    void gated_preflightFailed_failFast_doesNotStart() {
        properties.setAutoStartup(false);
        readiness.markPreflightFailed(MdmProjectionType.BRAND, "mdm.brand", "missing");
        for (MdmProjectionType p : MdmProjectionType.values()) {
            if (p != MdmProjectionType.BRAND) {
                readiness.markPreflightPassed(p, p.directoryTopic());
            }
        }
        coordinator.onPreflightResult(new MdmProjectionPreflightResultEvent(Collections.emptyList(),
                Map.of(), false));
        verify(registry, never()).getListenerContainer(MdmProjectionType.ConsumerIds.BRAND);
    }

    @Test
    @DisplayName("auto-startup=false 且预检失败但降级启动：启动容器")
    void gated_preflightFailed_degraded_starts() {
        properties.setAutoStartup(false);
        properties.setFailFast(false);
        readiness.markPreflightFailed(MdmProjectionType.BRAND, "mdm.brand", "missing");
        stubContainersStopped();
        coordinator.onPreflightResult(new MdmProjectionPreflightResultEvent(Collections.emptyList(),
                Map.of(), false));
        verify(registry).getListenerContainer(MdmProjectionType.ConsumerIds.BRAND);
    }

    @Test
    @DisplayName("auto-startup=false 且预检从未执行（INIT）：按既有行为启动（非托管环境回归）")
    void gated_allInit_startsLegacy() {
        properties.setAutoStartup(false);
        stubContainersStopped();
        coordinator.onApplicationReady(null);
        verify(registry).getListenerContainer(MdmProjectionType.ConsumerIds.BRAND);
    }

    @Test
    @DisplayName("CR-077：激活事实消费启用 + 门禁关闭 → Topic 缺失也启动容器（不阻塞启动）")
    void activationFact_topicMissing_stillStarts() {
        properties.setAutoStartup(true);
        activationFactProperties.setEnabled(true);
        activationFactProperties.setStartupGateEnabled(false);
        activationFactReadiness.markNotPresent("上游 Topic 暂不存在");
        activationFactReadiness.markUp(6);
        MessageListenerContainer container = stoppedContainer();
        when(registry.getListenerContainer(VmdConsumeType.ConsumerIds.USER_VEHICLE_ACTIVATION_FACT))
                .thenReturn(container);

        coordinator.onActivationFactTopicProbe(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.NOT_PRESENT, 0));

        verify(container).start();
        assertTrue(activationFactReadiness.consumerRunning());
    }

    @Test
    @DisplayName("CR-077：门禁开启且 Topic 未就绪 → 暂不启动；探针就绪后自恢复启动")
    void activationFact_gated_waitsThenStarts() {
        properties.setAutoStartup(true);
        activationFactProperties.setEnabled(true);
        activationFactProperties.setStartupGateEnabled(true);
        MessageListenerContainer container = stoppedContainer();
        when(registry.getListenerContainer(VmdConsumeType.ConsumerIds.USER_VEHICLE_ACTIVATION_FACT))
                .thenReturn(container);

        activationFactReadiness.markNotPresent("上游 Topic 暂不存在");
        coordinator.onActivationFactTopicProbe(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.NOT_PRESENT, 0));
        verify(container, never()).start();

        activationFactReadiness.markUp(6);
        coordinator.onActivationFactTopicProbe(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.UP, 6));
        verify(container).start();
        assertTrue(activationFactReadiness.consumerRunning());
    }

    @Test
    @DisplayName("CR-077：消费未启用 → 不触碰容器（默认暗部署关闭）")
    void activationFact_disabled_noContainerLookup() {
        properties.setAutoStartup(true);
        activationFactProperties.setEnabled(false);
        coordinator.onActivationFactTopicProbe(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.UP, 6));
        verify(registry, never()).getListenerContainer(anyString());
    }

    @Test
    @DisplayName("仅注册容器启动一次，未注册投影跳过")
    void startAll_skipsUnregistered() {
        properties.setAutoStartup(false);
        for (MdmProjectionType p : MdmProjectionType.values()) {
            readiness.markPreflightPassed(p, p.directoryTopic());
        }
        when(registry.getListenerContainer(anyString())).thenReturn(null);
        MessageListenerContainer brand = stoppedContainer();
        when(registry.getListenerContainer(MdmProjectionType.ConsumerIds.BRAND)).thenReturn(brand);
        coordinator.onPreflightResult(new MdmProjectionPreflightResultEvent(Collections.emptyList(),
                Map.of(), true));
        verify(registry).getListenerContainer(MdmProjectionType.ConsumerIds.BRAND);
        // PART 未注册，不启动；BRAND 启动一次
        assertTrue(readiness.status(MdmProjectionType.BRAND).consumerRunning());
        assertFalse(readiness.status(MdmProjectionType.PART).consumerRunning());
    }
}
