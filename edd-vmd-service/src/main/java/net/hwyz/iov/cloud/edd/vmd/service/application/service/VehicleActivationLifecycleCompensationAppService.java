package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.VehicleActivationLifecycleWriter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 首次激活事实 → 生命周期补偿应用服务（VMD-DSN-CR-079 / D079-05、D079-06）
 * <p>
 * 覆盖两类补偿，均复用同一幂等算法（{@link VehicleActivationLifecycleWriter}），因此增量与存量口径一致：
 * <ol>
 *   <li><strong>建档钩子</strong>：车辆建档成功后立即补偿该 VIN 未解析的激活投影</li>
 *   <li><strong>有界定时对账</strong>：分页扫描 {@code vin_resolved=0} 且主档已存在的投影（覆盖漏钩子/历史）</li>
 *   <li><strong>一次性存量对齐</strong>：分页扫描 {@code vin_resolved=1} 投影，把既有投影对齐到生命周期</li>
 * </ol>
 * 边界：不自动建车、不改其它生命周期节点、不写 {@code vmd_outbox}、不发二次事件、不回调 TSP；
 * 补偿失败只记指标并留待下一轮，不阻断建档主事务与既有消费链路（D079-05 / D079-07）。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Service
public class VehicleActivationLifecycleCompensationAppService {

    /** 无需补偿：该 VIN 没有激活投影 */
    public static final String RESULT_SKIPPED = "SKIPPED";
    /** VIN 仍未建档：保留投影与 vin_resolved=0，等待下一轮 */
    public static final String RESULT_PENDING_VIN = "PENDING_VIN";
    /** 补偿技术失败：记录指标，留待下一轮 */
    public static final String RESULT_FAILED = "FAILED";

    private static final String DESCRIPTION_LIFECYCLE_CORRECTED = "LIFECYCLE_CORRECTED";
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final VehicleActivationNodeRepository activationNodeRepository;
    private final VehicleActivationFactConsumeRepository activationFactConsumeRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleActivationLifecycleWriter lifecycleWriter;
    private final VmdActivationFactMetrics activationFactMetrics;
    private final VmdKafkaTopicProperties topicProperties;
    private final TransactionTemplate requiresNewTransaction;

    public VehicleActivationLifecycleCompensationAppService(
            VehicleActivationNodeRepository activationNodeRepository,
            VehicleActivationFactConsumeRepository activationFactConsumeRepository,
            VehBasicInfoRepository vehBasicInfoRepository,
            VehicleActivationLifecycleWriter lifecycleWriter,
            VmdActivationFactMetrics activationFactMetrics,
            VmdKafkaTopicProperties topicProperties,
            PlatformTransactionManager transactionManager) {
        this.activationNodeRepository = activationNodeRepository;
        this.activationFactConsumeRepository = activationFactConsumeRepository;
        this.vehBasicInfoRepository = vehBasicInfoRepository;
        this.lifecycleWriter = lifecycleWriter;
        this.activationFactMetrics = activationFactMetrics;
        this.topicProperties = topicProperties;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 单个 VIN 生命周期对齐（独立短事务，幂等）
     * <p>
     * 已建档且存在激活投影时写/收敛 {@code VEHICLE_ACTIVE}，并把投影 {@code vin_resolved} 置 1；
     * 主档缺失只记 {@code PENDING_VIN} 并保留 {@code vin_resolved=0}，绝不自动建车。
     *
     * @param vin 车架号
     * @return 补偿结论
     */
    public CompensationOutcome compensate(String vin) {
        if (StrUtil.isBlank(vin)) {
            return new CompensationOutcome(RESULT_SKIPPED, null, null);
        }
        CompensationOutcome outcome = requiresNewTransaction.execute(status -> align(vin));
        return outcome == null ? new CompensationOutcome(RESULT_SKIPPED, null, null) : outcome;
    }

    /**
     * 单个 VIN 补偿（吞掉技术异常，仅记录指标与告警）
     * <p>
     * 用于建档后置钩子与定时对账：失败不得影响建档主流程，也不得中断同批其它 VIN。
     *
     * @param vin 车架号
     */
    public void compensateQuietly(String vin) {
        try {
            CompensationOutcome outcome = compensate(vin);
            log.debug("首次激活生命周期补偿完成: vin={}, result={}", maskVin(vin), outcome.result());
        } catch (Exception e) {
            activationFactMetrics.recordLifecycle(topic(), RESULT_FAILED);
            log.warn("首次激活生命周期补偿失败（留待下一轮）: vin={}, error={}",
                    maskVin(vin), e.getMessage(), e);
        }
    }

    /**
     * 有界定时对账：分页扫描未建档标记投影，主档已存在者立即补偿
     *
     * @param batchSize  单页大小
     * @param maxBatches 单轮最大页数（防御性上限）
     * @return 本轮实际处理条数
     */
    public int reconcileUnresolved(int batchSize, int maxBatches) {
        return scanByVinResolved(0, batchSize, maxBatches, true);
    }

    /**
     * 一次性存量对齐：分页扫描已建档投影，幂等对齐生命周期（D079-06）
     * <p>
     * 既有 {@code VEHICLE_ACTIVE} 更早时间优先保留；只有可证明的激活事实更早时才向前纠正。
     *
     * @param batchSize  单页大小
     * @param maxBatches 单轮最大页数（防御性上限）
     * @return 本轮实际处理条数
     */
    public int alignResolved(int batchSize, int maxBatches) {
        return scanByVinResolved(1, batchSize, maxBatches, false);
    }

    /**
     * 统计未建档待补偿投影数量（backlog 指标）
     *
     * @return 数量
     */
    public long countUnresolved() {
        try {
            return activationNodeRepository.countByVinResolved(0);
        } catch (Exception e) {
            log.debug("统计未建档激活投影数量失败（不影响补偿）: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 事务内单 VIN 对齐（由 {@link #compensate(String)} 的事务模板调用）
     *
     * @param vin 车架号
     * @return 补偿结论
     */
    private CompensationOutcome align(String vin) {
        VehicleActivationNode node = activationNodeRepository.selectByVin(vin);
        if (node == null) {
            return new CompensationOutcome(RESULT_SKIPPED, null, null);
        }
        if (vehBasicInfoRepository.selectByVin(vin) == null) {
            // 未建档：保留投影与 vin_resolved=0，不写无车辆锚点的生命周期
            activationFactMetrics.recordLifecycle(topic(), RESULT_PENDING_VIN);
            return new CompensationOutcome(RESULT_PENDING_VIN, null, null);
        }
        LocalDateTime earliest = node.getFirstActivatedAt() != null
                ? node.getFirstActivatedAt() : node.getOccurredAt();
        if (earliest == null) {
            log.warn("激活投影缺少首次激活时刻，跳过生命周期补偿: vin={}", maskVin(vin));
            return new CompensationOutcome(RESULT_SKIPPED, null, null);
        }

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome writeOutcome =
                lifecycleWriter.write(vin, earliest);
        activationFactMetrics.recordLifecycle(topic(), writeOutcome.result().name());

        if (node.getVinResolved() == null || node.getVinResolved() != 1) {
            activationNodeRepository.updateVinResolved(node.getId(), 1);
        }
        if (writeOutcome.corrected()) {
            appendCorrectionEvidence(node, writeOutcome);
        }
        log.info("首次激活生命周期补偿已生效: vin={}, result={}, previous={}, current={}",
                maskVin(vin), writeOutcome.result(), writeOutcome.previousReachTime(),
                writeOutcome.currentReachTime());
        return new CompensationOutcome(writeOutcome.result().name(),
                writeOutcome.previousReachTime(), writeOutcome.currentReachTime());
    }

    /**
     * 分页扫描并逐条补偿（单条失败不影响其余与后续轮次）
     *
     * @param vinResolved 建档标记
     * @param batchSize   单页大小
     * @param maxBatches  单轮最大页数
     * @param onlyResolvedMatches 是否只处理主档已存在的投影
     * @return 本轮处理条数
     */
    private int scanByVinResolved(int vinResolved, int batchSize, int maxBatches,
                                  boolean onlyResolvedMatches) {
        int effectiveBatch = batchSize <= 0 ? 200 : batchSize;
        int effectiveBatches = maxBatches <= 0 ? 20 : maxBatches;
        int processed = 0;
        Long cursor = null;
        for (int round = 0; round < effectiveBatches; round++) {
            List<VehicleActivationNode> page =
                    activationNodeRepository.selectPageByVinResolved(vinResolved, cursor, effectiveBatch);
            if (page.isEmpty()) {
                break;
            }
            for (VehicleActivationNode node : page) {
                cursor = node.getId();
                if (onlyResolvedMatches
                        && vehBasicInfoRepository.selectByVin(node.getVin()) == null) {
                    // 定时对账只处理可匹配主档的数据；无匹配继续保留，不创建主档
                    continue;
                }
                compensateQuietly(node.getVin());
                processed++;
            }
            if (page.size() < effectiveBatch) {
                break;
            }
        }
        return processed;
    }

    /**
     * 在消费台账记录生命周期向前纠正证据（D079-02）
     * <p>
     * 证据只写事件 ID 与纠正前后时间摘要，不写完整来源原文与 VIN。
     *
     * @param node    激活投影
     * @param outcome 写入结论
     */
    private void appendCorrectionEvidence(VehicleActivationNode node,
                                          VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome) {
        VehicleActivationFactConsume consume = activationFactConsumeRepository.selectByBusinessKey(
                node.getVin(), node.getBindingOperationId());
        if (consume == null) {
            log.debug("未找到对应消费台账，生命周期纠正证据仅记录于投影: vin={}", maskVin(node.getVin()));
            return;
        }
        String evidence = DESCRIPTION_LIFECYCLE_CORRECTED
                + ":event=" + node.getEventId()
                + ":old=" + outcome.previousReachTime()
                + ":new=" + outcome.currentReachTime();
        consume.setDescription(appendDescription(consume.getDescription(), evidence));
        consume.setModifyTime(LocalDateTime.now(ZoneOffset.UTC));
        activationFactConsumeRepository.update(consume);
    }

    /**
     * 激活事实 Topic（指标标签，避免使用 VIN/eventId 作标签）
     *
     * @return Topic 名称
     */
    private String topic() {
        return topicProperties.getUserVehicleActivationFact();
    }

    /**
     * 备注追加（去重、限长）
     *
     * @param existing 现有备注
     * @param append   追加备注
     * @return 合并后备注
     */
    private static String appendDescription(String existing, String append) {
        if (StrUtil.isBlank(append)) {
            return existing;
        }
        if (StrUtil.isBlank(existing)) {
            return truncate(append);
        }
        if (existing.contains(append)) {
            return existing;
        }
        return truncate(existing + "," + append);
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= DESCRIPTION_MAX_LENGTH
                ? text : text.substring(0, DESCRIPTION_MAX_LENGTH - 3) + "...";
    }

    /**
     * VIN 脱敏（保留前 3 后 4）
     *
     * @param vin 车架号
     * @return 脱敏后的 VIN
     */
    private static String maskVin(String vin) {
        if (StrUtil.isBlank(vin) || vin.length() <= 7) {
            return "****";
        }
        return vin.substring(0, 3) + "****" + vin.substring(vin.length() - 4);
    }

    /**
     * 单 VIN 补偿结论
     *
     * @param result            结果分类（APPLIED/DUPLICATE/CORRECTED/PENDING_VIN/SKIPPED）
     * @param previousReachTime 纠正前触达时间（可空）
     * @param currentReachTime  写入后触达时间（可空）
     */
    public record CompensationOutcome(String result, Instant previousReachTime, Instant currentReachTime) {
    }
}
