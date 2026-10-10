package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayPostProcessResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionHandler;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.PartImportPostProcessReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayActionNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayInProgressException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessReplayStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 零件导入后置处理重放应用服务
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 负责资格校验、候选实例识别、当前快照装载、动作规划、执行调度、并发控制、
 * 动作级幂等和审计汇总（D34）。
 * <p>
 * 注意：
 * - 仅从原导入记录提取并去重候选 (partCode,sn)，禁止调用 ImportDataParserRegistry / 具体解析器 / 统一入站内核
 * - 不执行字段校验、type-schema 标准化、part_info upsert、绑定创建 / 换绑 / 解绑
 * - 不修改 part_import_data.handle/description、part_info 主体和 vehicle_part 绑定关系
 * - 主任务、每个动作状态及 Outbox 写入各自采用短事务；单动作失败不得回滚其他动作
 * - 事件动作以 Outbox 入队和动作状态更新同事务提交；同步调用不持有数据库长事务
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartImportPostProcessReplayAppService {

    private final PartImportDataRepository partImportDataRepository;
    private final PartImportPostProcessReplayRepository replayRepository;
    private final PartImportPostProcessReplayItemRepository itemRepository;
    private final PartImportPostProcessReplayExtractor replayExtractor;
    private final PartPostProcessActionRegistry actionRegistry;
    private final PartInfoRepository partInfoRepository;
    private final VehiclePartRepository vehiclePartRepository;
    private final MdmPartRepository mdmPartRepository;
    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private final VmdOutboxRepository vmdOutboxRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * error_message 字段最大长度
     */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 900;

    /**
     * RUNNING状态超时时间（分钟）
     */
    @Value("${vmd.replay.running-timeout-minutes:30}")
    private int runningTimeoutMinutes;

    /**
     * 重放执行模式
     */
    private enum ReplayMode {
        /** 全新完整重放 */
        FULL,
        /** 复用既有 replayId，仅继续失败或未完成动作 */
        RESUME,
        /** 新建 replayId，从最近一次终态重放的失败/未完成动作播种 */
        SEED_FAILED
    }

    /**
     * 执行零件导入后置处理重放
     *
     * @param id           零件导入数据ID
     * @param request      重放请求
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @return 重放结果
     */
    public ReplayPostProcessResult replay(Long id, ReplayPartImportPostProcessCmd request,
                                          String operatorId, String operatorName) {
        log.info("开始零件导入后置处理重放, id={}, operatorId={}", id, operatorId);
        LocalDateTime now = LocalDateTime.now();

        // 1. 加载并校验原导入记录
        PartImportData importData = partImportDataRepository.selectById(id);
        validateImportData(importData, id);
        List<PartImportPostProcessReplayExtractor.Candidate> candidates =
                replayExtractor.extractDistinctCandidates(importData.getData());
        if (candidates.isEmpty()) {
            throw new PartImportPostProcessReplayNotAllowedException("无法从原导入记录识别候选实例");
        }

        // 2. 解析动作范围
        List<PartPostProcessActionType> actionTypes = resolveScope(request.getScope());

        // 3. 解析 replayId 与执行模式
        boolean retryFailedOnly = Boolean.TRUE.equals(request.getRetryFailedOnly());
        String requestReplayId = StrUtil.isNotBlank(request.getRequestId()) ? request.getRequestId() : null;
        ReplayPlan plan = buildPlan(id, candidates, requestReplayId, retryFailedOnly);

        // 4. 执行中互斥（同一 part_import_data_id 同时仅允许一个 RUNNING 完整重放）
        assertNoConcurrentRunning(id, plan);

        // 5. 创建 / 复用主任务（短事务）
        PartImportPostProcessReplay master = replayRepository.selectByReplayId(plan.replayId());
        if (master == null) {
            PartImportPostProcessReplay newMaster = PartImportPostProcessReplay.builder()
                    .replayId(plan.replayId())
                    .partImportDataId(id)
                    .batchNum(importData.getBatchNum())
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .reason(request.getReason())
                    .scope(actionTypes.stream().map(PartPostProcessActionType::getValue).collect(Collectors.joining(",")))
                    .status(PartPostProcessReplayStatus.RUNNING.getValue())
                    .totalItemCount(0)
                    .totalActionCount(0)
                    .queuedCount(0)
                    .successCount(0)
                    .skippedCount(0)
                    .failureCount(0)
                    .startedAt(now)
                    .createTime(now)
                    .build();
            final PartImportPostProcessReplay created = newMaster;
            transactionTemplate.executeWithoutResult(tx -> replayRepository.insert(created));
            master = created;
        } else {
            master.setStatus(PartPostProcessReplayStatus.RUNNING.getValue());
            master.setStartedAt(now);
            master.setFinishedAt(null);
            if (StrUtil.isNotBlank(request.getReason())) {
                master.setReason(request.getReason());
            }
            final PartImportPostProcessReplay updated = master;
            transactionTemplate.executeWithoutResult(tx -> replayRepository.update(updated));
        }

        // 6. 逐候选实例规划并执行动作
        int totalActionCount = 0;
        int queuedCount = 0;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> failures = new ArrayList<>();

        for (PartImportPostProcessReplayExtractor.Candidate candidate : plan.candidates()) {
            if (plan.mode() == ReplayMode.SEED_FAILED && !plan.seedCandidates().contains(candidate)) {
                continue;
            }

            PartInfo partInfo = partInfoRepository.selectByPartCodeAndSn(candidate.partCode(), candidate.sn());
            VehiclePart activeBinding = partInfo != null ? vehiclePartRepository.selectActiveByPartId(partInfo.getId()) : null;
            Part mdmPart = mdmPartRepository.selectByCode(candidate.partCode());
            VehicleNode vehicleNode = resolveVehicleNode(partInfo, mdmPart);

            for (PartPostProcessActionType actionType : actionTypes) {
                // RESUME：仅继续该 replay 已规划且失败 / 未完成的动作；未规划或已成功 / 排队 / 跳过的动作不再执行
                if (plan.mode() == ReplayMode.RESUME) {
                    PartImportPostProcessReplayItem existing = itemRepository.selectByUniqueKey(
                            plan.replayId(), candidate.partCode(), candidate.sn(), actionType.getValue());
                    if (existing == null) {
                        continue;
                    }
                    if (!PartPostProcessActionStatus.valOf(existing.getStatus()).retryable()) {
                        continue;
                    }
                }
                // SEED_FAILED：仅执行播种的失败 / 未完成动作键
                if (plan.mode() == ReplayMode.SEED_FAILED
                        && !plan.seedKeys().contains(key(candidate.partCode(), candidate.sn(), actionType.getValue()))) {
                    continue;
                }

                totalActionCount++;
                int outcome = executeAction(plan.replayId(), id, importData.getBatchNum(), candidate,
                        partInfo, activeBinding, mdmPart, vehicleNode, actionType,
                        operatorId, operatorName, request.getReason(), now, failures);
                switch (outcome) {
                    case 0 -> queuedCount++;
                    case 1 -> successCount++;
                    case 2 -> skippedCount++;
                    default -> failureCount++;
                }
            }
        }

        // 7. 聚合主任务（短事务）
        List<PartImportPostProcessReplayItem> allItems = itemRepository.selectListByReplayId(plan.replayId());
        int q = 0, s = 0, sk = 0, f = 0;
        for (PartImportPostProcessReplayItem item : allItems) {
            switch (item.getStatus()) {
                case "QUEUED" -> q++;
                case "SUCCESS" -> s++;
                case "SKIPPED" -> sk++;
                case "FAILED_RETRYABLE", "FAILED_FINAL" -> f++;
                default -> {
                }
            }
        }
        int distinctItems = (int) allItems.stream()
                .map(i -> i.getPartCode() + ":" + i.getSn())
                .distinct().count();

        master.setTotalItemCount(distinctItems);
        master.setTotalActionCount(totalActionCount);
        master.setQueuedCount(q);
        master.setSuccessCount(s);
        master.setSkippedCount(sk);
        master.setFailureCount(f);
        master.setStatus(determineMasterStatus(q, s, sk, f));
        master.setFinishedAt(LocalDateTime.now());
        final PartImportPostProcessReplay finalMaster = master;
        transactionTemplate.executeWithoutResult(tx -> replayRepository.update(finalMaster));

        log.info("零件导入后置处理重放完成, id={}, replayId={}, item={}, action={}, queued={}, success={}, skipped={}, failure={}",
                id, plan.replayId(), distinctItems, totalActionCount, q, s, sk, f);

        return buildResult(master, failures);
    }

    /**
     * 查询重放主任务及逐项动作明细
     *
     * @param replayId 重放请求ID
     * @return 主任务 + 动作明细
     */
    public PartImportPostProcessReplayDto getReplayByReplayId(String replayId) {
        PartImportPostProcessReplay master = replayRepository.selectByReplayId(replayId);
        if (master == null) {
            throw new PartImportPostProcessReplayNotAllowedException("重放任务不存在: " + replayId);
        }
        List<PartImportPostProcessReplayItemDto> items = itemRepository.selectListByReplayId(replayId).stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
        return PartImportPostProcessReplayDto.builder()
                .id(master.getId())
                .replayId(master.getReplayId())
                .partImportDataId(master.getPartImportDataId())
                .batchNum(master.getBatchNum())
                .operatorId(master.getOperatorId())
                .operatorName(master.getOperatorName())
                .reason(master.getReason())
                .scope(master.getScope())
                .status(master.getStatus())
                .totalItemCount(master.getTotalItemCount())
                .totalActionCount(master.getTotalActionCount())
                .queuedCount(master.getQueuedCount())
                .successCount(master.getSuccessCount())
                .skippedCount(master.getSkippedCount())
                .failureCount(master.getFailureCount())
                .startedAt(master.getStartedAt())
                .finishedAt(master.getFinishedAt())
                .createTime(master.getCreateTime())
                .items(items)
                .build();
    }

    /**
     * 定时清理超时的RUNNING状态主任务
     * <p>
     * 每5分钟执行一次，将超过配置时间的RUNNING状态记录更新为FAILED
     */
    @Scheduled(fixedDelayString = "${vmd.replay.timeout-check-interval-ms:300000}")
    public void cleanupTimeoutRunningRecords() {
        log.debug("开始检查超时的RUNNING状态零件导入后置处理重放记录, timeoutMinutes={}", runningTimeoutMinutes);
        int updatedCount = replayRepository.updateTimeoutRunningToFailed(runningTimeoutMinutes);
        if (updatedCount > 0) {
            log.warn("已将{}条超时的RUNNING状态零件导入后置处理重放记录更新为FAILED, timeoutMinutes={}",
                    updatedCount, runningTimeoutMinutes);
        }
    }

    /**
     * 校验原导入记录
     */
    private void validateImportData(PartImportData importData, Long id) {
        if (ObjUtil.isNull(importData)) {
            throw new PartImportPostProcessReplayNotAllowedException("零件导入记录不存在");
        }
        if (StrUtil.isBlank(importData.getData())) {
            throw new PartImportPostProcessReplayNotAllowedException("原始数据不存在");
        }
    }

    /**
     * 解析动作范围（空 = 全部适用动作；未知动作类型抛 806080）
     */
    private List<PartPostProcessActionType> resolveScope(List<String> scope) {
        if (scope == null || scope.isEmpty()) {
            return List.of(PartPostProcessActionType.values());
        }
        List<PartPostProcessActionType> result = new ArrayList<>();
        for (String s : scope) {
            PartPostProcessActionType type = PartPostProcessActionType.valOf(s);
            if (type == null) {
                throw new PartImportPostProcessReplayActionNotFoundException(s);
            }
            result.add(type);
        }
        return result;
    }

    /**
     * 构建重放计划（replayId / 模式 / 候选 / 播种键）
     */
    private ReplayPlan buildPlan(Long id, List<PartImportPostProcessReplayExtractor.Candidate> candidates,
                                 String requestReplayId, boolean retryFailedOnly) {
        if (retryFailedOnly) {
            if (StrUtil.isNotBlank(requestReplayId)) {
                PartImportPostProcessReplay existing = replayRepository.selectByReplayId(requestReplayId);
                if (existing == null) {
                    throw new PartImportPostProcessReplayNotAllowedException("replayId[" + requestReplayId + "]不存在");
                }
                if (!existing.getPartImportDataId().equals(id)) {
                    throw new PartImportPostProcessReplayNotAllowedException("replayId 不属于该零件导入记录");
                }
                return new ReplayPlan(ReplayMode.RESUME, requestReplayId, candidates, Set.of(), Set.of(), existing);
            }
            PartImportPostProcessReplay latest = replayRepository.selectLatestTerminalByPartImportDataId(id);
            if (latest == null) {
                throw new PartImportPostProcessReplayNotAllowedException("无历史重放任务可重试失败项");
            }
            List<PartImportPostProcessReplayItem> retryableItems = itemRepository.selectRetryableByReplayId(latest.getReplayId());
            if (retryableItems.isEmpty()) {
                throw new PartImportPostProcessReplayNotAllowedException("历史重放任务无失败或未完成动作可重试");
            }
            Set<String> seedKeys = retryableItems.stream()
                    .map(i -> key(i.getPartCode(), i.getSn(), i.getActionType()))
                    .collect(Collectors.toSet());
            Set<PartImportPostProcessReplayExtractor.Candidate> seedCandidates = new LinkedHashSet<>();
            retryableItems.forEach(i -> seedCandidates.add(
                    new PartImportPostProcessReplayExtractor.Candidate(i.getPartCode(), i.getSn())));
            String replayId = UUID.randomUUID().toString();
            return new ReplayPlan(ReplayMode.SEED_FAILED, replayId, candidates, seedKeys, seedCandidates, null);
        }

        if (StrUtil.isNotBlank(requestReplayId) && replayRepository.selectByReplayId(requestReplayId) != null) {
            throw new PartImportPostProcessReplayNotAllowedException("replayId 已存在，请使用新的 replayId");
        }
        String replayId = StrUtil.isNotBlank(requestReplayId) ? requestReplayId : UUID.randomUUID().toString();
        return new ReplayPlan(ReplayMode.FULL, replayId, candidates, Set.of(), Set.of(), null);
    }

    /**
     * 执行中互斥校验
     */
    private void assertNoConcurrentRunning(Long id, ReplayPlan plan) {
        long running = replayRepository.countRunningByPartImportDataId(id);
        if (plan.mode() == ReplayMode.RESUME) {
            boolean selfRunning = plan.existingReplay() != null
                    && PartPostProcessReplayStatus.RUNNING.getValue().equals(plan.existingReplay().getStatus());
            if (running > (selfRunning ? 1 : 0)) {
                throw new PartImportPostProcessReplayInProgressException(id);
            }
        } else if (running > 0) {
            throw new PartImportPostProcessReplayInProgressException(id);
        }
    }

    /**
     * 执行单个动作（短事务；返回 0=QUEUED 1=SUCCESS 2=SKIPPED 3=FAILED）
     */
    private int executeAction(String replayId, Long partImportDataId, String batchNum,
                              PartImportPostProcessReplayExtractor.Candidate candidate,
                              PartInfo partInfo, VehiclePart activeBinding, Part mdmPart, VehicleNode vehicleNode,
                              PartPostProcessActionType actionType,
                              String operatorId, String operatorName, String reason, LocalDateTime now,
                              List<String> failures) {
        String actionValue = actionType.getValue();
        PartImportPostProcessReplayItem existingItem = itemRepository.selectByUniqueKey(replayId, candidate.partCode(), candidate.sn(), actionValue);
        PartImportPostProcessReplayItem item = existingItem != null
                ? existingItem
                : PartImportPostProcessReplayItem.builder()
                        .replayId(replayId)
                        .partCode(candidate.partCode())
                        .sn(candidate.sn())
                        .actionType(actionValue)
                        .status(PartPostProcessActionStatus.PENDING.getValue())
                        .attemptCount(0)
                        .createTime(now)
                        .build();
        int attemptCount = (item.getAttemptCount() != null ? item.getAttemptCount() : 0) + 1;

        // 候选实例在当前已不存在 → FAILED_FINAL(PART_NOT_FOUND)，不得依靠历史报文重建
        if (partInfo == null) {
            item.setStatus(PartPostProcessActionStatus.FAILED_FINAL.getValue());
            item.setAttemptCount(attemptCount);
            item.setErrorCode("PART_NOT_FOUND");
            item.setErrorMessage("候选实例在当前已不存在，不得依据历史报文重建");
            item.setStartedAt(now);
            item.setFinishedAt(now);
            persistItem(item);
            failures.add(describeFailure(candidate, actionValue, "PART_NOT_FOUND"));
            return 3;
        }

        PartPostProcessActionHandler handler = actionRegistry.getHandler(actionValue);
        if (handler == null) {
            item.setStatus(PartPostProcessActionStatus.FAILED_FINAL.getValue());
            item.setAttemptCount(attemptCount);
            item.setErrorCode("806080");
            item.setErrorMessage("动作未登记: " + actionValue);
            item.setStartedAt(now);
            item.setFinishedAt(now);
            persistItem(item);
            failures.add(describeFailure(candidate, actionValue, "806080 动作未登记"));
            return 3;
        }

        PartPostProcessActionContext context = PartPostProcessActionContext.builder()
                .replayId(replayId)
                .partImportDataId(partImportDataId)
                .batchNum(batchNum)
                .partCode(candidate.partCode())
                .sn(candidate.sn())
                .partInfo(partInfo)
                .activeBinding(activeBinding)
                .mdmPart(mdmPart)
                .vehicleNode(vehicleNode)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .reason(reason)
                .build();

        try {
            item.setStatus(PartPostProcessActionStatus.RUNNING.getValue());
            item.setAttemptCount(attemptCount);
            item.setStartedAt(now);
            item.setFinishedAt(null);
            item.setErrorCode(null);
            item.setErrorMessage(null);
            item.setSkipReason(null);
            item.setEventId(null);
            item.setOutboxId(null);
            persistItem(item);

            PartPostProcessActionResult result = handler.execute(context);
            String outcome = result.getOutcome().name();
            switch (outcome) {
                case "QUEUED" -> {
                    VmdOutbox outbox = result.getOutbox();
                    transactionTemplate.executeWithoutResult(tx -> {
                        if (outbox != null) {
                            vmdOutboxRepository.insert(outbox);
                        }
                        item.setStatus(PartPostProcessActionStatus.QUEUED.getValue());
                        item.setEventId(result.getEventId());
                        item.setOutboxId(outbox != null ? outbox.getId() : null);
                        item.setFinishedAt(LocalDateTime.now());
                        persistItemInTx(item);
                    });
                    return 0;
                }
                case "SUCCESS" -> {
                    item.setStatus(PartPostProcessActionStatus.SUCCESS.getValue());
                    item.setFinishedAt(LocalDateTime.now());
                    persistItem(item);
                    return 1;
                }
                case "SKIPPED" -> {
                    item.setStatus(PartPostProcessActionStatus.SKIPPED.getValue());
                    item.setSkipReason(result.getSkipReason());
                    item.setFinishedAt(LocalDateTime.now());
                    persistItem(item);
                    return 2;
                }
                default -> {
                    // FAILED_RETRYABLE / FAILED_FINAL
                    item.setStatus(outcome.equals("FAILED_RETRYABLE")
                            ? PartPostProcessActionStatus.FAILED_RETRYABLE.getValue()
                            : PartPostProcessActionStatus.FAILED_FINAL.getValue());
                    item.setErrorCode(result.getErrorCode());
                    item.setErrorMessage(result.getErrorMessage());
                    item.setFinishedAt(LocalDateTime.now());
                    persistItem(item);
                    failures.add(describeFailure(candidate, actionValue,
                            (result.getErrorCode() != null ? result.getErrorCode() + " " : "") + result.getErrorMessage()));
                    return 3;
                }
            }
        } catch (Exception e) {
            item.setStatus(PartPostProcessActionStatus.FAILED_RETRYABLE.getValue());
            item.setErrorMessage(truncate(e.getMessage()));
            item.setFinishedAt(LocalDateTime.now());
            persistItem(item);
            failures.add(describeFailure(candidate, actionValue, e.getMessage()));
            return 3;
        }
    }

    /**
     * 解析当前 VehicleNode 投影（part_info 节点优先，缺失回退 MDM Part 投影）
     */
    private VehicleNode resolveVehicleNode(PartInfo partInfo, Part mdmPart) {
        String nodeCode = null;
        if (partInfo != null && StrUtil.isNotBlank(partInfo.getVehicleNodeCode())) {
            nodeCode = partInfo.getVehicleNodeCode();
        } else if (mdmPart != null && StrUtil.isNotBlank(mdmPart.getVehicleNodeCode())) {
            nodeCode = mdmPart.getVehicleNodeCode();
        }
        return StrUtil.isBlank(nodeCode) ? null : mdmVehicleNodeRepository.selectByCode(nodeCode);
    }

    /**
     * 短事务持久化动作明细（事务内调用）
     */
    private void persistItemInTx(PartImportPostProcessReplayItem item) {
        if (item.getId() == null) {
            itemRepository.insert(item);
        } else {
            itemRepository.update(item);
        }
    }

    /**
     * 短事务持久化动作明细
     */
    private void persistItem(PartImportPostProcessReplayItem item) {
        transactionTemplate.executeWithoutResult(tx -> persistItemInTx(item));
    }

    /**
     * 根据动作结果统计确定主任务状态
     */
    private String determineMasterStatus(int queued, int success, int skipped, int failure) {
        if (failure == 0) {
            return PartPostProcessReplayStatus.SUCCESS.getValue();
        }
        if (queued + success + skipped == 0) {
            return PartPostProcessReplayStatus.FAILED.getValue();
        }
        return PartPostProcessReplayStatus.PARTIAL_SUCCESS.getValue();
    }

    private String key(String partCode, String sn, String actionType) {
        return partCode + ":" + sn + ":" + actionType;
    }

    private String describeFailure(PartImportPostProcessReplayExtractor.Candidate candidate, String actionType, String reason) {
        return candidate.partCode() + ":" + candidate.sn() + ":" + actionType + " - " + (reason != null ? reason : "未知错误");
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= ERROR_MESSAGE_MAX_LENGTH
                ? message : message.substring(0, ERROR_MESSAGE_MAX_LENGTH - 3) + "...";
    }

    private PartImportPostProcessReplayItemDto toItemDto(PartImportPostProcessReplayItem item) {
        return PartImportPostProcessReplayItemDto.builder()
                .id(item.getId())
                .replayId(item.getReplayId())
                .partCode(item.getPartCode())
                .sn(item.getSn())
                .actionType(item.getActionType())
                .idempotencyKey(item.getIdempotencyKey())
                .status(item.getStatus())
                .attemptCount(item.getAttemptCount())
                .eventId(item.getEventId())
                .outboxId(item.getOutboxId())
                .errorCode(item.getErrorCode())
                .errorMessage(item.getErrorMessage())
                .skipReason(item.getSkipReason())
                .startedAt(item.getStartedAt())
                .finishedAt(item.getFinishedAt())
                .build();
    }

    private ReplayPostProcessResult buildResult(PartImportPostProcessReplay master, List<String> failures) {
        return ReplayPostProcessResult.builder()
                .replayId(master.getReplayId())
                .itemCount(master.getTotalItemCount() != null ? master.getTotalItemCount() : 0)
                .actionCount(master.getTotalActionCount() != null ? master.getTotalActionCount() : 0)
                .queuedCount(master.getQueuedCount() != null ? master.getQueuedCount() : 0)
                .successCount(master.getSuccessCount() != null ? master.getSuccessCount() : 0)
                .skippedCount(master.getSkippedCount() != null ? master.getSkippedCount() : 0)
                .failureCount(master.getFailureCount() != null ? master.getFailureCount() : 0)
                .failures(failures)
                .build();
    }

    /**
     * 重放计划
     */
    private record ReplayPlan(ReplayMode mode, String replayId,
                              List<PartImportPostProcessReplayExtractor.Candidate> candidates,
                              Set<String> seedKeys,
                              Set<PartImportPostProcessReplayExtractor.Candidate> seedCandidates,
                              PartImportPostProcessReplay existingReplay) {
    }
}
