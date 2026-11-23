package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactBackOff;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactFailureRecovery;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 车辆首次激活事实消费基础设施配置（VMD-DSN-CR-077 / D-VMD-077-07 / D-VMD-077-12）
 * <p>
 * 独立容器工厂的目的：
 * <ul>
 *   <li>新消费组起始位点默认 {@code auto-offset-reset=earliest}（<strong>终审 R-8</strong> 改定；原 R-18 #5 的 {@code latest}），
 *       与既有组位点语义隔离；{@code earliest} 仅对新消费组首次分配分区生效，
 *       已有 committed offset 不受影响（不重复回放）；开启前用 {@code earliest} 回放历史事实，
 *       回放完成后（lag 归零、台账无新增 {@code RECORDED}）如需可切回 {@code latest} 仅消费增量</li>
 *   <li>错误处理固定「退避 3 次（100ms→1s）→ 台账 FAILED → 提交位点」，
 *       不影响既有 MDM / OTA 消费者的错误处理策略</li>
 * </ul>
 * 不建 DLQ Topic（R-18 #6）：失败证据由台账 {@code FAILED/QUARANTINED} +
 * {@code topic/partition/offset} 承载，按位点人工重放。
 * <p>
 * <strong>错误处理装配位置（BLK-VMD-1 修复）</strong>：
 * {@link DefaultErrorHandler} 是<em>容器级</em> {@link CommonErrorHandler}，只能通过
 * {@code ConcurrentKafkaListenerContainerFactory#setCommonErrorHandler} 装配，
 * <strong>不能</strong>作为 {@code @KafkaListener(errorHandler=...)} 的引用目标
 * ——该属性只接受 {@link org.springframework.kafka.listener.KafkaListenerErrorHandler}，
 * 类型不符会在容器启动期抛 {@code BeanNotOfRequiredTypeException} 并导致消费者完全不可用。
 * 因此 {@code @KafkaListener} 不再声明 {@code errorHandler}，退避/归档语义完全由本类
 * 装配到独立容器工厂的 {@code commonErrorHandler} 承担（见
 * {@code VmdActivationFactKafkaConfigTest} 的回归断言）。
 *
 * @author hwyz_leo
 */
@Configuration
@ConditionalOnProperty(prefix = "vmd.activation-fact.kafka", name = "enabled", havingValue = "true")
public class VmdActivationFactKafkaConfig {

    /**
     * 容器级错误处理器 Bean 名称（{@link CommonErrorHandler} 语义）。
     * <p>
     * <strong>注意</strong>：该 Bean 只能由 {@link #CONTAINER_FACTORY_BEAN} 通过
     * {@code setCommonErrorHandler(...)} 使用，<strong>禁止</strong>被
     * {@code @KafkaListener(errorHandler=...)} 引用（后者要求
     * {@link org.springframework.kafka.listener.KafkaListenerErrorHandler}，类型不兼容 → BLK-VMD-1）。
     */
    public static final String ERROR_HANDLER_BEAN = "vmdActivationFactErrorHandler";

    /**
     * 容器工厂 Bean 名称
     */
    public static final String CONTAINER_FACTORY_BEAN = "vmdActivationFactContainerFactory";

    /**
     * 独立消费容器工厂（auto-offset-reset 由本消费类型配置决定，默认 {@code earliest}，
     * 终审 R-8：开启前回放历史事实；仅新消费组首次生效，已有位点不受影响）
     *
     * @param kafkaProperties 框架 Kafka 连接配置
     * @param properties      激活事实消费配置
     * @param errorHandler    退避 + 终态归档的<em>容器级</em>错误处理器
     * @return 容器工厂
     */
    @Bean(CONTAINER_FACTORY_BEAN)
    public ConcurrentKafkaListenerContainerFactory<String, String> vmdActivationFactContainerFactory(
            KafkaProperties kafkaProperties,
            VmdActivationFactConsumerProperties properties,
            @Qualifier(ERROR_HANDLER_BEAN) CommonErrorHandler errorHandler) {
        Map<String, Object> consumerProps = new HashMap<>(kafkaProperties.buildConsumerProperties());
        // 终审 R-8：默认 earliest → 开启消费时回放 Topic 中既有事实，补齐早期已激活车辆的首次激活节点；
        // 该值只在新消费组首次分配分区时被 Kafka 采用，已有 committed offset 的组不受影响。
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getAutoOffsetReset());

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps,
                        new StringDeserializer(), new StringDeserializer());
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * 退避 + 终态归档的容器级错误处理器（{@link CommonErrorHandler}）：
     * 技术错误重试 max-attempts 次后落 FAILED 并提交位点。
     * <p>
     * 声明类型保持 {@link DefaultErrorHandler}（Bean 自身类型），但只经
     * {@link #vmdActivationFactContainerFactory} 的 {@code setCommonErrorHandler} 生效；
     * 装配点的形参类型故意声明为 {@link CommonErrorHandler}，与 {@code @KafkaListener(errorHandler=...)}
     * 所需的 {@code KafkaListenerErrorHandler} 显式区分，避免再次误接（BLK-VMD-1）。
     *
     * @param properties 激活事实消费配置
     * @param recovery   失败恢复器
     * @return 错误处理器
     */
    @Bean(ERROR_HANDLER_BEAN)
    public DefaultErrorHandler vmdActivationFactErrorHandler(
            VmdActivationFactConsumerProperties properties,
            VmdActivationFactFailureRecovery recovery) {
        VmdActivationFactConsumerProperties.Retry retry = properties.getRetry();
        int maxAttempts = Math.max(1, retry.getMaxAttempts());
        VmdActivationFactBackOff backOff = new VmdActivationFactBackOff(
                maxAttempts, retry.getInitialIntervalMs(), retry.getMultiplier(), retry.getMaxIntervalMs());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recovery::recover, backOff);
        // 终态归档后位点照常提交：不毒化分区，失败证据由台账 + topic/partition/offset 承载（可重放）
        errorHandler.setAckAfterHandle(true);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }

}
