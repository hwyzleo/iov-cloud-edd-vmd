package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayVehicleImportEventCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayActionPreview;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayEventResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportReplayPreview;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayAction;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionTarget;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayActionNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayInProgressException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayMasterStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 车辆导入事件补发应用服务
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 * VMD-DSN-CR-057: 扩展为按 ImportType 路由的动作补偿编排器
 * <p>
 * 负责资格校验、preview 预检、replayId 幂等、动作编排（VehicleImportReplayActionRegistry）、
 * 主任务 + 逐项动作审计、并发控制和结果汇总（D22 / F14）。
 * <p>
 * 注意：
 * - 严禁通过 VehiclePublish.produce() / ApplicationEventPublisher 重放进程内事件
 * - 不调用 ImportDataParserRegistry / 具体 Parser / D15 六步内核
 * - 不重入建档/更新车辆、写选项值快照、绑定零件或触发证书/密钥/安全根/安全常量预置
 * - 原始 veh_import_data.data 仅负责候选集识别；payload 从当前事实构造
 * - 事件动作以 Outbox 入队和动作状态更新同事务提交；单动作失败不得回滚其他动作
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
    private final VehImportEventReplayItemRepository itemRepository;
    private final VehImportReplayExtractor replayExtractor;
    private final VehicleImportReplayActionRegistry actionRegistry;
    private final VmdOutboxRepository vmdOutboxRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * failure_detail 字段最大长度
     */
    private static final int FAILURE_DETAIL_MAX_LENGTH = 2000;

    /**
     * failure_reason 字段最大长度
     */
    private static final int FAILURE_REASON_MAX_LENGTH = 900;

    /**
     * RUNNING状态超时时间（分钟）
     */
    @Value("${vmd.replay.running-timeout-minutes:30}")
    private int runningTimeoutMinutes;

    /**
     * 支持的导入类型
     */
    private static final List<String> SUPPORTED_IMPORT_TYPES = List.of("PRODUCE", "TOL", "EOL");

    /**
     * 车辆导入补发预检
     *
     * @param id 车辆导入数据ID
     * @return 预检结果
     */
    public VehicleImportReplayPreview preview(Long id) {
        VehImportData importData = vehImportDataRepository.selectById(id);
        validateImportData(importData, id);
        String importType = importData.getType();
        List<VehImportReplayExtractor.VehCandidate> candidates =
                replayExtractor.extractCandidates(importData.getData(), importType);
        if (candidates.isEmpty()) {
            throw new VehicleImportEventReplayNotAllowedException("无法从原导入记录识别候选车辆");
        }

        List<ReplayActionPreview> actionPreviews = new ArrayList<>();
        for (VehicleImportReplayAction action : actionRegistry.actionsForImportType(importType)) {
            int eligibleCount = 0;
            int skipCount = 0;
            String reason = null;
            for (VehImportReplayExtractor.VehCandidate candidate : candidates) {
                VehicleImportReplayActionContext context = buildContext(null, importData, candidate, null, null, null);
                List<VehicleImportReplayActionTarget> targets = action.plan(context);
                if (targets.isEmpty()) {
                    skipCount++;
                    if (reason == null) {
                        reason = defaultSkipReason(action.actionType());
                    }
                } else {
                    eligibleCount += targets.size();
                }
            }
            actionPreviews.add(ReplayActionPreview.builder()
                    .actionType(action.actionType())
                    .actionName(label(action.actionType()))
                    .eligibleCount(eligibleCount)
                    .skipCount(skipCount)
                    .reason(reason)
                    .build());
        }

        return VehicleImportReplayPreview.builder()
                .importType(importType)
                .batchNum(importData.getBatchNum())
                .candidateVinCount(candidates.size())
                .actions(actionPreviews)
                .build();
    }

    /**
     * 执行车辆导入事件补发（按 ImportType 路由动作补偿）
     *
     * @param id           车辆导入数据ID
     * @param request      补发请求
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @return 补发结果
     */
    public ReplayEventResult replay(Long id, ReplayVehicleImportEventCmd request,
                                    String operatorId, String operatorName) {
        log.info("开始车辆导入事件补发, id={}, operatorId={}", id, operatorId);
        LocalDateTime now = LocalDateTime.now();

        // 1. 加载并校验原导入记录
        VehImportData importData = vehImportDataRepository.selectById(id);
        validateImportData(importData, id);
        String importType = importData.getType();
        List<VehImportReplayExtractor.VehCandidate> candidates =
                replayExtractor.extractCandidates(importData.getData(), importType);
        if (candidates.isEmpty()) {
            throw new VehicleImportEventReplayNotAllowedException("无法从原导入记录识别候选车辆");
        }

        // 2. 解析动作范围（空 = 全部适用动作；未知/未登记动作抛 806081）
        List<VehicleImportReplayAction> actions = resolveActionScope(request.getActionTypes(), importType);

        // 3. replayId 幂等：已存在且终态直接返回既有结果；RUNNING 拒绝
        String replayId = StrUtil.isNotBlank(request.getRequestId())
                ? request.getRequestId() : UUID.randomUUID().toString();
        VehImportEventReplay existing = vehImportEventReplayRepository.selectByReplayId(replayId);
        if (existing != null) {
            if (VehicleImportReplayMasterStatus.RUNNING.getValue().equals(existing.getStatus())) {
                throw new VehicleImportEventReplayInProgressException(id);
            }
            log.info("replayId[{}]已存在且已终态，返回既有补发结果", replayId);
            return buildResult(existing);
        }

        // 4. 执行中互斥（同一导入记录同时仅允许一个 RUNNING 主任务）
        if (vehImportEventReplayRepository.countRunningByVehImportDataId(id) > 0) {
            throw new VehicleImportEventReplayInProgressException(id);
        }

        // 5. 创建主任务（短事务）
        VehImportEventReplay master = VehImportEventReplay.builder()
                .replayId(replayId)
                .vehImportDataId(importData.getId())
                .batchNum(importData.getBatchNum())
                .importType(importType)
                .requestedActions(actions.stream()
                        .map(VehicleImportReplayAction::actionType).collect(Collectors.joining(",")))
                .operatorId(operatorId)
                .operatorName(operatorName)
                .reason(request.getReason())
                .status(VehicleImportReplayMasterStatus.RUNNING.getValue())
                .totalCount(0)
                .queuedCount(0)
                .successCount(0)
                .skipCount(0)
                .failureCount(0)
                .startedAt(now)
                .createTime(now)
                .build();
        final VehImportEventReplay created = master;
        transactionTemplate.executeWithoutResult(tx -> vehImportEventReplayRepository.insert(created));
        master = created;

        // 6. 逐候选车辆 × 动作执行（动作顺序由注册表保证：生命周期 → 绑定 → 软件实装）
        int plannedCount = 0;
        int queuedCount = 0;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> failures = new ArrayList<>();
        Map<String, int[]> actionCounts = new LinkedHashMap<>(); // actionType -> [planned, queued, success, skip, failure]

        for (VehImportReplayExtractor.VehCandidate candidate : candidates) {
            for (VehicleImportReplayAction action : actions) {
                VehicleImportReplayActionContext context = buildContext(
                        replayId, importData, candidate, operatorId, operatorName, request.getReason());
                List<VehicleImportReplayActionTarget> targets = action.plan(context);
                if (targets.isEmpty()) {
                    continue;
                }
                int[] counts = actionCounts.computeIfAbsent(action.actionType(), k -> new int[5]);
                for (VehicleImportReplayActionTarget target : targets) {
                    int outcome = executeAction(replayId, action, context, target, now, failures);
                    if (outcome < 0) {
                        // 同请求逐项幂等命中：已处理过，不重复计数
                        continue;
                    }
                    plannedCount++;
                    counts[0]++;
                    switch (outcome) {
                        case 0 -> {
                            queuedCount++;
                            counts[1]++;
                        }
                        case 1 -> {
                            successCount++;
                            counts[2]++;
                        }
                        case 2 -> {
                            skippedCount++;
                            counts[3]++;
                        }
                        default -> {
                            failureCount++;
                            counts[4]++;
                        }
                    }
                }
            }
        }

        // 7. 聚合主任务（短事务）
        master.setTotalCount(plannedCount);
        master.setQueuedCount(queuedCount);
        master.setSuccessCount(successCount);
        master.setSkipCount(skippedCount);
        master.setFailureCount(failureCount);
        master.setStatus(determineMasterStatus(queuedCount, successCount, skippedCount, failureCount));
        if (!failures.isEmpty()) {
            master.setFailureDetail(truncateFailureDetail(String.join("; ", failures)));
        }
        master.setFinishedAt(LocalDateTime.now());
        final VehImportEventReplay finalMaster = master;
        transactionTemplate.executeWithoutResult(tx -> vehImportEventReplayRepository.update(finalMaster));

        log.info("车辆导入事件补发完成, id={}, replayId={}, importType={}, planned={}, queued={}, success={}, skipped={}, failure={}",
                id, replayId, importType, plannedCount, queuedCount, successCount, skippedCount, failureCount);

        return buildResult(master);
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
     * 执行单个动作目标（短事务；返回 0=QUEUED 1=SUCCESS 2=SKIPPED 3=FAILED，-1=逐项幂等命中已处理）
     */
    private int executeAction(String replayId, VehicleImportReplayAction action,
                              VehicleImportReplayActionContext context, VehicleImportReplayActionTarget target,
                              LocalDateTime now, List<String> failures) {
        String actionType = action.actionType();

        // 逐项幂等：同请求 (replay_id, action_type, aggregate_type, aggregate_id, aggregate_version) 已处理过则跳过
        VehImportEventReplayItem existingItem = itemRepository.selectByUniqueKey(
                replayId, actionType, target.aggregateType(), target.aggregateId(), target.aggregateVersion());
        if (existingItem != null) {
            log.debug("逐项动作已处理，跳过重复执行: replayId={}, actionType={}, aggregate={}:{}@{}",
                    replayId, actionType, target.aggregateType(), target.aggregateId(), target.aggregateVersion());
            return -1;
        }

        VehImportEventReplayItem item = VehImportEventReplayItem.builder()
                .replayId(replayId)
                .actionType(actionType)
                .aggregateType(target.aggregateType())
                .aggregateId(target.aggregateId())
                .aggregateVersion(target.aggregateVersion())
                .sourceRecordId(target.sourceRecordId())
                .status(VehicleImportReplayActionStatus.PENDING.getValue())
                .startedAt(now)
                .createTime(now)
                .build();

        try {
            // 标记执行中（短事务插入）
            item.setStatus(VehicleImportReplayActionStatus.RUNNING.getValue());
            persistItem(item);

            VehicleImportReplayActionResult result = action.execute(context, target);
            switch (result.getOutcome()) {
                case QUEUED -> {
                    VmdOutbox outbox = result.getOutbox();
                    final VehImportEventReplayItem queuedItem = item;
                    transactionTemplate.executeWithoutResult(tx -> {
                        if (outbox != null) {
                            vmdOutboxRepository.insert(outbox);
                        }
                        queuedItem.setStatus(VehicleImportReplayActionStatus.QUEUED.getValue());
                        queuedItem.setEventId(result.getEventId());
                        queuedItem.setCompletedAt(LocalDateTime.now());
                        persistItemInTx(queuedItem);
                    });
                    return 0;
                }
                case SUCCESS -> {
                    item.setStatus(VehicleImportReplayActionStatus.SUCCESS.getValue());
                    item.setCompletedAt(LocalDateTime.now());
                    persistItem(item);
                    return 1;
                }
                case SKIPPED -> {
                    item.setStatus(VehicleImportReplayActionStatus.SKIPPED.getValue());
                    item.setSkipReason(result.getSkipReason());
                    item.setCompletedAt(LocalDateTime.now());
                    persistItem(item);
                    return 2;
                }
                default -> {
                    item.setStatus(VehicleImportReplayActionStatus.FAILED_FINAL.getValue());
                    item.setFailureReason(truncateFailureReason(
                            (result.getErrorCode() != null ? result.getErrorCode() + " " : "") + result.getErrorMessage()));
                    item.setCompletedAt(LocalDateTime.now());
                    persistItem(item);
                    failures.add(describeFailure(context.getVin(), actionType, result.getErrorCode(), result.getErrorMessage()));
                    return 3;
                }
            }
        } catch (DuplicateKeyException ex) {
            // 并发竞态：同键动作已被其他请求写入，视为已处理
            log.warn("逐项动作并发冲突，视为已处理: replayId={}, actionType={}, aggregate={}:{}@{}",
                    replayId, actionType, target.aggregateType(), target.aggregateId(), target.aggregateVersion());
            return -1;
        } catch (Exception e) {
            item.setStatus(VehicleImportReplayActionStatus.FAILED_RETRYABLE.getValue());
            item.setFailureReason(truncateFailureReason(e.getMessage()));
            item.setCompletedAt(LocalDateTime.now());
            persistItem(item);
            failures.add(describeFailure(context.getVin(), actionType, null, e.getMessage()));
            return 3;
        }
    }

    /**
     * 校验车辆导入数据是否允许补发（PRODUCE/TOL/EOL + 处理成功 + 原始数据存在）
     */
    private void validateImportData(VehImportData importData, Long id) {
        if (ObjUtil.isNull(importData)) {
            throw new VehicleImportEventReplayNotAllowedException("车辆导入记录不存在");
        }
        if (!SUPPORTED_IMPORT_TYPES.contains(importData.getType())) {
            throw new VehicleImportEventReplayNotAllowedException("仅支持PRODUCE/TOL/EOL类型的导入记录");
        }
        if (!Boolean.TRUE.equals(importData.getHandle())) {
            throw new VehicleImportEventReplayNotAllowedException("导入记录未处理成功");
        }
        if (StrUtil.isBlank(importData.getData())) {
            throw new VehicleImportEventReplayNotAllowedException("原始数据不存在");
        }
    }

    /**
     * 解析动作范围（空 = 该导入类型全部适用动作；未知或未登记动作抛 806081）
     */
    private List<VehicleImportReplayAction> resolveActionScope(List<String> actionTypes, String importType) {
        List<VehicleImportReplayAction> result = new ArrayList<>();
        if (actionTypes == null || actionTypes.isEmpty()) {
            return actionRegistry.actionsForImportType(importType);
        }
        for (String actionTypeValue : actionTypes) {
            VehicleImportReplayActionType type = VehicleImportReplayActionType.valOf(actionTypeValue);
            if (type == null || !type.applicableTo(importType)) {
                throw new VehicleImportEventReplayActionNotFoundException(actionTypeValue);
            }
            VehicleImportReplayAction action = actionRegistry.getAction(type.getValue());
            if (action == null) {
                throw new VehicleImportEventReplayActionNotFoundException(type.getValue());
            }
            result.add(action);
        }
        return result;
    }

    /**
     * 构建单个候选车辆的动作执行上下文
     */
    private VehicleImportReplayActionContext buildContext(String replayId, VehImportData importData,
                                                          VehImportReplayExtractor.VehCandidate candidate,
                                                          String operatorId, String operatorName, String reason) {
        return VehicleImportReplayActionContext.builder()
                .replayId(replayId)
                .vehImportDataId(importData.getId())
                .batchNum(importData.getBatchNum())
                .importType(importData.getType())
                .vin(candidate.vin())
                .candidates(candidate.parts())
                .batchTime(candidate.eolTime())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .reason(reason)
                .build();
    }

    /**
     * 根据动作结果统计确定主任务状态
     */
    private String determineMasterStatus(int queued, int success, int skipped, int failure) {
        if (failure > 0) {
            return (queued + success + skipped > 0)
                    ? VehicleImportReplayMasterStatus.PARTIAL_FAILED.getValue()
                    : VehicleImportReplayMasterStatus.FAILED.getValue();
        }
        if (skipped > 0) {
            return VehicleImportReplayMasterStatus.SUCCEEDED_WITH_SKIPS.getValue();
        }
        if (queued > 0) {
            return VehicleImportReplayMasterStatus.QUEUED.getValue();
        }
        return VehicleImportReplayMasterStatus.SUCCESS.getValue();
    }

    /**
     * 短事务持久化动作明细
     */
    private void persistItem(VehImportEventReplayItem item) {
        transactionTemplate.executeWithoutResult(tx -> persistItemInTx(item));
    }

    /**
     * 短事务持久化动作明细（事务内调用）
     */
    private void persistItemInTx(VehImportEventReplayItem item) {
        if (item.getId() == null) {
            itemRepository.insert(item);
        } else {
            itemRepository.update(item);
        }
    }

    /**
     * 动作标签
     */
    private String label(String actionType) {
        VehicleImportReplayActionType type = VehicleImportReplayActionType.valOf(actionType);
        return type != null ? type.getLabel() : actionType;
    }

    /**
     * 预检默认跳过原因
     */
    private String defaultSkipReason(String actionType) {
        VehicleImportReplayActionType type = VehicleImportReplayActionType.valOf(actionType);
        if (type == null) {
            return "动作未登记";
        }
        return switch (type) {
            case PRODUCE_EVENT -> "当前无候选VIN";
            case TOL_LIFECYCLE_ENSURE, EOL_LIFECYCLE_ENSURE -> "当前无候选车辆";
            case BINDING_EVENT_REPLAY -> "候选范围内无当前active绑定";
            case SOFTWARE_INVENTORY_EVENT_REPLAY -> "候选范围内无当前ACTIVE实装记录";
        };
    }

    /**
     * 构建返回结果（分动作结果由逐项审计按 actionType 聚合）
     */
    private ReplayEventResult buildResult(VehImportEventReplay replay) {
        List<VehImportEventReplayItem> items = itemRepository.selectListByReplayId(replay.getReplayId());
        Map<String, int[]> actionCounts = new LinkedHashMap<>(); // actionType -> [total, queued, success, skip, failure]
        for (VehImportEventReplayItem item : items) {
            int[] counts = actionCounts.computeIfAbsent(item.getActionType(), k -> new int[5]);
            counts[0]++;
            switch (item.getStatus()) {
                case "QUEUED" -> counts[1]++;
                case "SUCCESS" -> counts[2]++;
                case "SKIPPED" -> counts[3]++;
                case "FAILED_RETRYABLE", "FAILED_FINAL" -> counts[4]++;
                default -> {
                }
            }
        }
        List<ReplayActionResult> actionResults = actionCounts.entrySet().stream()
                .map(e -> ReplayActionResult.builder()
                        .actionType(e.getKey())
                        .totalCount(e.getValue()[0])
                        .queuedCount(e.getValue()[1])
                        .successCount(e.getValue()[2])
                        .skipCount(e.getValue()[3])
                        .failureCount(e.getValue()[4])
                        .build())
                .collect(Collectors.toList());

        return ReplayEventResult.builder()
                .replayId(replay.getReplayId())
                .importType(replay.getImportType())
                .totalCount(replay.getTotalCount() != null ? replay.getTotalCount() : 0)
                .queuedCount(replay.getQueuedCount() != null ? replay.getQueuedCount() : 0)
                .successCount(replay.getSuccessCount() != null ? replay.getSuccessCount() : 0)
                .skipCount(replay.getSkipCount() != null ? replay.getSkipCount() : 0)
                .failureCount(replay.getFailureCount() != null ? replay.getFailureCount() : 0)
                .actionResults(actionResults)
                .failures(parseFailures(replay.getFailureDetail()))
                .build();
    }

    /**
     * 解析失败详情
     */
    private List<String> parseFailures(String failureDetail) {
        List<String> failures = new ArrayList<>();
        if (StrUtil.isNotBlank(failureDetail)) {
            for (String part : failureDetail.split("; ")) {
                if (StrUtil.isNotBlank(part)) {
                    failures.add(part);
                }
            }
        }
        return failures;
    }

    /**
     * 失败详情描述
     */
    private String describeFailure(String vin, String actionType, String errorCode, String errorMessage) {
        return vin + ":" + actionType + " - "
                + ((errorCode != null ? errorCode + " " : "") + (errorMessage != null ? errorMessage : "未知错误"));
    }

    /**
     * 截断失败详情到列长限制
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
     * 截断失败原因到列长限制
     */
    private String truncateFailureReason(String reason) {
        if (reason == null) {
            return null;
        }
        if (reason.length() <= FAILURE_REASON_MAX_LENGTH) {
            return reason;
        }
        return reason.substring(0, FAILURE_REASON_MAX_LENGTH - 3) + "...";
    }
}
