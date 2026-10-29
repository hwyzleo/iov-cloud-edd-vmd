package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayVehicleImportEventCmd;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一导入操作应用服务（CR-071 §3/§8）
 * <p>
 * 管理后台统一操作门面：REPLAY 仅补发已登记 Kafka 事件；REIMPORT 进入导入内核
 * 修复事实。服务端持久化预览计划、摘要幂等、同 domain+importData 互斥、
 * 租约与 fence 并发控制。REPLAY 委托既有补发服务并记录 legacyReplayId。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportOperationAppService {

    private final ImportOperationRepository importOperationRepository;
    private final VehImportDataRepository vehImportDataRepository;
    private final PartImportDataRepository partImportDataRepository;
    private final ImportReimportExecutor importReimportExecutor;
    private final VehImportEventReplayAppService vehImportEventReplayAppService;
    private final PartImportPostProcessReplayAppService partImportPostProcessReplayAppService;

    public static final String DOMAIN_VEHICLE = "VEHICLE";
    public static final String DOMAIN_PART = "PART";
    public static final String MODE_REIMPORT = "REIMPORT";
    public static final String MODE_REPLAY = "REPLAY";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_PARTIAL_FAILED = "PARTIAL_FAILED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RECOVERING = "RECOVERING";

    /** 租约时长（秒） */
    private static final int LEASE_SECONDS = 60;

    /** 执行器标识 */
    private static final String WORKER_ID = "import-worker-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * 预检：读取冻结载荷、识别候选、生成计划摘要
     */
    public PreviewResult preview(PreviewRequest request) {
        Object importData = loadImportData(request.getDomain(), request.getImportDataId());
        List<String> candidates = extractCandidates(request.getDomain(), importData);
        if (candidates.isEmpty()) {
            throw new OperationNotAllowedException("无法从原导入记录识别候选（" + request.getDomain() + "）");
        }
        // 范围过滤
        List<String> scope = new ArrayList<>(candidates);
        if (request.getScope() != null && !request.getScope().isEmpty()) {
            scope.retainAll(request.getScope());
        }
        if (scope.isEmpty()) {
            throw new OperationNotAllowedException("候选范围内无匹配项");
        }
        String planJson = buildPlanJson(request, scope);
        String planDigest = hex(sha256(planJson));
        log.info("导入操作预检: domain={}, importDataId={}, mode={}, candidates={}, planDigest={}",
                request.getDomain(), request.getImportDataId(), request.getMode(), scope.size(), planDigest);
        return PreviewResult.builder()
                .planDigest(planDigest)
                .candidates(scope)
                .candidateCount(scope.size())
                .messageEstimate(scope.size() * 2L)
                .build();
    }

    /**
     * 执行操作（幂等：同 operationId 同 requestDigest 返回原结果）
     */
    public ExecuteResult execute(ExecuteRequest request) {
        // 1. 摘要校验与幂等
        String requestDigest = hex(sha256(buildRequestRaw(request)));
        ImportOperation existing = request.getOperationId() != null
                ? importOperationRepository.selectByOperationId(request.getOperationId()) : null;
        if (existing != null) {
            if (!java.util.Arrays.equals(existing.getRequestDigest(), decodeHex(requestDigest))) {
                throw new IdempotencyConflictException("operationId[" + request.getOperationId() + "]请求摘要冲突");
            }
            return toExecuteResult(existing);
        }

        // 2. 互斥检查：同 domain+importData 不得并发 active 操作
        ImportOperation active = importOperationRepository.selectActiveByDomainAndImportDataId(
                request.getDomain(), request.getImportDataId());
        if (active != null) {
            throw new InProgressException("同 domain+importData 存在进行中操作[" + active.getOperationId() + "]");
        }

        // 3. 创建操作（PENDING；operationId 复用调用方提供值以支持幂等重试）
        Object importData = loadImportData(request.getDomain(), request.getImportDataId());
        ImportOperation operation = ImportOperation.builder()
                .operationId(request.getOperationId() != null
                        ? request.getOperationId() : UUID.randomUUID().toString())
                .domain(request.getDomain())
                .operationType(request.getMode())
                .importDataId(request.getImportDataId())
                .batchNum(extractBatchNum(request.getDomain(), importData))
                .payloadDigest(sha256(importData.toString()))
                .requestDigest(decodeHex(requestDigest))
                .planDigest(decodeHex(request.getPlanDigest() != null ? request.getPlanDigest() : ""))
                .planJson(buildPlanJson(new PreviewRequest(request.getDomain(), request.getImportDataId(),
                        request.getMode(), request.getScope(), request.getRepairPolicy()), extractCandidates(request.getDomain(), importData)))
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .operationSource("MPT_" + request.getMode())
                .reason(request.getReason() != null ? request.getReason() : request.getMode())
                .status(STATUS_PENDING)
                .fenceToken(0L)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        importOperationRepository.insert(operation);
        operation = importOperationRepository.selectByOperationId(operation.getOperationId());

        // 4. 认领并执行
        int claimed = importOperationRepository.claim(operation.getOperationId(), WORKER_ID,
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(LEASE_SECONDS));
        if (claimed == 0) {
            throw new InProgressException("操作认领失败（并发）");
        }

        int changed = 0, unchanged = 0, skipped = 0, failed = 0;
        String status = STATUS_SUCCEEDED;
        String legacyReplayId = null;
        try {
            ImportReimportExecutor.ExecuteContext ctx = ImportReimportExecutor.ExecuteContext.builder()
                    .operationId(operation.getOperationId())
                    .operatorId(request.getOperatorId())
                    .operatorName(request.getOperatorName())
                    .build();
            if (MODE_REIMPORT.equals(request.getMode())) {
                ImportReimportExecutor.ReimportOutcome outcome = DOMAIN_VEHICLE.equals(request.getDomain())
                        ? importReimportExecutor.executeVehicle(operation, request.getScope(), ctx)
                        : importReimportExecutor.executePart(operation, request.getScope(), ctx);
                changed = outcome.changed();
                unchanged = outcome.unchanged();
                skipped = outcome.skipped();
                failed = outcome.failed();
                status = failed > 0 ? STATUS_PARTIAL_FAILED : STATUS_SUCCEEDED;
            } else if (MODE_REPLAY.equals(request.getMode())) {
                legacyReplayId = executeReplay(request);
                status = legacyReplayId != null ? STATUS_SUCCEEDED : STATUS_FAILED;
            } else {
                throw new OperationNotAllowedException("不支持的 mode: " + request.getMode());
            }
        } catch (Exception e) {
            log.error("导入操作执行异常: operationId={}, error={}",
                    operation.getOperationId(), e.getMessage(), e);
            status = STATUS_FAILED;
            failed = 1;
        }

        // 5. 完成
        String countersJson = String.format("{\"changed\":%d,\"unchanged\":%d,\"skipped\":%d,\"failed\":%d}",
                changed, unchanged, skipped, failed);
        importOperationRepository.finish(operation.getOperationId(), WORKER_ID, status, countersJson);
        if (legacyReplayId != null) {
            // 同步终态后整体更新（避免 updateById 用内存旧 status 覆盖 finish 结果）
            operation.setStatus(status);
            operation.setLegacyReplayId(legacyReplayId);
            importOperationRepository.update(operation);
        }

        ImportOperation finalOp = importOperationRepository.selectByOperationId(operation.getOperationId());
        return toExecuteResult(finalOp);
    }

    /**
     * 重试（仅 PENDING/RECOVERING/FAILED_RETRYABLE 语义；当前实现重新执行同摘要）
     */
    public ExecuteResult retry(String operationId, String operatorId) {
        ImportOperation operation = importOperationRepository.selectByOperationId(operationId);
        if (operation == null) {
            throw new OperationNotAllowedException("操作不存在: " + operationId);
        }
        if (!List.of(STATUS_PENDING, STATUS_FAILED).contains(operation.getStatus())) {
            throw new OperationNotAllowedException("当前状态[" + operation.getStatus() + "]不可重试");
        }
        ExecuteRequest request = ExecuteRequest.builder()
                .domain(operation.getDomain())
                .importDataId(operation.getImportDataId())
                .mode(operation.getOperationType())
                .scope(null)
                .repairPolicy("FILL_MISSING")
                .reason("retry:" + operation.getReason())
                .operationId(operation.getOperationId())
                .operatorId(operatorId)
                .build();
        return execute(request);
    }

    public ImportOperation queryOperation(String operationId) {
        return importOperationRepository.selectByOperationId(operationId);
    }

    public List<ImportOperationItem> queryItems(String operationId, String status, Long cursor, int limit) {
        return importOperationRepository.selectItems(operationId, status, cursor, limit);
    }

    // ==================== 内部 ====================

    private Object loadImportData(String domain, Long importDataId) {
        if (DOMAIN_VEHICLE.equals(domain)) {
            VehImportData data = vehImportDataRepository.selectById(importDataId);
            if (data == null) {
                throw new OperationNotAllowedException("车辆导入数据不存在: " + importDataId);
            }
            return data;
        }
        PartImportData data = partImportDataRepository.selectById(importDataId);
        if (data == null) {
            throw new OperationNotAllowedException("零件导入数据不存在: " + importDataId);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCandidates(String domain, Object importData) {
        List<String> candidates = new ArrayList<>();
        try {
            cn.hutool.json.JSONObject dataJson;
            if (importData instanceof VehImportData veh) {
                dataJson = cn.hutool.json.JSONUtil.parseObj(veh.getData());
                cn.hutool.json.JSONObject data = dataJson.getJSONObject("REQUEST") != null
                        ? dataJson.getJSONObject("REQUEST").getJSONObject("DATA") : dataJson;
                cn.hutool.json.JSONArray items = data.getJSONArray("ITEMS");
                if (items != null) {
                    for (Object item : items) {
                        String vin = cn.hutool.json.JSONUtil.parseObj(item).getStr("VIN");
                        if (vin != null && !vin.isBlank()) {
                            candidates.add(vin);
                        }
                    }
                }
            } else if (importData instanceof PartImportData part) {
                dataJson = cn.hutool.json.JSONUtil.parseObj(part.getData());
                cn.hutool.json.JSONObject data = dataJson.getJSONObject("REQUEST") != null
                        ? dataJson.getJSONObject("REQUEST").getJSONObject("DATA") : dataJson;
                cn.hutool.json.JSONArray items = data.getJSONArray("ITEMS");
                if (items != null) {
                    for (Object item : items) {
                        cn.hutool.json.JSONObject itemJson = cn.hutool.json.JSONUtil.parseObj(item);
                        String partCode = itemJson.getStr("PART_CODE") != null ? itemJson.getStr("PART_CODE")
                                : itemJson.getStr("ASSEMBLY_PART_NO");
                        String sn = itemJson.getStr("SN");
                        if (partCode != null && !partCode.isBlank() && sn != null && !sn.isBlank()) {
                            candidates.add(partCode + ":" + sn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("候选提取失败: domain={}, error={}", domain, e.getMessage());
        }
        return candidates;
    }

    private String extractBatchNum(String domain, Object importData) {
        return importData instanceof VehImportData veh ? veh.getBatchNum()
                : importData instanceof PartImportData part ? part.getBatchNum() : null;
    }

    private String buildPlanJson(PreviewRequest request, List<String> scope) {
        return cn.hutool.json.JSONUtil.toJsonStr(Map.of(
                "domain", request.getDomain(),
                "importDataId", request.getImportDataId(),
                "mode", request.getMode(),
                "repairPolicy", request.getRepairPolicy() != null ? request.getRepairPolicy() : "FILL_MISSING",
                "candidates", scope,
                "plannedAt", LocalDateTime.now(ZoneOffset.UTC).toString()));
    }

    private String buildRequestRaw(ExecuteRequest request) {
        return String.join(":",
                request.getDomain(), String.valueOf(request.getImportDataId()), request.getMode(),
                request.getScope() != null ? String.join(",", request.getScope()) : "*",
                request.getRepairPolicy() != null ? request.getRepairPolicy() : "FILL_MISSING",
                request.getReason() != null ? request.getReason() : "",
                request.getOperatorId() != null ? request.getOperatorId() : "");
    }

    /**
     * REPLAY 委托既有补发服务（§8.3 事件白名单：仅事件类动作，不执行 lifecycle/sync/安全补偿）
     */
    private String executeReplay(ExecuteRequest request) {
        try {
            if (DOMAIN_VEHICLE.equals(request.getDomain())) {
                ReplayVehicleImportEventCmd cmd = ReplayVehicleImportEventCmd.builder()
                        .requestId(request.getOperationId())
                        .reason(request.getReason())
                        // CR-071 §8.3：新统一 REPLAY 仅事件动作（不含 *LIFECYCLE_ENSURE）
                        .actionTypes(List.of(
                                net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject
                                        .VehicleImportReplayActionType.BINDING_EVENT_REPLAY.getValue(),
                                net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject
                                        .VehicleImportReplayActionType.SOFTWARE_INVENTORY_EVENT_REPLAY.getValue()))
                        .build();
                var result = vehImportEventReplayAppService.replay(
                        request.getImportDataId(), cmd, request.getOperatorId(), request.getOperatorName());
                return result != null ? result.getReplayId() : null;
            }
            ReplayPartImportPostProcessCmd cmd = ReplayPartImportPostProcessCmd.builder()
                    .requestId(request.getOperationId())
                    .reason(request.getReason())
                    // CR-071 §8.3：零件域统一 REPLAY 仅入站跨域事件（不含 TSP/OTA/IDK/SECURITY_PRESET）
                    .scope(List.of(net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject
                            .PartPostProcessActionType.PART_INBOUND_EVENT.getValue()))
                    .build();
            var result = partImportPostProcessReplayAppService.replay(
                    request.getImportDataId(), cmd, request.getOperatorId(), request.getOperatorName());
            return result != null ? result.getReplayId() : null;
        } catch (Exception e) {
            log.warn("REPLAY 委托失败: domain={}, error={}", request.getDomain(), e.getMessage());
            return null;
        }
    }

    private ExecuteResult toExecuteResult(ImportOperation operation) {
        return ExecuteResult.builder()
                .operationId(operation.getOperationId())
                .status(operation.getStatus())
                .counters(operation.getCountersJson())
                .legacyReplayId(operation.getLegacyReplayId())
                .build();
    }

    private byte[] sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private byte[] decodeHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return new byte[32];
        }
        return HexFormat.of().parseHex(hex);
    }

    // ==================== DTO ====================

    @Getter
    @Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PreviewRequest {
        private String domain;
        private Long importDataId;
        private String mode;
        private List<String> scope;
        private String repairPolicy;
    }

    @Getter
    @Builder
    public static class PreviewResult {
        private final String planDigest;
        private final List<String> candidates;
        private final int candidateCount;
        private final long messageEstimate;
    }

    @Getter
    @Builder
    public static class ExecuteRequest {
        private final String domain;
        private final Long importDataId;
        private final String mode;
        private final List<String> scope;
        private final String repairPolicy;
        private final String planDigest;
        private final String reason;
        private final String operationId;
        private final String operatorId;
        private final String operatorName;
    }

    @Getter
    @Builder
    public static class ExecuteResult {
        private final String operationId;
        private final String status;
        private final String counters;
        private final String legacyReplayId;
    }

    // ==================== 异常 ====================

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }

    public static class InProgressException extends RuntimeException {
        public InProgressException(String message) {
            super(message);
        }
    }

    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String message) {
            super(message);
        }
    }
}
