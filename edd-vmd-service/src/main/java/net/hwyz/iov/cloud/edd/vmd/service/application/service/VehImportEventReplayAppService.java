package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayEventResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEventEnvelope;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceEventReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayInProgressException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 车辆导入成功事件补发应用服务
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 * <p>
 * 负责资格校验、VIN 识别、当前车辆快照读取、Kafka 事件构造、Outbox 入队、并发控制和审计。
 * <p>
 * 注意：
 * - 严禁通过 VehiclePublish.produce() / ApplicationEventPublisher 重放进程内事件
 * - 不调用 ImportDataParserRegistry / VehicleProduceParser
 * - 不重入 D15 六步内核，不重新建档/更新车辆、写选项值快照、绑定零件或触发安全常量预置
 * - API 返回的 queuedCount 表示"已成功写入 Outbox"，实际 Kafka 投递状态由 Outbox 跟踪
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehImportEventReplayAppService {

    private final VehImportDataRepository vehImportDataRepository;
    private final VehImportEventReplayRepository vehImportEventReplayRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VmdOutboxRepository vmdOutboxRepository;
    private final ProduceEventReplayExtractor produceEventReplayExtractor;

    /**
     * failure_detail 字段最大长度
     */
    private static final int FAILURE_DETAIL_MAX_LENGTH = 2000;

    /**
     * RUNNING状态超时时间（分钟）
     */
    @Value("${vmd.replay.running-timeout-minutes:30}")
    private int runningTimeoutMinutes;

    /**
     * Kafka Topic
     */
    private static final String KAFKA_TOPIC = "vmd.vehicle.produce.event";

    /**
     * 事件类型
     */
    private static final String EVENT_TYPE = "VehicleProduceEvent";

    /**
     * 聚合类型
     */
    private static final String AGGREGATE_TYPE = "VEHICLE";

    /**
     * 来源类型
     */
    private static final String SOURCE_TYPE = "IMPORT_EVENT_REPLAY";

    /**
     * 补发车辆导入成功事件
     *
     * @param id         车辆导入数据ID
     * @param requestId  请求ID（可选，作为replayId）
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param reason     补发原因
     * @return 补发结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ReplayEventResult replay(Long id, String requestId, String operatorId, String operatorName, String reason) {
        log.info("开始补发车辆导入成功事件, id={}, operatorId={}", id, operatorId);

        // 1. 获取并校验车辆导入数据
        VehImportData vehImportData = vehImportDataRepository.selectById(id);
        validateVehImportData(vehImportData, id);

        // 2. 生成或使用提供的replayId
        String replayId = StrUtil.isNotBlank(requestId) ? requestId : UUID.randomUUID().toString();

        // 3. 检查是否有执行中的任务
        long runningCount = vehImportEventReplayRepository.countRunningByVehImportDataIdAndEventType(
                vehImportData.getId(), vehImportData.getType());
        if (runningCount > 0) {
            throw new VehicleImportEventReplayInProgressException(id);
        }

        // 4. 创建审计记录
        VehImportEventReplay replay = VehImportEventReplay.builder()
                .replayId(replayId)
                .vehImportDataId(vehImportData.getId())
                .batchNum(vehImportData.getBatchNum())
                .eventType(vehImportData.getType())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .reason(reason)
                .status("RUNNING")
                .totalCount(0)
                .queuedCount(0)
                .failureCount(0)
                .startedAt(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
        vehImportEventReplayRepository.insert(replay);

        // 5. 提取VIN列表
        List<String> vins = produceEventReplayExtractor.extractDistinctVins(vehImportData.getData());
        replay.setTotalCount(vins.size());

        if (vins.isEmpty()) {
            log.warn("车辆导入数据[id={}]未提取到有效VIN", id);
            replay.setStatus("FAILED");
            replay.setFailureDetail("未提取到有效VIN");
            replay.setFinishedAt(LocalDateTime.now());
            vehImportEventReplayRepository.update(replay);
            return buildResult(replay);
        }

        // 6. 逐个VIN构造事件并写入Outbox
        int queuedCount = 0;
        int failureCount = 0;
        List<String> failureDetails = new ArrayList<>();

        for (String vin : vins) {
            try {
                // 读取当前车辆完整快照
                VehicleBasicInfo vehicleInfo = vehBasicInfoRepository.selectByVin(vin);
                if (ObjUtil.isNull(vehicleInfo)) {
                    failureCount++;
                    failureDetails.add(vin + ": 车辆不存在");
                    log.warn("补发车辆[{}]生产事件失败: 车辆不存在", vin);
                    continue;
                }

                // 构造事件payload
                VehicleProduceEventEnvelope.VehicleProducePayload payload = VehicleProduceEventEnvelope.VehicleProducePayload.builder()
                        .vin(vin)
                        .produceTime(LocalDateTime.now())
                        .plantCode(vehicleInfo.getPlantCode())
                        .brandCode(vehicleInfo.getBrandCode())
                        .platformCode(vehicleInfo.getPlatformCode())
                        .carLineCode(vehicleInfo.getCarLineCode())
                        .modelCode(vehicleInfo.getModelCode())
                        .variantCode(vehicleInfo.getVariantCode())
                        .configurationCode(vehicleInfo.getConfigurationCode())
                        .orderNum(vehicleInfo.getOrderNum())
                        .build();

                // 构造事件信封
                VehicleProduceEventEnvelope envelope = VehicleProduceEventEnvelope.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(EVENT_TYPE)
                        .aggregateType(AGGREGATE_TYPE)
                        .aggregateId(vin)
                        .version(System.currentTimeMillis())
                        .occurredAt(LocalDateTime.now())
                        .producer("vmd-replay")
                        .payload(payload)
                        // 补发扩展字段
                        .replay(true)
                        .batchNum(vehImportData.getBatchNum())
                        .replayId(replayId)
                        .replayOperator(operatorId)
                        .replayedAt(LocalDateTime.now())
                        .build();

                // 写入Outbox
                VmdOutbox outbox = VmdOutbox.builder()
                        .eventId(envelope.getEventId())
                        .eventType(EVENT_TYPE)
                        .aggregateType(AGGREGATE_TYPE)
                        .aggregateId(vin)
                        .aggregateVersion(envelope.getVersion())
                        .topic(KAFKA_TOPIC)
                        .messageKey(vin)
                        .payload(JSONUtil.toJsonStr(envelope))
                        .publishState("PENDING")
                        .retryCount(0)
                        .sourceType(SOURCE_TYPE)
                        .sourceRefId(replayId)
                        .createTime(LocalDateTime.now())
                        .build();
                vmdOutboxRepository.insert(outbox);

                queuedCount++;
                log.debug("补发车辆[{}]生产事件写入Outbox成功", vin);
            } catch (Exception e) {
                failureCount++;
                String detail = vin + ": " + e.getMessage();
                failureDetails.add(detail);
                log.warn("补发车辆[{}]生产事件写入Outbox失败: {}", vin, e.getMessage());
            }
        }

        // 7. 更新审计记录
        replay.setQueuedCount(queuedCount);
        replay.setFailureCount(failureCount);
        replay.setStatus(determineStatus(queuedCount, failureCount));
        if (!failureDetails.isEmpty()) {
            replay.setFailureDetail(truncateFailureDetail(String.join("; ", failureDetails)));
        }
        replay.setFinishedAt(LocalDateTime.now());
        vehImportEventReplayRepository.update(replay);

        log.info("补发车辆导入成功事件完成, id={}, total={}, queued={}, failure={}",
                id, replay.getTotalCount(), queuedCount, failureCount);

        return buildResult(replay);
    }

    /**
     * 定时清理超时的RUNNING状态记录
     * <p>
     * 每5分钟执行一次，将超过配置时间的RUNNING状态记录更新为FAILED
     */
    @Scheduled(fixedDelayString = "${vmd.replay.timeout-check-interval-ms:300000}")
    public void cleanupTimeoutRunningRecords() {
        log.debug("开始检查超时的RUNNING状态补发记录, timeoutMinutes={}", runningTimeoutMinutes);
        int updatedCount = vehImportEventReplayRepository.updateTimeoutRunningToFailed(runningTimeoutMinutes);
        if (updatedCount > 0) {
            log.warn("已将{}条超时的RUNNING状态补发记录更新为FAILED, timeoutMinutes={}", updatedCount, runningTimeoutMinutes);
        }
    }

    /**
     * 校验车辆导入数据是否允许补发
     *
     * @param vehImportData 车辆导入数据
     * @param id            车辆导入数据ID
     */
    private void validateVehImportData(VehImportData vehImportData, Long id) {
        if (ObjUtil.isNull(vehImportData)) {
            throw new VehicleImportEventReplayNotAllowedException("车辆导入记录不存在");
        }
        // 仅支持已成功的 PRODUCE 记录
        if (!"PRODUCE".equals(vehImportData.getType())) {
            throw new VehicleImportEventReplayNotAllowedException("仅支持PRODUCE类型的导入记录");
        }
        if (!Boolean.TRUE.equals(vehImportData.getHandle())) {
            throw new VehicleImportEventReplayNotAllowedException("导入记录未处理成功");
        }
        if (StrUtil.isBlank(vehImportData.getData())) {
            throw new VehicleImportEventReplayNotAllowedException("原始数据不存在");
        }
    }

    /**
     * 根据入队/失败数量确定状态
     *
     * @param queuedCount 入队数
     * @param failureCount 失败数
     * @return 状态
     */
    private String determineStatus(int queuedCount, int failureCount) {
        if (failureCount == 0) {
            return "QUEUED";
        } else if (queuedCount == 0) {
            return "FAILED";
        } else {
            return "PARTIAL_FAILED";
        }
    }

    /**
     * 截断失败详情到列长限制
     *
     * @param detail 原始详情
     * @return 截断后的详情
     */
    private String truncateFailureDetail(String detail) {
        if (detail == null) {
            return null;
        }
        if (detail.length() <= FAILURE_DETAIL_MAX_LENGTH) {
            return detail;
        }
        return detail.substring(0, FAILURE_DETAIL_MAX_LENGTH - 3) + "...";
    }

    /**
     * 构建返回结果
     *
     * @param replay 审计记录
     * @return 补发结果
     */
    private ReplayEventResult buildResult(VehImportEventReplay replay) {
        List<String> failures = new ArrayList<>();
        if (StrUtil.isNotBlank(replay.getFailureDetail())) {
            // 解析失败详情
            String[] parts = replay.getFailureDetail().split("; ");
            for (String part : parts) {
                if (StrUtil.isNotBlank(part)) {
                    failures.add(part);
                }
            }
        }
        return ReplayEventResult.builder()
                .replayId(replay.getReplayId())
                .totalCount(replay.getTotalCount())
                .queuedCount(replay.getQueuedCount())
                .failureCount(replay.getFailureCount())
                .failures(failures)
                .build();
    }
}
