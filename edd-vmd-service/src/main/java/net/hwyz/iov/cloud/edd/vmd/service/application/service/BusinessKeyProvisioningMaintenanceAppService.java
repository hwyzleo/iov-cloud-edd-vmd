package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OperatorContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.TerminateStaleProvisioningCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningBlockingDiagnosisResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningTerminationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ProvisioningOperationEvidence.OperationOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateDirectoryConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateDisabledException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateReconcileFirstException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateStaleViewException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateSuccessFactExistsException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMaintenanceMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 滞留开通批次运维终止应用服务（VMD-DSN-CR-065 §3.1）。
 * <p>
 * 对外提供两个方法：
 * <ul>
 *   <li>{@link #inspectTermination(String)}：只读诊断（供 MPT 页面展示），返回 ELIGIBLE/RECONCILE_FIRST/NOT_STALE/
 *       STATE_NOT_ALLOWED/SUCCESS_FACT_EXISTS/DIRECTORY_CONFLICT 等门禁结论与页面字段；预检只读结果不是授权令牌；</li>
 *   <li>{@link #terminateStale(TerminateStaleProvisioningCommand, OperatorContext)}：唯一破坏性入口，单事务内
 *       重跑全部安全门禁 → 锁定批次/明细/目录 → PENDING 目录连带失败化 → 批次逻辑终止 FAILED → 提交后指标与告警。</li>
 * </ul>
 * 门禁要点（RD-065-1/2/3）：只提供 TERMINATE_STALE_PROVISIONING 一种破坏性动作；不删除数据、不伪造成功、
 * 不自动按证书变化终止；WAIT_DEVICE_ACK 复用 expireStaleAckBatches → EXPIRED；COMPLETED 永不允许逆转；
 * FAILED/EXPIRED 幂等成功无状态变更。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningMaintenanceAppService {

    private static final String EVENT_TYPE_TBOX_V2C = "TboxV2cKeyProvisionedEvent";

    private static final Pattern VIN_PATTERN = Pattern.compile("\\b[A-HJ-NPR-Z0-9]{17}\\b");
    private static final Pattern HEX_BLOB_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{32,}\\b");
    private static final Pattern BASE64_BLOB_PATTERN = Pattern.compile("\\b[A-Za-z0-9+/]{44,}={0,2}\\b");
    private static final Pattern SENSITIVE_TOKEN_PATTERN = Pattern.compile("(keyId|kmsKeyRef|operationId|certSn|wrappedKey|kcv|secret|token)[=:\\s]*\\S+",
            Pattern.CASE_INSENSITIVE);

    private static final int REASON_MAX_LENGTH = 200;

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyProvisioningDomainService domainService;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    private final VmdOutboxRepository vmdOutboxRepository;
    private final ProvisioningOperationEvidence operationEvidence;
    private final BusinessKeyProvisioningMaintenanceMetrics metrics;
    private final PlatformTransactionManager transactionManager;

    @Value("${vmd.business-key.maintenance.terminate-enabled:false}")
    private boolean terminateEnabled = false;

    @Value("${vmd.business-key.maintenance.terminate-stale-threshold-seconds:86400}")
    private long staleThresholdSeconds = 86400;

    private volatile TransactionTemplate transactionTemplate;

    private TransactionTemplate tx() {
        TransactionTemplate tpl = this.transactionTemplate;
        if (tpl == null) {
            synchronized (this) {
                if (this.transactionTemplate == null) {
                    this.transactionTemplate = new TransactionTemplate(transactionManager);
                }
                tpl = this.transactionTemplate;
            }
        }
        return tpl;
    }

    /**
     * 只读阻塞诊断（VMD-DSN-CR-065 §6.2）。
     * <p>仅用于页面展示；执行必须重做全部门禁。
     */
    public ProvisioningBlockingDiagnosisResult inspectTermination(String requestId) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        if (batch == null) {
            throw new BusinessKeyProvisioningNotExistException("requestId=" + requestId);
        }
        List<BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningId(batch.getId());
        Map<Long, OperationOutcome> opOutcomes = operationEvidence.query(items);

        return buildDiagnosis(batch, items, opOutcomes);
    }

    /**
     * 运维一键清理（VMD-DSN-CR-065 §6.3）。
     *
     * @param cmd   终止命令（expectedState/expectedModifyTime 防过期页面）
     * @param operatorCtx 操作者上下文（审计）
     * @return 终止结果；门禁不通过时抛稳定 806xxx 异常且零数据变更
     */
    public ProvisioningTerminationResult terminateStale(TerminateStaleProvisioningCommand cmd, OperatorContext operatorCtx) {
        if (cmd == null || cmd.getRequestId() == null || cmd.getRequestId().isBlank()) {
            throw new IllegalArgumentException("终止命令缺少 requestId");
        }
        if (!terminateEnabled) {
            metrics.recordBlocked("TERMINATE_DISABLED");
            throw new BusinessKeyProvisioningTerminateDisabledException(
                    "业务密钥开通批次运维终止未启用（vmd.business-key.maintenance.terminate-enabled=false）");
        }
        if (cmd.getReasonCode() == null) {
            throw new BusinessKeyProvisioningTerminateNotAllowedException("缺少受治理终止原因码 reasonCode");
        }
        String sanitizedReason = sanitizeReason(cmd.getReason());

        // 事务外 operation 证据快照：远程 Provider 查询不得持有数据库锁（VMD-DSN-CR-065 §5.1）
        BusinessKeyProvisioning preBatch = provisioningRepository.selectByRequestId(cmd.getRequestId());
        if (preBatch == null) {
            throw new BusinessKeyProvisioningNotExistException("requestId=" + cmd.getRequestId());
        }
        List<BusinessKeyProvisioningItem> preItems = provisioningItemRepository.selectByProvisioningId(preBatch.getId());
        Map<Long, OperationOutcome> opOutcomes = operationEvidence.query(preItems);

        return tx().execute(status -> doTerminate(cmd, sanitizedReason, operatorCtx, opOutcomes));
    }

    /**
     * 单事务内执行终止（VMD-DSN-CR-065 §5.2）。
     */
    private ProvisioningTerminationResult doTerminate(TerminateStaleProvisioningCommand cmd, String sanitizedReason,
                                                      OperatorContext operatorCtx,
                                                      Map<Long, OperationOutcome> opOutcomes) {
        LocalDateTime now = LocalDateTime.now();
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestIdForUpdate(cmd.getRequestId());
        if (batch == null) {
            throw new BusinessKeyProvisioningNotExistException("requestId=" + cmd.getRequestId());
        }
        String fromState = batch.getStatus() == null ? null : batch.getStatus().getValue();
        String reasonCode = cmd.getReasonCode() == null ? null : cmd.getReasonCode().getValue();

        // 1) 终态幂等（RD-065-2）：FAILED/EXPIRED 幂等成功无状态变更（先于 STALE_VIEW，
        //    保证两个终止并发时一次变更、一次幂等返回，VMD-DSN-CR-065 §9.2）
        if (batch.getStatus() == BusinessKeyProvisioningState.FAILED
                || batch.getStatus() == BusinessKeyProvisioningState.EXPIRED) {
            metrics.recordTermination(fromState, reasonCode, "idempotent");
            return ProvisioningTerminationResult.builder()
                    .requestId(batch.getRequestId())
                    .beforeState(fromState)
                    .afterState(fromState)
                    .changed(false)
                    .blockingCleared(true)
                    .reopenAllowed(true)
                    .failedPendingKeyCount(0)
                    .blockingReasons(List.of("批次已终结，幂等返回，无状态变更"))
                    .operatedAt(now)
                    .build();
        }

        // 2) STALE_VIEW：防止运维基于过期页面执行
        if (cmd.getExpectedState() != null && cmd.getExpectedState() != batch.getStatus()) {
            metrics.recordBlocked("STALE_VIEW");
            throw new BusinessKeyProvisioningTerminateStaleViewException(
                    "页面批次状态已变化: expected=" + cmd.getExpectedState() + ", actual=" + batch.getStatus());
        }
        if (cmd.getExpectedModifyTime() != null && batch.getModifyTime() != null
                && !cmd.getExpectedModifyTime().truncatedTo(ChronoUnit.SECONDS)
                .equals(batch.getModifyTime().truncatedTo(ChronoUnit.SECONDS))) {
            metrics.recordBlocked("STALE_VIEW");
            throw new BusinessKeyProvisioningTerminateStaleViewException(
                    "页面批次修改时间已变化，请刷新后重试: requestId=" + cmd.getRequestId());
        }

        // 3) 状态不允许（RD-065-2）
        if (batch.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
            metrics.recordBlocked("STATE_NOT_ALLOWED");
            throw new BusinessKeyProvisioningTerminateNotAllowedException(
                    "批次已COMPLETED（存在合法成功事实），禁止运维终止: requestId=" + cmd.getRequestId());
        }
        if (batch.getStatus() == BusinessKeyProvisioningState.WAIT_DEVICE_ACK) {
            metrics.recordBlocked("WAIT_ACK_EXPIRY");
            throw new BusinessKeyProvisioningTerminateNotAllowedException(
                    "WAIT_DEVICE_ACK 仅由 ACK 超时任务治理（expireStaleAckBatches）: requestId=" + cmd.getRequestId());
        }
        // 此处仅 PREPARING / RECONCILE_REQUIRED 继续

        // 4) NOT_STALE：最小滞留阈值
        LocalDateTime lastActivity = batch.getModifyTime() != null ? batch.getModifyTime() : batch.getStartedAt();
        if (lastActivity != null && Duration.between(lastActivity, now).getSeconds() < staleThresholdSeconds) {
            metrics.recordBlocked("NOT_STALE");
            throw new BusinessKeyProvisioningTerminateNotAllowedException(
                    "未超过最小滞留阈值(" + staleThresholdSeconds + "s)，暂不可清理: requestId=" + cmd.getRequestId());
        }

        // 4) 锁定明细与目录（VMD-DSN-CR-065 §5.1 锁顺序 3/4）
        List<BusinessKeyProvisioningItem> items = provisioningItemRepository.selectByProvisioningIdForUpdate(batch.getId());
        Map<Long, DeviceBusinessKey> directories = new LinkedHashMap<>();
        for (BusinessKeyProvisioningItem item : items) {
            if (item.getDirectoryKeyId() != null) {
                DeviceBusinessKey directory = deviceBusinessKeyRepository.selectByIdForUpdate(item.getDirectoryKeyId());
                if (directory != null) {
                    directories.put(item.getDirectoryKeyId(), directory);
                }
            }
        }

        // 5) 成功事实反查（§4.3）
        if (hasSuccessFacts(batch, items)) {
            metrics.recordBlocked("SUCCESS_FACT_EXISTS");
            throw new BusinessKeyProvisioningTerminateSuccessFactExistsException(
                    "存在成功事实（ACK/成功明细/成功事件），禁止终止: requestId=" + cmd.getRequestId());
        }

        // 6) 目录归属冲突（§4.1 DIRECTORY_CONFLICT / §3.2）
        if (hasDirectoryConflict(items, directories, batch)) {
            metrics.recordBlocked("DIRECTORY_CONFLICT");
            throw new BusinessKeyProvisioningTerminateDirectoryConflictException(
                    "目录版本不全为本批次 PENDING，禁止终止: requestId=" + cmd.getRequestId());
        }

        // 7) operation 安全判定（§4.2，使用事务外快照；明细已加锁，operationId 不会变化）
        for (BusinessKeyProvisioningItem item : items) {
            OperationOutcome outcome = opOutcomes.getOrDefault(item.getId(), OperationOutcome.IN_PROGRESS_OR_UNKNOWN);
            if (outcome == OperationOutcome.SUCCEEDED) {
                metrics.recordBlocked("OPERATION_SUCCEEDED");
                throw new BusinessKeyProvisioningTerminateSuccessFactExistsException(
                        "operation 已成功，不得终止，应恢复原结果并推进批次: purpose=" + item.getPurpose());
            }
            if (outcome == OperationOutcome.IN_PROGRESS_OR_UNKNOWN) {
                metrics.recordBlocked("RECONCILE_FIRST");
                throw new BusinessKeyProvisioningTerminateReconcileFirstException(
                        "存在结果未知/处理中的 operation，需先对账: purpose=" + item.getPurpose());
            }
        }

        // 8) 执行终止（§5.2 原子提交）：PENDING 目录连带失败化 → 批次 FAILED
        List<String> keyIds = items.stream().map(BusinessKeyProvisioningItem::getKeyId)
                .filter(Objects::nonNull).toList();
        int expectedPending = (int) directories.values().stream()
                .filter(d -> d.getKeyState() == BusinessKeyState.PENDING).count();
        String compactReason = buildCompactReason(cmd, sanitizedReason, operatorCtx);
        int affected = deviceBusinessKeyRepository.markPendingKeysFailed(batch.getRequestId(), keyIds, compactReason);
        if (affected != expectedPending) {
            // 数量不符 → 整体回滚
            metrics.recordBlocked("DIRECTORY_CONFLICT");
            throw new BusinessKeyProvisioningTerminateDirectoryConflictException(
                    "目录失败化数量与预期不一致，整体回滚: expected=" + expectedPending + ", affected=" + affected
                            + ", requestId=" + cmd.getRequestId());
        }

        boolean changed = batch.terminateByOperator(cmd.getReasonCode(),
                buildSanitizedReasonWithAuditRef(sanitizedReason, cmd, operatorCtx), now);
        if (!changed) {
            metrics.recordBlocked("STATE_NOT_ALLOWED");
            throw new BusinessKeyProvisioningTerminateNotAllowedException(
                    "批次状态不允许运维终止: state=" + batch.getStatus() + ", requestId=" + cmd.getRequestId());
        }
        provisioningRepository.update(batch);

        // 9) 审计日志（结构化、非敏感；持久审计由 MPT 层 framework-audit-starter @Log 承载）
        logAudit(batch, cmd, operatorCtx, fromState, affected);

        // 10) 指标（§7.2）
        metrics.recordTermination(fromState, reasonCode, "success");
        metrics.recordPendingFailed(affected);
        metrics.recordStale(fromState != null ? fromState : "UNKNOWN");

        log.info("业务密钥开通批次运维终止完成: requestId={}, deviceSn={}, domain={}, from={}, failedPendingKeys={}",
                batch.getRequestId(), batch.getDeviceSn(), batch.getBusinessDomain(), fromState, affected);

        return ProvisioningTerminationResult.builder()
                .requestId(batch.getRequestId())
                .beforeState(fromState)
                .afterState(batch.getStatus() == null ? null : batch.getStatus().getValue())
                .changed(true)
                .blockingCleared(true)
                .reopenAllowed(true)
                .failedPendingKeyCount(affected)
                .blockingReasons(List.of())
                .operatedAt(now)
                .build();
    }

    /**
     * 构建 MPT 诊断结果（只读门禁结论）。
     */
    private ProvisioningBlockingDiagnosisResult buildDiagnosis(BusinessKeyProvisioning batch,
                                                               List<BusinessKeyProvisioningItem> items,
                                                               Map<Long, OperationOutcome> opOutcomes) {
        LocalDateTime now = LocalDateTime.now();
        List<BusinessKeyProvisioningItem> sortedItems = items == null ? List.of() : items;
        Map<Long, DeviceBusinessKey> directories = loadDirectories(sortedItems);
        List<String> blocking = new ArrayList<>();
        Boolean certificateChanged = resolveCertificateChanged(batch);
        boolean terminal = batch.getStatus() == BusinessKeyProvisioningState.FAILED
                || batch.getStatus() == BusinessKeyProvisioningState.EXPIRED;
        boolean successFact = hasSuccessFacts(batch, sortedItems);
        boolean directoryConflict = hasDirectoryConflict(sortedItems, directories, batch);
        int pendingCount = (int) directories.values().stream()
                .filter(d -> d.getKeyState() == BusinessKeyState.PENDING).count();
        OperationSummary summary = buildOperationSummary(opOutcomes);

        ProvisioningBlockingDiagnosisResult.OperationSummary opSummary =
                ProvisioningBlockingDiagnosisResult.OperationSummary.builder()
                        .notStarted(summary.notStarted)
                        .confirmedFailed(summary.confirmedFailed)
                        .succeeded(summary.succeeded)
                        .inProgressOrUnknown(summary.inProgressOrUnknown)
                        .build();

        boolean clearable;
        boolean reopenAllowed;
        String suggestedAction;
        List<String> reasons;

        if (terminal) {
            clearable = false;
            reopenAllowed = true;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("批次已终结（FAILED/EXPIRED），无需清理");
        } else if (batch.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("批次已完成，不可清理");
        } else if (batch.getStatus() == BusinessKeyProvisioningState.WAIT_DEVICE_ACK) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "WAIT_ACK_EXPIRY";
            reasons = List.of("等待设备导入ACK，由ACK超时任务治理（expireStaleAckBatches→EXPIRED）");
        } else if (successFact) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("存在成功事实（ACK/成功明细/成功事件），禁止终止");
        } else if (directoryConflict) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("目录版本不全为本批次 PENDING，需数据治理");
        } else if (summary.succeeded > 0) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("存在已成功的 operation，禁止终止");
        } else if (summary.inProgressOrUnknown > 0) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "RECONCILE_FIRST";
            reasons = List.of("存在处理中或结果未知的 operation，需先对账");
        } else if (!isStaleEnough(batch, now)) {
            clearable = false;
            reopenAllowed = false;
            suggestedAction = "NOT_CLEARABLE";
            reasons = List.of("未超过最小滞留阈值(" + staleThresholdSeconds + "s)");
        } else {
            clearable = true;
            reopenAllowed = true;
            suggestedAction = "CLEARABLE";
            reasons = List.of();
        }

        boolean uniqueConstraintOccupied = batch.getStatus() != null && batch.getStatus().isOpen();
        Long staleSeconds = staleSeconds(batch, now);

        return ProvisioningBlockingDiagnosisResult.builder()
                .requestId(batch.getRequestId())
                .deviceSn(batch.getDeviceSn())
                .businessDomain(batch.getBusinessDomain())
                .status(batch.getStatus() == null ? null : batch.getStatus().getValue())
                .modifyTime(batch.getModifyTime())
                .staleDurationSeconds(staleSeconds)
                .failReason(batch.getFailReason())
                .certificateChanged(certificateChanged)
                .operationSummary(opSummary)
                .pendingDirectoryCount(pendingCount)
                .uniqueConstraintOccupied(uniqueConstraintOccupied)
                .clearable(clearable)
                .blockingReasons(reasons)
                .reopenAllowedAfterClear(reopenAllowed)
                .suggestedAction(suggestedAction)
                .build();
    }

    /**
     * 判断是否超过最小滞留阈值。
     */
    private boolean isStaleEnough(BusinessKeyProvisioning batch, LocalDateTime now) {
        LocalDateTime lastActivity = batch.getModifyTime() != null ? batch.getModifyTime() : batch.getStartedAt();
        return lastActivity == null || Duration.between(lastActivity, now).getSeconds() >= staleThresholdSeconds;
    }

    private Long staleSeconds(BusinessKeyProvisioning batch, LocalDateTime now) {
        LocalDateTime lastActivity = batch.getModifyTime() != null ? batch.getModifyTime() : batch.getStartedAt();
        if (lastActivity == null) {
            return null;
        }
        return Math.max(0L, Duration.between(lastActivity, now).getSeconds());
    }

    /**
     * 批量加载明细对应目录行。
     */
    private Map<Long, DeviceBusinessKey> loadDirectories(List<BusinessKeyProvisioningItem> items) {
        Map<Long, DeviceBusinessKey> directories = new LinkedHashMap<>();
        for (BusinessKeyProvisioningItem item : items) {
            if (item.getDirectoryKeyId() != null) {
                DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
                if (directory != null) {
                    directories.put(item.getDirectoryKeyId(), directory);
                }
            }
        }
        return directories;
    }

    /**
     * 成功事实反查（VMD-DSN-CR-065 §4.3）：任一命中立即拒绝。
     */
    private boolean hasSuccessFacts(BusinessKeyProvisioning batch, List<BusinessKeyProvisioningItem> items) {
        if (batch.getAckDigest() != null) {
            return true;
        }
        if (items != null) {
            for (BusinessKeyProvisioningItem item : items) {
                if (item.getDeviceResult() == ProvisioningDeviceResult.SUCCEEDED) {
                    return true;
                }
            }
        }
        return hasSuccessOutbox(batch.getRequestId());
    }

    /**
     * 是否存在该 requestId 的 TboxV2cKeyProvisionedEvent 成功 Outbox（成功事实之一）。
     * <p>按 aggregate_id + event_type 精确过滤；查询异常按不可判定处理（fail-closed 视为存在成功事实）。
     */
    private boolean hasSuccessOutbox(String requestId) {
        try {
            return vmdOutboxRepository.countByAggregateAndEventType(requestId, EVENT_TYPE_TBOX_V2C) > 0;
        } catch (Exception e) {
            // Outbox 查询异常按不可判定处理：fail-closed 视为存在成功事实，禁止终止
            log.warn("Outbox 成功事实查询失败，按存在成功事实拒绝终止: requestId={}, cause={}",
                    requestId, e.getClass().getSimpleName());
            return true;
        }
    }

    /**
     * 目录归属冲突（VMD-DSN-CR-065 §3.2/§4.1）：任一明细目录不存在、requestId 不归属本批次或非 PENDING。
     */
    private boolean hasDirectoryConflict(List<BusinessKeyProvisioningItem> items,
                                         Map<Long, DeviceBusinessKey> directories,
                                         BusinessKeyProvisioning batch) {
        for (BusinessKeyProvisioningItem item : items) {
            if (item.getDirectoryKeyId() == null) {
                // 明细未关联目录（item 落库前失败场景）：不构成冲突，交由 operation 判定
                continue;
            }
            DeviceBusinessKey directory = directories.get(item.getDirectoryKeyId());
            if (directory == null) {
                return true;
            }
            if (directory.getRequestId() != null && !directory.getRequestId().equals(batch.getRequestId())) {
                return true;
            }
            if (directory.getKeyState() != BusinessKeyState.PENDING) {
                return true;
            }
        }
        return false;
    }

    /**
     * 批次冻结证书摘要 vs 当前 ACTIVE 证书摘要（仅展示，不含证书材料）。
     * <p>V2C 批次冻结的是 ECDH-ES v1 逻辑证书摘要（CR-063），故与当前 {@code resolveActiveEcdhV1}
     * 比较；解析不可用时按未变化处理（best-effort，不作为执行门禁）。
     */
    private Boolean resolveCertificateChanged(BusinessKeyProvisioning batch) {
        if (batch.getDeviceSn() == null || batch.getCertificateSnDigest() == null) {
            return Boolean.FALSE;
        }
        try {
            BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolveByDeviceSn(batch.getDeviceSn(), null);
            ActiveDeviceCertificate current = activeDeviceCertificateResolver.resolveActiveEcdhV1(identity);
            String currentDigest = domainService.sha256Hex(domainService.utf8(current.certSn()));
            return !batch.getCertificateSnDigest().equals(currentDigest);
        } catch (Exception e) {
            log.warn("证书变更判定不可用（设备/证书解析失败），按未变化处理: requestId={}, deviceSn={}, cause={}",
                    batch.getRequestId(), batch.getDeviceSn(), e.getClass().getSimpleName());
            return Boolean.FALSE;
        }
    }

    private OperationSummary buildOperationSummary(Map<Long, OperationOutcome> opOutcomes) {
        OperationSummary summary = new OperationSummary();
        if (opOutcomes != null) {
            for (OperationOutcome outcome : opOutcomes.values()) {
                switch (outcome) {
                    case NOT_STARTED -> summary.notStarted++;
                    case CONFIRMED_FAILED -> summary.confirmedFailed++;
                    case SUCCEEDED -> summary.succeeded++;
                    case IN_PROGRESS_OR_UNKNOWN -> summary.inProgressOrUnknown++;
                }
            }
        }
        return summary;
    }

    private static final class OperationSummary {
        int notStarted;
        int confirmedFailed;
        int succeeded;
        int inProgressOrUnknown;
    }

    /**
     * 构造批次 fail_reason 使用的紧凑非敏感终止原因（终止码 + 审计关联号）。
     */
    private String buildCompactReason(TerminateStaleProvisioningCommand cmd, String sanitizedReason,
                                      OperatorContext operatorCtx) {
        String code = cmd.getReasonCode() == null ? "MANUAL_INCIDENT_RECOVERY" : cmd.getReasonCode().getValue();
        StringBuilder sb = new StringBuilder(code);
        String ref = auditRef(cmd);
        if (ref != null && !ref.isBlank()) {
            sb.append("|").append(ref);
        }
        if (sanitizedReason != null && !sanitizedReason.isBlank()) {
            sb.append("|").append(sanitizedReason);
        }
        String value = sb.toString();
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String buildSanitizedReasonWithAuditRef(String sanitizedReason, TerminateStaleProvisioningCommand cmd,
                                                    OperatorContext operatorCtx) {
        return buildCompactReason(cmd, sanitizedReason, operatorCtx);
    }

    private String auditRef(TerminateStaleProvisioningCommand cmd) {
        StringBuilder sb = new StringBuilder();
        if (cmd.getMaintenanceRequestId() != null && !cmd.getMaintenanceRequestId().isBlank()) {
            sb.append("op=").append(cmd.getMaintenanceRequestId());
        }
        if (cmd.getTicketNo() != null && !cmd.getTicketNo().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(";");
            }
            sb.append("ticket=").append(cmd.getTicketNo());
        }
        return sb.toString();
    }

    /**
     * 结构化审计日志（非敏感；持久化审计由 MPT 层 framework-audit-starter @Log 承载）。
     */
    private void logAudit(BusinessKeyProvisioning batch, TerminateStaleProvisioningCommand cmd,
                          OperatorContext operatorCtx, String fromState, int failedPendingKeyCount) {
        log.info("AUDIT|biz=BusinessKeyProvisioningTerminate|maintenanceRequestId={}|requestId={}|deviceSn={}|domain={}|"
                        + "operatorId={}|operatorName={}|reasonCode={}|sanitizedReason={}|ticketNo={}|beforeStatus={}|afterStatus={}|"
                        + "failedPendingKeyCount={}|gateDecision=PASSED",
                cmd.getMaintenanceRequestId(), batch.getRequestId(), batch.getDeviceSn(), batch.getBusinessDomain(),
                operatorCtx != null ? operatorCtx.getOperatorId() : null,
                operatorCtx != null ? operatorCtx.getOperatorName() : null,
                cmd.getReasonCode() == null ? null : cmd.getReasonCode().getValue(),
                sanitizeReason(cmd.getReason()),
                cmd.getTicketNo(),
                fromState,
                batch.getStatus() == null ? null : batch.getStatus().getValue(),
                failedPendingKeyCount);
    }

    /**
     * 自由文本清理：换行清理、长度限制、敏感信息过滤（RD-065-3 §7.1）。
     */
    String sanitizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String value = reason.replaceAll("[\\r\\n\\t]+", " ").trim();
        value = value.replaceAll("\\s{2,}", " ");
        value = SENSITIVE_TOKEN_PATTERN.matcher(value).replaceAll("$1=[REDACTED]");
        value = VIN_PATTERN.matcher(value).replaceAll("[VIN-REDACTED]");
        value = HEX_BLOB_PATTERN.matcher(value).replaceAll("[HEX-REDACTED]");
        value = BASE64_BLOB_PATTERN.matcher(value).replaceAll("[BASE64-REDACTED]");
        if (value.length() > REASON_MAX_LENGTH) {
            value = value.substring(0, REASON_MAX_LENGTH);
        }
        return value;
    }
}
