package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactNormalized;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.UserVehicleActivationFactEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationConsumeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactContractException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationFactConsumeStatus;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactKafkaConfig;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdConsumeType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * IOV-TSP 车辆首次激活候选事实 Kafka 消费者（VMD-DSN-CR-077 §6.1）
 * <p>
 * Topic {@code tsp.user-vehicle-activation-fact}（CONSUMER_ONLY，Key=VIN，schemaVersion=1）：
 * <ol>
 *   <li>解析 + 契约校验（严格必填 / 宽松未知字段 / {@code vin} ≤20 不截断）</li>
 *   <li>事件级去重（{@code eventId}）→ 业务级去重（{@code vin + bindingOperationId}）
 *       → 台账归档 → 投影条件 upsert（应用服务内同事务完成）</li>
 *   <li>契约错误 → {@code QUARANTINED}（不重试、不阻塞分区）</li>
 *   <li>技术错误 → 抛出让容器级 {@code CommonErrorHandler}（{@link VmdActivationFactKafkaConfig#ERROR_HANDLER_BEAN}
 *       装配在 {@link VmdActivationFactKafkaConfig#CONTAINER_FACTORY_BEAN} 上）退避（3 次 / 100ms→1s），
 *       耗尽后由 {@link net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactFailureRecovery}
 *       归档 {@code FAILED} 并提交位点（不建 DLQ，R-18 #6）</li>
 * </ol>
 * 纯终端消费者：不写 {@code vmd_outbox}、不发二次事件、不调 Feign（D-VMD-077-10）。
 * <p>
 * 容器 id 常量化（{@link VmdConsumeType.ConsumerIds#USER_VEHICLE_ACTIVATION_FACT}），
 * 由启动协调器按门禁启动；{@code auto-startup} 默认 false，Topic 缺失时不阻塞启动。
 * <p>
 * <strong>不声明 {@code errorHandler} 属性（BLK-VMD-1）</strong>：{@code @KafkaListener#errorHandler}
 * 只接受 {@link org.springframework.kafka.listener.KafkaListenerErrorHandler}，而本消费的退避/归档处理器
 * 是容器级 {@link org.springframework.kafka.listener.DefaultErrorHandler}（{@code CommonErrorHandler}），
 * 由 {@code containerFactory} 装配；误接会在启动期抛 {@code BeanNotOfRequiredTypeException}
 * 导致消费者完全不可用。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vmd.activation-fact.kafka", name = "enabled", havingValue = "true")
public class UserVehicleActivationFactKafkaConsumer {

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_DUPLICATE = "duplicate";
    private static final String RESULT_CORRECTED = "corrected";
    private static final String RESULT_QUARANTINED = "quarantined";
    private static final String RESULT_FAILED = "failed";

    private final VehicleActivationFactConsumeAppService consumeAppService;
    private final VmdActivationFactMetrics activationFactMetrics;
    private final ObjectMapper objectMapper;

    /**
     * 消费车辆首次激活候选事实
     *
     * @param record Kafka 消费者记录（Key=VIN）
     */
    @KafkaListener(
            id = VmdConsumeType.ConsumerIds.USER_VEHICLE_ACTIVATION_FACT,
            topics = {"${vmd.kafka.topics.user-vehicle-activation-fact:tsp.user-vehicle-activation-fact}"},
            groupId = "${vmd.activation-fact.kafka.group-id:edd-vmd-vehicle-activation-fact}",
            autoStartup = "${vmd.activation-fact.kafka.auto-startup:false}",
            containerFactory = VmdActivationFactKafkaConfig.CONTAINER_FACTORY_BEAN
    )
    public void onActivationFact(ConsumerRecord<String, String> record) {
        ActivationFactRecordMeta meta =
                new ActivationFactRecordMeta(record.topic(), record.partition(), record.offset());
        log.info("收到车辆首次激活候选事实: location={}, key={}", meta, record.key());

        UserVehicleActivationFactEvent raw;
        try {
            raw = parse(record.value());
        } catch (Exception e) {
            // 载荷不可解析：契约错误直接隔离，不重试（不毒化分区）
            consumeAppService.quarantine(null, meta, "载荷不可解析: " + e.getClass().getSimpleName(),
                    record.value());
            recordMetric(record.topic(), RESULT_QUARANTINED);
            return;
        }

        try {
            ActivationFactNormalized fact = ActivationFactNormalized.of(raw, record.key());
            // 生产端摘要为权威：本地复算仅作完整性校验，不一致只告警 + 计指标（不改变 status/幂等）
            if (fact.digestProvidedButMismatch()) {
                log.warn("激活事实摘要校验不一致（事件摘要为权威落库、不改变判定）: location={}, eventId={}, "
                                + "payloadDigest={}, localDigest={}",
                        meta, fact.eventId(), fact.payloadDigest(), fact.localDigest());
            }
            VehicleActivationConsumeResult result = consumeAppService.consume(fact, meta);
            recordResult(record.topic(), fact, result);
        } catch (ActivationFactContractException e) {
            // 契约错误 → 立即隔离（不抛异常、不重试、不阻塞分区）
            consumeAppService.quarantine(raw, meta, e.getMessage(), record.value());
            recordMetric(record.topic(), RESULT_QUARANTINED);
            log.warn("激活事实契约错误已隔离: location={}, reason={}", meta, e.getMessage());
        }
    }

    /**
     * 解析载荷（未知字段忽略，缺必填交给规范化阶段）
     *
     * @param payload 载荷 JSON
     * @return 事件
     * @throws Exception 解析失败
     */
    private UserVehicleActivationFactEvent parse(String payload) throws Exception {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("载荷为空");
        }
        return objectMapper.readValue(payload, UserVehicleActivationFactEvent.class);
    }

    /**
     * 记录消费结果与指标（摘要来源、未建档 VIN、摘要差异）
     * <p>
     * {@code digest_mismatch} 指标同时覆盖两类观测：
     * ① 业务键重投时台账摘要不同（{@code result.digestMismatch()}）；
     * ② 事件摘要与本地复算不一致（{@code fact.digestProvidedButMismatch()}，完整性校验）。
     * 两者均不影响终态与幂等，仅作数据质量观测。
     *
     * @param topic  Topic
     * @param fact   规范化事实
     * @param result 消费结论
     */
    private void recordResult(String topic, ActivationFactNormalized fact, VehicleActivationConsumeResult result) {
        activationFactMetrics.recordDigestSource(fact.digestSourceCode());
        if (result.digestMismatch() || fact.digestProvidedButMismatch()) {
            activationFactMetrics.recordDigestMismatch(topic);
        }
        if (result.vinUnresolved()) {
            activationFactMetrics.recordVinUnresolved(topic);
        }
        if (result.status() == ActivationFactConsumeStatus.RECORDED) {
            recordMetric(topic, RESULT_SUCCESS);
            if (result.corrected()) {
                recordMetric(topic, RESULT_CORRECTED);
            }
            return;
        }
        if (result.status() == ActivationFactConsumeStatus.DUPLICATE) {
            recordMetric(topic, RESULT_DUPLICATE);
            return;
        }
        if (result.status() == ActivationFactConsumeStatus.QUARANTINED) {
            recordMetric(topic, RESULT_QUARANTINED);
            return;
        }
        recordMetric(topic, RESULT_FAILED);
    }

    /**
     * 记录单一结果指标（指标组件不可用时不影响消费）
     *
     * @param topic  Topic
     * @param result 结果标签
     */
    private void recordMetric(String topic, String result) {
        try {
            activationFactMetrics.recordConsume(topic, result);
        } catch (Exception e) {
            log.debug("激活事实指标记录失败（不影响消费）: {}", e.getMessage());
        }
    }

}
