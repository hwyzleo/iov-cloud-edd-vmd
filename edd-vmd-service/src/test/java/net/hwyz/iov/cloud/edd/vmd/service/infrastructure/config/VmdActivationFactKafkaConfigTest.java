package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationConsumeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.UserVehicleActivationFactKafkaConsumer;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactFailureRecovery;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdConsumeType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 激活事实消费错误处理装配测试（VMD-DSN-CR-077 / D-VMD-077-07 / BLK-VMD-1 回归）
 * <p>
 * 退化路径（必须在测试层固化的原因）：
 * <ul>
 *   <li>{@code @KafkaListener(errorHandler=...)} 只接受 {@link KafkaListenerErrorHandler}；
 *       误接容器级 {@link DefaultErrorHandler}（{@link CommonErrorHandler}）会在容器启动期抛
 *       {@code BeanNotOfRequiredTypeException}，消费容器整体不可用（S11-4/5/6 SKIP 的根因）</li>
 *   <li>正解：错误处理器经容器工厂 {@code setCommonErrorHandler} 装配，{@code @KafkaListener}
 *       不声明 {@code errorHandler}</li>
 *   <li>同时验证退避/归档语义未因装配位置调整而丢失：3 次投递（首次 + 2 次退避）后回调
 *       {@link VmdActivationFactFailureRecovery} 归档 {@code FAILED}，并提交位点（不毒化分区）</li>
 *   <li><strong>终审 R-8</strong>：容器工厂写入的消费属性 {@code auto-offset-reset} 默认为
 *       {@code earliest}（开启消费时回放历史事实，补齐早期已激活车辆的首次激活节点），
 *       且可经 {@link VmdActivationFactConsumerProperties#setAutoOffsetReset(String)} 切回 {@code latest}</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@DisplayName("VmdActivationFactKafkaConfig 错误处理装配测试")
class VmdActivationFactKafkaConfigTest {

    private static final String TOPIC = "tsp.user-vehicle-activation-fact";

    private final VmdActivationFactKafkaConfig config = new VmdActivationFactKafkaConfig();

    @Test
    @DisplayName("BLK-VMD-1：错误处理器 Bean 类型必须是 CommonErrorHandler，且不是 KafkaListenerErrorHandler")
    void errorHandlerBean_isCommonErrorHandler() throws Exception {
        Method beanMethod = VmdActivationFactKafkaConfig.class.getMethod(
                "vmdActivationFactErrorHandler",
                VmdActivationFactConsumerProperties.class,
                VmdActivationFactFailureRecovery.class);

        Class<?> returnType = beanMethod.getReturnType();
        assertTrue(CommonErrorHandler.class.isAssignableFrom(returnType),
                "错误处理器必须是容器级 CommonErrorHandler");
        assertFalse(KafkaListenerErrorHandler.class.isAssignableFrom(returnType),
                "错误处理器不得被当成 KafkaListenerErrorHandler 引用（BLK-VMD-1 根因）");
    }

    @Test
    @DisplayName("BLK-VMD-1：容器工厂按 CommonErrorHandler 形参接收错误处理器（装配点类型显式区分）")
    void containerFactory_acceptsCommonErrorHandler() throws Exception {
        Method factoryMethod = VmdActivationFactKafkaConfig.class.getMethod(
                "vmdActivationFactContainerFactory",
                KafkaProperties.class,
                VmdActivationFactConsumerProperties.class,
                CommonErrorHandler.class);

        Parameter[] parameters = factoryMethod.getParameters();
        assertSame(CommonErrorHandler.class, parameters[2].getType());
        assertFalse(KafkaListenerErrorHandler.class.isAssignableFrom(parameters[2].getType()),
                "容器工厂不得以 KafkaListenerErrorHandler 接收容器级错误处理器");
    }

    @Test
    @DisplayName("BLK-VMD-1：@KafkaListener 不声明 errorHandler，只绑定独立容器工厂")
    void listenerAnnotation_doesNotDeclareErrorHandler() throws Exception {
        Method listener = UserVehicleActivationFactKafkaConsumer.class
                .getMethod("onActivationFact", ConsumerRecord.class);
        KafkaListener annotation = listener.getAnnotation(KafkaListener.class);

        assertEquals("", annotation.errorHandler(),
                "errorHandler 属性只接受 KafkaListenerErrorHandler，容器级 DefaultErrorHandler 必须走容器工厂");
        assertEquals(VmdActivationFactKafkaConfig.CONTAINER_FACTORY_BEAN, annotation.containerFactory());
        assertEquals(VmdConsumeType.ConsumerIds.USER_VEHICLE_ACTIVATION_FACT, annotation.id());
        assertEquals("${vmd.kafka.topics.user-vehicle-activation-fact:tsp.user-vehicle-activation-fact}",
                annotation.topics()[0]);
    }

    @Test
    @DisplayName("容器工厂确实安装了退避 + 归档错误处理器，且终态提交位点（不毒化分区）")
    void containerFactory_installsErrorHandler() {
        VmdActivationFactConsumerProperties properties = new VmdActivationFactConsumerProperties();
        VmdActivationFactFailureRecovery recovery = new VmdActivationFactFailureRecovery(
                mock(VehicleActivationFactConsumeAppService.class), properties);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                config.vmdActivationFactContainerFactory(new KafkaProperties(), properties,
                        config.vmdActivationFactErrorHandler(properties, recovery));

        Object installed = ReflectionTestUtils.getField(factory, "commonErrorHandler");
        DefaultErrorHandler errorHandler = assertInstanceOf(DefaultErrorHandler.class, installed);
        assertTrue(errorHandler.isAckAfterHandle(), "终态归档后必须提交位点，避免分区被毒化");
        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(errorHandler, "commitRecovered"),
                "恢复成功的记录必须提交位点");
    }

    @Test
    @DisplayName("终审 R-8：容器工厂写入 auto-offset-reset=earliest（默认），且可经配置切回 latest")
    void containerFactory_autoOffsetResetDefaultsToEarliest() {
        VmdActivationFactConsumerProperties properties = new VmdActivationFactConsumerProperties();

        assertEquals("earliest", consumerProps(properties).get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
                "开启消费前必须回放历史事实（earliest），否则早期已激活车辆缺首次激活节点");

        properties.setAutoOffsetReset("latest");
        assertEquals("latest", consumerProps(properties).get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
                "回放完成后应可经配置切回 latest（仅消费增量）");
    }

    @Test
    @DisplayName("TC-077-09：3 次投递（首次 + 2 次退避）后归档 FAILED 并返回已处理（不阻塞分区）")
    void errorHandler_retriesThenRecovers() {
        VmdActivationFactConsumerProperties properties = new VmdActivationFactConsumerProperties();
        VehicleActivationFactConsumeAppService consumeAppService =
                mock(VehicleActivationFactConsumeAppService.class);
        when(consumeAppService.markFailed(anyString(), any(ActivationFactRecordMeta.class), anyString(), anyInt()))
                .thenReturn(VehicleActivationConsumeResult.failed());
        VmdActivationFactFailureRecovery recovery =
                new VmdActivationFactFailureRecovery(consumeAppService, properties);
        DefaultErrorHandler errorHandler = config.vmdActivationFactErrorHandler(properties, recovery);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 3, 77L, "VIN", "{}");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        IllegalStateException failure = new IllegalStateException("db down");

        // 首次 + 2 次退避重试：均未进入恢复
        assertFalse(assertDoesNotThrow(
                () -> errorHandler.handleOne(failure, record, consumer, container)), "第 1 次投递应退避重试");
        assertFalse(assertDoesNotThrow(
                () -> errorHandler.handleOne(failure, record, consumer, container)), "第 2 次投递应退避重试");
        // 第 3 次投递耗尽 → 归档 FAILED 并返回 true（容器继续提交位点，不毒化分区）
        assertTrue(assertDoesNotThrow(
                () -> errorHandler.handleOne(failure, record, consumer, container)), "退避耗尽后必须归档并继续");

        verify(consumeAppService, times(1))
                .markFailed(anyString(), any(ActivationFactRecordMeta.class), anyString(), anyInt());
    }

    /**
     * 构建容器工厂并取出其消费者工厂实际生效的属性（不启动容器）。
     *
     * @param properties 激活事实消费配置
     * @return 消费者属性
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> consumerProps(VmdActivationFactConsumerProperties properties) {
        VmdActivationFactFailureRecovery recovery = new VmdActivationFactFailureRecovery(
                mock(VehicleActivationFactConsumeAppService.class), properties);
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                config.vmdActivationFactContainerFactory(new KafkaProperties(), properties,
                        config.vmdActivationFactErrorHandler(properties, recovery));
        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                (DefaultKafkaConsumerFactory<String, String>) factory.getConsumerFactory();
        return consumerFactory.getConfigurationProperties();
    }

}
