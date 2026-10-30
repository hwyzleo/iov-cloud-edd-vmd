package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayVehicleImportEventCmd;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PreviewStaleException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private final PartInfoRepository partInfoRepository;
    private final PartDeviceCategorySnapshotResolver partDeviceCategorySnapshotResolver;

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
     * 预检：读取冻结载荷、识别候选、生成计划摘要（CR-072：REIMPORT 计划含类别计划，digest 确定性编码）
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
        rejectUnsupportedCorrection(request);
        Map<String, Object> plan = buildPlan(request, scope, importData);
        String planJson = ImportPlanCoder.toCanonicalJson(plan);
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
     * CR-072 预览安全：REIMPORT 必须关联批准计划（planDigest），客户端省略 digest 不能跳过校验；
     * 执行期重扫与预览摘要不一致 → PREVIEW_STALE；执行仅提交获批计划候选，不扩展范围。
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

        // 2.5 REIMPORT：批准计划校验（重扫 + 摘要比对 + 存储获批计划）
        Object importData = loadImportData(request.getDomain(), request.getImportDataId());
        String planJsonToStore = null;
        byte[] planDigestToStore = null;
        List<String> effectiveScope = request.getScope();
        if (MODE_REIMPORT.equals(request.getMode())) {
            rejectUnsupportedCorrection(new PreviewRequest(request.getDomain(), request.getImportDataId(),
                    request.getMode(), request.getScope(), request.getRepairPolicy()));
            if (request.getPlanDigest() == null || request.getPlanDigest().isBlank()) {
                throw new OperationNotAllowedException("REIMPORT 需先预览并提交 planDigest（服务端批准计划），不能跳过预览校验");
            }
            List<String> candidates = extractCandidates(request.getDomain(), importData);
            List<String> scope = new ArrayList<>(candidates);
            if (request.getScope() != null && !request.getScope().isEmpty()) {
                scope.retainAll(request.getScope());
            }
            if (scope.isEmpty()) {
                throw new OperationNotAllowedException("候选范围内无匹配项");
            }
            Map<String, Object> plan = buildPlan(new PreviewRequest(request.getDomain(), request.getImportDataId(),
                    request.getMode(), request.getScope(), request.getRepairPolicy()), scope, importData);
            String planJson = ImportPlanCoder.toCanonicalJson(plan);
            byte[] digest = sha256(planJson);
            if (!java.util.Arrays.equals(digest, decodeHex(request.getPlanDigest()))) {
                throw new PreviewStaleException("预览计划已过期：执行期重扫与预览摘要不一致，请重新预览");
            }
            planJsonToStore = planJson;
            planDigestToStore = digest;
            effectiveScope = planCandidates(plan);
        }

        // 3. 创建操作（PENDING；operationId 复用调用方提供值以支持幂等重试）
        ImportOperation operation = ImportOperation.builder()
                .operationId(request.getOperationId() != null
                        ? request.getOperationId() : UUID.randomUUID().toString())
                .domain(request.getDomain())
                .operationType(request.getMode())
                .importDataId(request.getImportDataId())
                .batchNum(extractBatchNum(request.getDomain(), importData))
                .payloadDigest(sha256(importData.toString()))
                .requestDigest(decodeHex(requestDigest))
                .planDigest(planDigestToStore != null ? planDigestToStore
                        : decodeHex(request.getPlanDigest() != null ? request.getPlanDigest() : ""))
                .planJson(planJsonToStore != null ? planJsonToStore
                        : buildPlanJson(new PreviewRequest(request.getDomain(), request.getImportDataId(),
                                request.getMode(), request.getScope(), request.getRepairPolicy()),
                                extractCandidates(request.getDomain(), importData)))
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
                        ? importReimportExecutor.executeVehicle(operation, effectiveScope, ctx)
                        : importReimportExecutor.executePart(operation, effectiveScope, ctx);
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
     * 重试（CR-072 预览安全）：按 operationId 加载原冻结计划与检查点，只处理未提交条目；
     * 已提交条目按检查点返回原结果（不因自身 rowVersion 变化误判陈旧），未提交条目仍验证原批准条件。
     * 不再重新生成计划/悄悄扩大范围；老操作无类别计划时也不自动获得新增类别补缺能力。
     */
    public ExecuteResult retry(String operationId, String operatorId) {
        ImportOperation operation = importOperationRepository.selectByOperationId(operationId);
        if (operation == null) {
            throw new OperationNotAllowedException("操作不存在: " + operationId);
        }
        // CR-072：允许 FAILED_RETRYABLE 条目所在的 PARTIAL_FAILED 状态按检查点恢复
        if (!List.of(STATUS_PENDING, STATUS_FAILED, STATUS_PARTIAL_FAILED).contains(operation.getStatus())) {
            throw new OperationNotAllowedException("当前状态[" + operation.getStatus() + "]不可重试");
        }
        // 冻结计划（服务端已保存；不重新生成）
        Map<String, Object> plan = parsePlan(operation.getPlanJson());
        List<String> candidates = planCandidates(plan);
        if (candidates.isEmpty()) {
            throw new OperationNotAllowedException("操作缺少冻结计划候选，无法重试");
        }
        // 检查点：已提交（SUCCEEDED/NOOP/FAILED_FINAL）条目返回原结果；SKIPPED/FAILED_RETRYABLE/PENDING 恢复
        Set<String> committed = new HashSet<>();
        int committedChanged = 0, committedUnchanged = 0, committedFailed = 0;
        long cursor = 0L;
        List<ImportOperationItem> items;
        do {
            items = importOperationRepository.selectItems(operationId, null, cursor, 200);
            for (ImportOperationItem item : items) {
                if (List.of("SUCCEEDED", "NOOP", "FAILED_FINAL").contains(item.getStatus())) {
                    committed.add(item.getCandidateKey());
                    switch (item.getStatus()) {
                        case "SUCCEEDED" -> committedChanged++;
                        case "NOOP" -> committedUnchanged++;
                        case "FAILED_FINAL" -> committedFailed++;
                    }
                }
                cursor = item.getId();
            }
        } while (items.size() == 200);

        List<String> resumeScope = candidates.stream().filter(k -> !committed.contains(k)).toList();
        ImportReimportExecutor.ExecuteContext ctx = ImportReimportExecutor.ExecuteContext.builder()
                .operationId(operation.getOperationId())
                .operatorId(operatorId)
                .build();
        ImportReimportExecutor.ReimportOutcome outcome = DOMAIN_VEHICLE.equals(operation.getDomain())
                ? importReimportExecutor.executeVehicle(operation, resumeScope, ctx)
                : importReimportExecutor.executePart(operation, resumeScope, ctx);
        int changed = committedChanged + outcome.changed();
        int unchanged = committedUnchanged + outcome.unchanged();
        int skipped = outcome.skipped();
        int failed = committedFailed + outcome.failed();
        String status = failed > 0 ? STATUS_PARTIAL_FAILED : STATUS_SUCCEEDED;
        String countersJson = String.format("{\"changed\":%d,\"unchanged\":%d,\"skipped\":%d,\"failed\":%d}",
                changed, unchanged, skipped, failed);
        importOperationRepository.finish(operation.getOperationId(), WORKER_ID, status, countersJson);
        return toExecuteResult(importOperationRepository.selectByOperationId(operationId));
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

    // ==================== CR-072 类别计划（确定性编码 + 预览安全） ====================

    /**
     * 拒绝重导显式纠错（CORRECT_EXPLICIT，含 deviceCategory 服务器重算）：执行前明确拒绝、不部分执行；
     * 仅作用于重导预览/执行入口，既有其他字段纠错（如 modifyPartInfo APP 通道）不受影响。
     */
    private void rejectUnsupportedCorrection(PreviewRequest request) {
        if (MODE_REIMPORT.equals(request.getMode()) && "CORRECT_EXPLICIT".equals(request.getRepairPolicy())) {
            throw new OperationNotAllowedException("重导显式纠错（CORRECT_EXPLICIT，含 deviceCategory 服务器重算）第一阶段待实施"
                    + "（VMD-DSN-CR-072）；请使用 FILL_MISSING 补缺，既有其他字段纠错能力不受影响");
        }
    }

    /**
     * 构建确定性计划（含 REIMPORT 类别计划）；运行时间（resolvedAt/plannedAt）不进入计划内容
     */
    private Map<String, Object> buildPlan(PreviewRequest request, List<String> scope, Object importData) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("schemaVersion", ImportPlanCoder.SCHEMA_VERSION);
        plan.put("domain", request.getDomain());
        plan.put("importDataId", request.getImportDataId());
        plan.put("mode", request.getMode());
        plan.put("repairPolicy", request.getRepairPolicy() != null ? request.getRepairPolicy() : "FILL_MISSING");
        List<String> sortedCandidates = ImportPlanCoder.sortedCopy(scope);
        plan.put("candidates", sortedCandidates);
        if (MODE_REIMPORT.equals(request.getMode())) {
            plan.put("categoryPlan", buildCategoryPlan(request.getDomain(), importData, sortedCandidates));
        }
        return plan;
    }

    /**
     * 类别计划：PART 域逐候选条目；VEHICLE 域按 VIN 聚合（ECU_BASELINE 合法零件候选），VIN 有效 = 全部零件条目有效
     */
    private Map<String, Object> buildCategoryPlan(String domain, Object importData, List<String> candidates) {
        Map<String, Object> categoryPlan = new TreeMap<>();
        if (DOMAIN_PART.equals(domain)) {
            cn.hutool.json.JSONObject dataJson = cn.hutool.json.JSONUtil.parseObj(((PartImportData) importData).getData());
            cn.hutool.json.JSONObject request = dataJson.getJSONObject("REQUEST");
            cn.hutool.json.JSONObject data = request != null ? request.getJSONObject("DATA") : dataJson;
            cn.hutool.json.JSONArray items = data.getJSONArray("ITEMS");
            if (items != null) {
                for (Object item : items) {
                    cn.hutool.json.JSONObject itemJson = cn.hutool.json.JSONUtil.parseObj(item);
                    String partCode = itemJson.getStr("PART_CODE") != null ? itemJson.getStr("PART_CODE")
                            : itemJson.getStr("ASSEMBLY_PART_NO");
                    String sn = itemJson.getStr("SN");
                    String key = partCode + ":" + sn;
                    if (!candidates.contains(key)) {
                        continue;
                    }
                    categoryPlan.put(key, buildCategoryEntry(partCode, sn, itemJson.getStr("VEHICLE_NODE_CODE"),
                            partInfoRepository.selectByPartCodeAndSn(partCode, sn)));
                }
            }
        } else if (DOMAIN_VEHICLE.equals(domain)) {
            cn.hutool.json.JSONObject dataJson = cn.hutool.json.JSONUtil.parseObj(((VehImportData) importData).getData());
            cn.hutool.json.JSONObject data = dataJson.getJSONObject("REQUEST") != null
                    ? dataJson.getJSONObject("REQUEST").getJSONObject("DATA") : dataJson;
            cn.hutool.json.JSONArray items = data.getJSONArray("ITEMS");
            if (items != null) {
                for (Object item : items) {
                    cn.hutool.json.JSONObject itemJson = cn.hutool.json.JSONUtil.parseObj(item);
                    String vin = itemJson.getStr("VIN");
                    if (vin == null || vin.isBlank() || !candidates.contains(vin)) {
                        continue;
                    }
                    Map<String, Object> parts = new TreeMap<>();
                    cn.hutool.json.JSONArray ecuBaseline = itemJson.getJSONArray("ECU_BASELINE");
                    boolean allValid = true;
                    if (ecuBaseline != null) {
                        for (Object ecuObj : ecuBaseline) {
                            cn.hutool.json.JSONObject ecu = cn.hutool.json.JSONUtil.parseObj(ecuObj);
                            String sn = ecu.getStr("SN");
                            String assemblyPartNo = ecu.getStr("ASSEMBLY_PART_NO");
                            String vehicleNode = ecu.getStr("VEHICLE_NODE");
                            if (sn == null || sn.isBlank() || assemblyPartNo == null || assemblyPartNo.isBlank()) {
                                continue;
                            }
                            String key = assemblyPartNo + ":" + sn;
                            Map<String, Object> entry = buildCategoryEntry(assemblyPartNo, sn, vehicleNode,
                                    partInfoRepository.selectByPartCodeAndSn(assemblyPartNo, sn));
                            parts.put(key, entry);
                            if (!Boolean.TRUE.equals(entry.get("candidateValid"))) {
                                allValid = false;
                            }
                        }
                    }
                    Map<String, Object> vinEntry = new LinkedHashMap<>();
                    vinEntry.put("parts", parts);
                    vinEntry.put("candidateValid", allValid);
                    if (!allValid) {
                        vinEntry.put("invalidReason", "PART_PLAN_INVALID");
                    }
                    categoryPlan.put(vin, vinEntry);
                }
            }
        }
        return categoryPlan;
    }

    /**
     * 单候选类别计划条目：finalNode（合并后）/expectedCurrentCategory（显式 null=要求当前为 NULL）/
     * plannedCategory/expectedRowVersion（null=新实例）/expectsNewInstance/节点版本或内容指纹；
     * 预览期已确认无效（节点冲突/解析失败）以 candidateValid=false + invalidReason 呈现，执行期不静默跳过。
     */
    private Map<String, Object> buildCategoryEntry(String partCode, String sn, String payloadNode, PartInfo current) {
        Map<String, Object> e = new LinkedHashMap<>();
        String currentCategory = current != null ? current.getDeviceCategory() : null;
        String currentNode = current != null ? current.getVehicleNodeCode() : null;
        String finalNode;
        String invalidReason = null;
        if (payloadNode == null || payloadNode.isBlank()) {
            finalNode = currentNode; // 未提供：不清节点
        } else if (currentNode != null && !currentNode.equals(payloadNode)) {
            finalNode = currentNode; // 节点冲突：执行期拒绝，不借类别纠错换节点
            invalidReason = "NODE_CONFLICT";
        } else {
            finalNode = payloadNode;
        }
        e.put("finalNode", finalNode);
        e.put("expectedCurrentCategory", currentCategory);
        e.put("expectedRowVersion", current != null ? current.getRowVersion() : null);
        e.put("expectsNewInstance", current == null);
        String plannedCategory = null;
        if (invalidReason == null) {
            if (finalNode != null && (currentCategory == null || currentCategory.isBlank())) {
                e.put("fillPlanned", true);
                try {
                    PartDeviceCategorySnapshotResolver.ResolvedCategory rc = partDeviceCategorySnapshotResolver.resolve(finalNode);
                    plannedCategory = rc.deviceCategory();
                    if (rc.externalVersion() != null) {
                        e.put("nodeExternalVersion", rc.externalVersion());
                    }
                    e.put("nodeFingerprint", ImportPlanCoder.nodeFingerprint(finalNode, rc.deviceCategory()));
                } catch (DeviceCategoryResolveException ex) {
                    invalidReason = "RESOLVE_FAILED:" + (ex.isRetryable() ? "RETRYABLE" : "FINAL")
                            + ":" + ex.getMessage();
                }
            } else if (finalNode != null) {
                e.put("fillPlanned", false);
                plannedCategory = currentCategory; // 已有类别：FILL_MISSING 不重写
                e.put("nodeFingerprint", ImportPlanCoder.nodeFingerprint(finalNode, currentCategory));
            }
        }
        e.put("plannedCategory", plannedCategory);
        e.put("candidateValid", invalidReason == null);
        if (invalidReason != null) {
            e.put("invalidReason", invalidReason);
        }
        return e;
    }

    @SuppressWarnings("unchecked")
    private List<String> planCandidates(Map<String, Object> plan) {
        Object candidates = plan.get("candidates");
        if (!(candidates instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object c : list) {
            result.add(String.valueOf(c));
        }
        return result;
    }

    /**
     * 解析冻结计划（retry 用）；老操作无 schemaVersion/类别计划时返回粗粒度候选，executor 按无类别计划处理
     */
    private Map<String, Object> parsePlan(String planJson) {
        if (planJson == null || planJson.isBlank()) {
            return Map.of();
        }
        try {
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(planJson);
            Map<String, Object> map = new LinkedHashMap<>();
            obj.forEach(map::put);
            return map;
        } catch (Exception e) {
            log.warn("操作冻结计划解析失败: {}", e.getMessage());
            return Map.of();
        }
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
