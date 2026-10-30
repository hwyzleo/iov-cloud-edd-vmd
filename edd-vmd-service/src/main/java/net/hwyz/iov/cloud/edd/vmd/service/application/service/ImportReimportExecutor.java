package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehiclePartBinder;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNodeConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PreviewStaleException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 导入重导执行器（CR-071 §6.1/§8.1）
 * <p>
 * 按原批次定向重导：读取冻结的原始载荷，对候选执行
 * 车辆 basic 补缺 → 实例/同件绑定修复 → 软件实装消解 → SSOT 投影，
 * 全程 DATA_REPAIR 副作用策略（不触发生命周期/事件/安全命令重放）。
 * 业务写入经统一 mutation 服务与 applyManifest；每候选落逐项审计（item）。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportReimportExecutor {

    private final VehImportDataRepository vehImportDataRepository;
    private final PartImportDataRepository partImportDataRepository;
    private final VehiclePartBinder vehiclePartBinder;
    private final SoftwareInventoryAppService softwareInventoryAppService;
    private final VehicleInfoMutationAppService vehicleInfoMutationAppService;
    private final ImportOperationRepository importOperationRepository;
    private final net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService vehiclePartAppService;
    private final net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService partInboundAppService;
    private final net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSoftwareSnapshotProjectionService softwareSnapshotProjectionService;
    private final net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository partInfoRepository;
    private final net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService vehicleNodeAppService;

    /** CR-072 功能开关：默认关闭；关闭期间重导补缺条目标未执行，不新增类别写入（启动时读取） */
    @Value("${vmd.part.device-category.enabled:false}")
    private boolean deviceCategoryEnabled;

    /** 执行器标识（租约 owner 前缀） */
    private static final String OWNER_PREFIX = "reimport-worker";

    /**
     * 执行车辆域定向重导（PRODUCE/TOL/EOL）
     * CR-072：类别计划为 VIN 粒度（ECU_BASELINE 合法零件候选）；任一零件条目陈旧/未批准 → 该 VIN 无写入并标记预览陈旧
     *
     * @param operation 操作
     * @param scopeVins 候选 VIN 子集（null=整批）
     * @param context 执行上下文
     * @return 执行计数
     */
    public ReimportOutcome executeVehicle(ImportOperation operation, List<String> scopeVins, ExecuteContext context) {
        VehImportData importData = vehImportDataRepository.selectById(operation.getImportDataId());
        if (importData == null || importData.getData() == null) {
            return ReimportOutcome.failed("IMPORT_DATA_NOT_FOUND");
        }
        JSONObject dataJson = JSONUtil.parseObj(importData.getData());
        JSONObject data = dataJson.getJSONObject("REQUEST") != null
                ? dataJson.getJSONObject("REQUEST").getJSONObject("DATA") : dataJson;
        JSONArray items = data.getJSONArray("ITEMS");
        if (items == null || items.isEmpty()) {
            return ReimportOutcome.failed("NO_ITEMS");
        }

        PlanContext planCtx = PlanContext.of(operation);
        String batchNum = importData.getBatchNum();
        String type = importData.getType();
        int changed = 0, unchanged = 0, skipped = 0, failed = 0;

        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String vin = itemJson.getStr("VIN");
            if (vin == null || vin.isBlank()) {
                skipped++;
                continue;
            }
            if (scopeVins != null && !scopeVins.contains(vin)) {
                skipped++;
                continue;
            }
            try {
                if (planCtx.isHasCategoryPlan()) {
                    JSONObject vinEntry = planCtx.getCategoryPlan().getJSONObject(vin);
                    if (vinEntry == null) {
                        recordItem(operation, vin, "SKIPPED", context, "VEHICLE_BASIC",
                                "PREVIEW_STALE:VIN_NOT_IN_PLAN", null, null);
                        skipped++;
                        continue;
                    }
                    if (!Boolean.TRUE.equals(vinEntry.getBool("candidateValid", false))) {
                        recordItem(operation, vin, "FAILED_FINAL", context, "VEHICLE_BASIC",
                                "PART_PLAN_INVALID:" + vinEntry.getStr("invalidReason"),
                                net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED.getCode(), null);
                        failed++;
                        continue;
                    }
                    // 开关关闭：车辆重导以绑定修复为主，类别写入由 mutation 开关控制，不整体阻断（PART 域补缺才标未执行）
                    String stale = validateVinPartsAgainstPlan(vin, vinEntry);
                    if (stale != null) {
                        recordItem(operation, vin, "SKIPPED", context, "VEHICLE_BASIC",
                                "PREVIEW_STALE:" + stale,
                                net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode(), null);
                        skipped++;
                        continue;
                    }
                }
                String outcome = reimportVehicle(batchNum, type, vin, itemJson, context);
                switch (outcome) {
                    case "CHANGED" -> {
                        changed++;
                        recordItem(operation, vin, "SUCCEEDED", context, "VEHICLE_BASIC");
                    }
                    case "NOOP" -> {
                        unchanged++;
                        recordItem(operation, vin, "NOOP", context, "VEHICLE_BASIC");
                    }
                    default -> {
                        skipped++;
                        recordItem(operation, vin, "SKIPPED", context, "VEHICLE_BASIC");
                    }
                }
            } catch (PreviewStaleException e) {
                skipped++;
                recordItem(operation, vin, "SKIPPED", context, "VEHICLE_BASIC", "PREVIEW_STALE",
                        net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode(), e.getMessage());
            } catch (Exception e) {
                failed++;
                log.warn("重导车辆[{}]失败: {}", vin, e.getMessage(), e);
                recordItem(operation, vin, "FAILED_RETRYABLE", context, "VEHICLE_BASIC", null, null, e.getMessage());
            }
        }
        return new ReimportOutcome(changed, unchanged, skipped, failed);
    }

    /**
     * 单车辆重导：basic 补缺 → 绑定修复 → 软件观测（applyManifest 内投影刷新）
     */
    private String reimportVehicle(String batchNum, String type, String vin, JSONObject itemJson, ExecuteContext context) {
        // 1. 车辆 basic 补缺（FILL_MISSING；仅明确字段，产品树映射由既有解析器保证）
        String eolResult = itemJson.getStr("EOL_RESULT");
        if (eolResult != null && !eolResult.isBlank()) {
            VehicleInfoMutationAppService.MutationResult result = vehicleInfoMutationAppService.patch(
                    VehicleInfoPatchCmd.builder()
                            .vin(vin)
                            .eolResult(eolResult)
                            .repairPolicy(VehicleInfoMutationAppService.POLICY_FILL_MISSING)
                            .changeType("INBOUND")
                            .operationSource("MPT_REIMPORT")
                            .operatorId(context.getOperatorId())
                            .operatorName(context.getOperatorName())
                            .source(type)
                            .operationId(context.getOperationId())
                            .phase("BASIC")
                            .seq(1)
                            .build());
            if (result.isApplied()) {
                log.debug("重导车辆[{}]basic补缺已应用", vin);
            }
        }

        // 2. 绑定修复（同件补列/漏绑补绑；executor 不发布 VehicleEolPartBoundEvent）
        JSONArray ecuBaseline = itemJson.getJSONArray("ECU_BASELINE");
        if (ecuBaseline != null && !ecuBaseline.isEmpty()) {
            JSONArray bindParts = new JSONArray();
            for (Object ecuObj : ecuBaseline) {
                JSONObject ecu = JSONUtil.parseObj(ecuObj);
                String sn = ecu.getStr("SN");
                String assemblyPartNo = ecu.getStr("ASSEMBLY_PART_NO");
                String vehicleNode = ecu.getStr("VEHICLE_NODE");
                if (sn == null || sn.isBlank() || assemblyPartNo == null || assemblyPartNo.isBlank()
                        || vehicleNode == null || vehicleNode.isBlank()) {
                    continue;
                }
                JSONObject part = new JSONObject();
                part.set("VIN", vin);
                part.set("DEVICE_CODE", vehicleNode);
                part.set("PART_NO", assemblyPartNo);
                part.set("PART_SN", sn);
                part.set("HARDWARE_PN", ecu.getStr("HARDWARE_PART_NO"));
                part.set("HARDWARE_VERSION", ecu.getStr("HARDWARE_VERSION"));
                part.set("ICCID1", ecu.getStr("ICCID1"));
                part.set("ICCID2", ecu.getStr("ICCID2"));
                bindParts.add(part);
            }
            if (!bindParts.isEmpty()) {
                vehiclePartBinder.bindParts(bindParts, vin, batchNum);
            }
        }

        // 3. 软件观测（幂等键与既有 EOL 解析器一致：MD5(batchNum_vin_ecuSn_softwarePartNo)）
        if (ecuBaseline != null) {
            for (Object ecuObj : ecuBaseline) {
                JSONObject ecu = JSONUtil.parseObj(ecuObj);
                String ecuSn = ecu.getStr("SN");
                String vehicleNode = ecu.getStr("VEHICLE_NODE");
                JSONArray softwareList = ecu.getJSONArray("SOFTWARE");
                if (softwareList == null || ecuSn == null || ecuSn.isBlank() || vehicleNode == null || vehicleNode.isBlank()) {
                    continue;
                }
                // 绑定修复后反查实例ID（软件实装锚点）
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart binding =
                        vehiclePartAppService.findByVinAndPosition(vin, vehicleNode);
                if (binding == null || binding.getPartId() == null) {
                    log.debug("重导软件观测跳过: vin={}, node={} 无当前绑定", vin, vehicleNode);
                    continue;
                }
                Long partId = binding.getPartId();
                Long bindingId = binding.getId();
                for (Object swObj : softwareList) {
                    JSONObject sw = JSONUtil.parseObj(swObj);
                    String softwarePartNo = sw.getStr("SOFTWARE_PART_NO");
                    String softwareVersion = sw.getStr("SOFTWARE_VERSION");
                    String softwareType = sw.getStr("SOFTWARE_TYPE");
                    String flashResult = sw.getStr("FLASH_RESULT");
                    if (softwarePartNo == null || softwarePartNo.isBlank()
                            || softwareVersion == null || softwareVersion.isBlank()) {
                        continue;
                    }
                    try {
                        softwareInventoryAppService.applyManifest(
                                partId,
                                bindingId,
                                vin,
                                softwarePartNo,
                                softwarePartNo,
                                softwareVersion,
                                null,
                                null,
                                flashResult,
                                "UPDATE",
                                "EOL",
                                md5(batchNum + "_" + vin + "_" + ecuSn + "_" + softwarePartNo),
                                java.time.Instant.now(),
                                java.time.Instant.now(),
                                true,
                                null, null, null, null, null,
                                softwareType);
                    } catch (Exception e) {
                        log.warn("重导软件观测失败: vin={}, ecuSn={}, swPartNo={}, error={}",
                                vin, ecuSn, softwarePartNo, e.getMessage());
                    }
                }
            }
        }
        return "CHANGED";
    }

    /**
     * 执行零件域定向重导（§3.4 扁平键契约 → 入站幂等 upsert + 审计 → SSOT 投影刷新）
     * CR-072 预览安全：逐候选与冻结计划条目比对（实例版本/节点/类别/投影版本指纹），陈旧不写入并标记 PREVIEW_STALE；
     * 执行仅处理获批计划候选（EXTRA_CANDIDATE 显式记录）；老操作无类别计划不获得新增补缺能力。
     *
     * @param operation 操作
     * @param scopeKeys 候选 partCode:sn 子集（null=整批；由调用方按批准计划传入）
     * @param context 执行上下文
     * @return 执行计数
     */
    public ReimportOutcome executePart(ImportOperation operation, List<String> scopeKeys, ExecuteContext context) {
        PartImportData importData = partImportDataRepository.selectById(operation.getImportDataId());
        if (importData == null || importData.getData() == null) {
            return ReimportOutcome.failed("IMPORT_DATA_NOT_FOUND");
        }
        cn.hutool.json.JSONObject dataJson = cn.hutool.json.JSONUtil.parseObj(importData.getData());
        cn.hutool.json.JSONObject request = dataJson.getJSONObject("REQUEST");
        cn.hutool.json.JSONObject head = request != null ? request.getJSONObject("HEAD") : null;
        String supplier = head != null ? head.getStr("ACCOUNT") : null;
        cn.hutool.json.JSONObject data = request != null ? request.getJSONObject("DATA") : dataJson;
        cn.hutool.json.JSONArray items = data.getJSONArray("ITEMS");
        if (items == null || items.isEmpty()) {
            return ReimportOutcome.failed("NO_ITEMS");
        }

        PlanContext planCtx = PlanContext.of(operation);
        String batchNum = importData.getBatchNum();
        int changed = 0, unchanged = 0, skipped = 0, failed = 0;
        java.util.Set<String> commonFields = java.util.Set.of(
                "SN", "VIN", "VEHICLE_NODE_CODE", "PART_CODE", "ASSEMBLY_PART_NO", "SOURCE_EVENT_ID");

        for (Object item : items) {
            cn.hutool.json.JSONObject itemJson = cn.hutool.json.JSONUtil.parseObj(item);
            String partCode = itemJson.getStr("PART_CODE") != null ? itemJson.getStr("PART_CODE")
                    : itemJson.getStr("ASSEMBLY_PART_NO");
            String sn = itemJson.getStr("SN");
            String candidateKey = partCode + ":" + sn;
            if (partCode == null || partCode.isBlank() || sn == null || sn.isBlank()) {
                skipped++;
                continue;
            }
            if (scopeKeys != null && !scopeKeys.contains(candidateKey)) {
                if (planCtx.isHasCategoryPlan() && !planCtx.getApprovedCandidates().contains(candidateKey)) {
                    // 候选额外出现：不在批准计划中，显式记录（不静默跳过）
                    recordItem(operation, candidateKey, "SKIPPED", context, "PART_INSTANCE",
                            "EXTRA_CANDIDATE_NOT_IN_PLAN", null, null);
                }
                skipped++;
                continue;
            }
            try {
                // CR-072：冻结计划逐候选校验（预览安全）
                cn.hutool.json.JSONObject entry = planCtx.isHasCategoryPlan()
                        ? planCtx.getCategoryPlan().getJSONObject(candidateKey) : null;
                boolean deriveAllowed = true;
                if (planCtx.isHasCategoryPlan()) {
                    if (entry == null) {
                        recordItem(operation, candidateKey, "SKIPPED", context, "PART_INSTANCE",
                                "PREVIEW_STALE:NO_PLAN_ENTRY",
                                net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode(), null);
                        skipped++;
                        continue;
                    }
                    if (!Boolean.TRUE.equals(entry.getBool("candidateValid", false))) {
                        String invalidReason = entry.getStr("invalidReason");
                        if (invalidReason != null && invalidReason.startsWith("RESOLVE_FAILED:RETRYABLE")) {
                            recordItem(operation, candidateKey, "FAILED_RETRYABLE", context, "PART_INSTANCE",
                                    invalidReason, net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED.getCode(), null);
                            failed++;
                        } else {
                            recordItem(operation, candidateKey, "FAILED_FINAL", context, "PART_INSTANCE",
                                    invalidReason != null ? invalidReason : "CANDIDATE_INVALID",
                                    net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED.getCode(), null);
                            failed++;
                        }
                        continue;
                    }
                    if (!deviceCategoryEnabled) {
                        // 开关关闭：类别补缺条目标未执行（不标成功，不静默降级为部分提交）
                        recordItem(operation, candidateKey, "SKIPPED", context, "PART_INSTANCE",
                                "DEVICE_CATEGORY_DISABLED",
                                net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_DISABLED.getCode(), null);
                        skipped++;
                        continue;
                    }
                    String staleReason = validatePartAgainstPlan(partCode, sn, entry);
                    if (staleReason != null) {
                        recordItem(operation, candidateKey, "SKIPPED", context, "PART_INSTANCE",
                                "PREVIEW_STALE:" + staleReason,
                                net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode(), null);
                        skipped++;
                        continue;
                    }
                } else {
                    // 老操作（无类别计划）：不自动获得新增类别补缺能力
                    deriveAllowed = false;
                }

                // 1. 扁平键标准化（§3.4：大小写别名同值合并、异值拒绝）；类型特殊键小写入 extra
                java.util.Map<String, String> extraFields = new java.util.HashMap<>();
                for (String key : itemJson.keySet()) {
                    if (commonFields.contains(key)) {
                        continue;
                    }
                    String value = itemJson.getStr(key);
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    String lowerKey = key.toLowerCase();
                    String existing = extraFields.get(lowerKey);
                    if (existing != null && !existing.equals(value)) {
                        throw new net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartInboundValidateFailedException(
                                "ITEM大小写别名冲突: " + lowerKey);
                    }
                    extraFields.put(lowerKey, value);
                }

                // 2. 入站（幂等 upsert + 属性历史 + MDM ACTIVE 校验；软件扁平字段保留为入站证据）
                net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord record =
                        net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord.builder()
                                .partCode(partCode)
                                .sn(sn)
                                .vehicleNodeCode(itemJson.getStr("VEHICLE_NODE_CODE"))
                                .supplierCode(supplier)
                                .batchNum(batchNum)
                                .inboundBatchNo(batchNum)
                                .sourceEventId(itemJson.getStr("SOURCE_EVENT_ID"))
                                .extraFields(extraFields.isEmpty() ? null : extraFields)
                                .deriveDeviceCategory(deriveAllowed)
                                .expectedPlannedCategory(planCtx.isHasCategoryPlan() ? entry.getStr("plannedCategory") : null)
                                .expectedRowVersion(planCtx.isHasCategoryPlan() && entry.getInt("expectedRowVersion") != null
                                        ? entry.getInt("expectedRowVersion") : null)
                                .requireNewInstance(planCtx.isHasCategoryPlan()
                                        && Boolean.TRUE.equals(entry.getBool("expectsNewInstance", false)))
                                .operationId(context.getOperationId())
                                .operationSource("MPT_REIMPORT")
                                .operatorId(context.getOperatorId())
                                .operatorName(context.getOperatorName())
                                .reason(operation.getReason())
                                .build();
                net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult inbound =
                        partInboundAppService.processInbound(java.util.List.of(record),
                                net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType.MANUAL,
                                itemJson.getStr("VIN"));
                boolean ok = inbound.getFailureCount() == 0 && inbound.getInvalidCount() == 0;

                // 3. SSOT 投影刷新（基于软件实装时态表；扁平字段不升级 confirmed）
                if (ok) {
                    net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo =
                            partInfoRepository.selectByPartCodeAndSn(partCode, sn);
                    if (partInfo != null) {
                        softwareSnapshotProjectionService.refreshFromCurrentSsot(partInfo.getId(),
                                PartSoftwareSnapshotProjectionService.ProjectionContext.builder()
                                        .operationId(operation.getOperationId())
                                        .operationSource("MPT_REIMPORT")
                                        .operatorId(context.getOperatorId())
                                        .operatorName(context.getOperatorName())
                                        .source("MANUAL")
                                        .seq(1)
                                        .build());
                    }
                    changed++;
                    recordItem(operation, candidateKey, "SUCCEEDED", context, "PART_INSTANCE");
                } else {
                    failed++;
                    log.warn("零件域重导入站失败: partCode={}, sn={}, errors={}", partCode, sn, inbound.getErrors());
                    recordItem(operation, candidateKey, "FAILED_RETRYABLE", context, "PART_INSTANCE",
                            "INBOUND_FAILED", null,
                            inbound.getErrors() != null && !inbound.getErrors().isEmpty()
                                    ? String.join(";", inbound.getErrors()) : null);
                }
            } catch (PartNodeConflictException e) {
                failed++;
                recordItem(operation, candidateKey, "FAILED_FINAL", context, "PART_INSTANCE",
                        "NODE_CONFLICT", net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_NODE_CONFLICT.getCode(),
                        e.getMessage());
            } catch (DeviceCategoryResolveException e) {
                failed++;
                recordItem(operation, candidateKey, e.isRetryable() ? "FAILED_RETRYABLE" : "FAILED_FINAL",
                        context, "PART_INSTANCE",
                        e.isRetryable() ? "RESOLVE_RETRYABLE" : "RESOLVE_FINAL",
                        net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED.getCode(),
                        e.getMessage());
            } catch (PreviewStaleException e) {
                skipped++;
                recordItem(operation, candidateKey, "SKIPPED", context, "PART_INSTANCE", "PREVIEW_STALE",
                        net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE.getCode(),
                        e.getMessage());
            } catch (Exception e) {
                failed++;
                log.warn("零件域重导失败: partCode={}, sn={}, error={}", partCode, sn, e.getMessage());
                recordItem(operation, candidateKey, "FAILED_RETRYABLE", context, "PART_INSTANCE", null, null, e.getMessage());
            }
        }
        return new ReimportOutcome(changed, unchanged, skipped, failed);
    }

    /**
     * 记录逐项审计（upsert：已存在则更新状态/原因，避免重试时重复插入冲突；检查点兼容）
     */
    private void recordItem(ImportOperation operation, String candidateKey, String status, ExecuteContext context,
                            String entityType) {
        recordItem(operation, candidateKey, status, context, entityType, null, null, null);
    }

    /**
     * 记录逐项审计（指定状态/原因/错误）
     */
    private void recordItem(ImportOperation operation, String candidateKey, String status,
                            ExecuteContext context, String entityType, String skipReason,
                            String errorCode, String errorMessage) {
        ImportOperationItem item = ImportOperationItem.builder()
                .operationId(operation.getOperationId())
                .itemKey(sha256(String.join(":",
                        operation.getDomain(), candidateKey, "REIMPORT", operation.getOperationType())))
                .candidateKey(candidateKey)
                .entityType(entityType)
                .vin(operation.getDomain().equals("VEHICLE") ? candidateKey : null)
                .actionType("REIMPORT")
                .phase("ALL")
                .status(status)
                .source(operation.getOperationSource())
                .attemptCount(1)
                .skipReason(skipReason)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .startedAt(LocalDateTime.now(ZoneOffset.UTC))
                .finishedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        try {
            ImportOperationItem existing = importOperationRepository.selectItemByKey(
                    operation.getOperationId(), item.getItemKey());
            if (existing != null) {
                item.setId(existing.getId());
                importOperationRepository.updateItem(item);
            } else {
                importOperationRepository.insertItem(item);
            }
        } catch (Exception e) {
            log.warn("重导逐项审计写入失败: operationId={}, candidate={}, error={}",
                    operation.getOperationId(), candidateKey, e.getMessage());
        }
    }

    // ==================== CR-072 冻结计划解析与校验 ====================

    /**
     * 冻结计划上下文：schemaVersion=cr072-category-v1 且含 categoryPlan 视为类别计划生效；
     * 老操作（无类别计划）不获得新增类别补缺能力。
     */
    @Getter
    private static class PlanContext {
        private final boolean hasCategoryPlan;
        private final Set<String> approvedCandidates;
        private final cn.hutool.json.JSONObject categoryPlan;

        private PlanContext(boolean hasCategoryPlan, Set<String> approvedCandidates,
                            cn.hutool.json.JSONObject categoryPlan) {
            this.hasCategoryPlan = hasCategoryPlan;
            this.approvedCandidates = approvedCandidates;
            this.categoryPlan = categoryPlan;
        }

        static PlanContext of(ImportOperation operation) {
            String planJson = operation.getPlanJson();
            if (planJson == null || planJson.isBlank()) {
                return new PlanContext(false, Set.of(), null);
            }
            try {
                cn.hutool.json.JSONObject plan = cn.hutool.json.JSONUtil.parseObj(planJson);
                boolean has = ImportPlanCoder.SCHEMA_VERSION.equals(plan.getStr("schemaVersion"))
                        && plan.getJSONObject("categoryPlan") != null;
                Set<String> candidates = new HashSet<>();
                cn.hutool.json.JSONArray arr = plan.getJSONArray("candidates");
                if (arr != null) {
                    arr.forEach(c -> candidates.add(String.valueOf(c)));
                }
                return new PlanContext(has, candidates, has ? plan.getJSONObject("categoryPlan") : null);
            } catch (Exception e) {
                log.warn("冻结计划解析失败: operationId={}, error={}", operation.getOperationId(), e.getMessage());
                return new PlanContext(false, Set.of(), null);
            }
        }
    }

    /**
     * PART 域：校验当前实例与冻结计划条目（实例存在性/版本/当前类别/最终节点/投影版本指纹），
     * 任一不符返回 PREVIEW_STALE 原因；全部通过返回 null。
     */
    private String validatePartAgainstPlan(String partCode, String sn, cn.hutool.json.JSONObject entry) {
        PartInfo current = partInfoRepository.selectByPartCodeAndSn(partCode, sn);
        boolean expectsNew = Boolean.TRUE.equals(entry.getBool("expectsNewInstance", false));
        if (expectsNew) {
            if (current != null) {
                return "INSTANCE_CREATED_CONCURRENTLY";
            }
        } else {
            if (current == null) {
                return "INSTANCE_MISSING";
            }
            if (entry.getInt("expectedRowVersion") != null
                    && !Objects.equals(entry.getInt("expectedRowVersion"), current.getRowVersion())) {
                return "ROW_VERSION_CHANGED";
            }
            String expectedCategory = entry.getStr("expectedCurrentCategory"); // null = 要求当前为 NULL
            if (!Objects.equals(expectedCategory, current.getDeviceCategory())) {
                return "CURRENT_CATEGORY_CHANGED";
            }
            String finalNode = entry.getStr("finalNode");
            if (finalNode != null && !finalNode.equals(current.getVehicleNodeCode())) {
                return "NODE_CHANGED";
            }
            // 仅当计划预期补缺时校验投影版本/指纹（非空快照保留不受字典后变影响）
            if (Boolean.TRUE.equals(entry.getBool("fillPlanned", false))) {
                String nodeMismatch = nodeEvidenceMismatch(finalNode, entry);
                if (nodeMismatch != null) {
                    return nodeMismatch;
                }
            }
        }
        return null;
    }

    /**
     * VEHICLE 域：VIN 粒度校验全部零件计划条目（任一陈旧/未批准 → 该 VIN 无写入）
     */
    private String validateVinPartsAgainstPlan(String vin, cn.hutool.json.JSONObject vinEntry) {
        cn.hutool.json.JSONObject parts = vinEntry.getJSONObject("parts");
        if (parts == null) {
            return "NO_PLAN_PARTS";
        }
        for (String partKey : parts.keySet()) {
            cn.hutool.json.JSONObject entry = parts.getJSONObject(partKey);
            if (entry == null || !Boolean.TRUE.equals(entry.getBool("candidateValid", false))) {
                return "PART_PLAN_INVALID:" + partKey;
            }
            String[] split = partKey.split(":", 2);
            if (split.length != 2) {
                return "PART_KEY_INVALID:" + partKey;
            }
            String stale = validatePartAgainstPlan(split[0], split[1], entry);
            if (stale != null) {
                return "PART_STALE:" + partKey + ":" + stale;
            }
        }
        return null;
    }

    /**
     * 节点投影证据校验：计划记录 externalVersion 则比对版本，否则比对内容指纹（不可得时不编造）
     */
    private String nodeEvidenceMismatch(String finalNode, cn.hutool.json.JSONObject entry) {
        if (finalNode == null) {
            return null;
        }
        VehicleNode node = vehicleNodeAppService.getVehicleNodeByCode(finalNode);
        if (node == null) {
            return "NODE_PROJECTION_NOT_READY";
        }
        Long planVersion = entry.getLong("nodeExternalVersion");
        String planFingerprint = entry.getStr("nodeFingerprint");
        if (planVersion != null) {
            if (!Objects.equals(planVersion, node.getExternalVersion())) {
                return "NODE_VERSION_CHANGED";
            }
        } else if (planFingerprint != null) {
            String category = node.getDeviceCategory() != null ? node.getDeviceCategory().trim() : null;
            String fp = ImportPlanCoder.nodeFingerprint(finalNode, category);
            if (!planFingerprint.equals(fp)) {
                return "NODE_FINGERPRINT_CHANGED";
            }
        }
        return null;
    }


    /**
     * 占位：构造与解析器一致的绑定数组（此处直接复用 ecuBaseline 片段）
     */
    private JSONArray bindParts(JSONObject ecu) {
        JSONArray array = new JSONArray();
        array.add(ecu);
        return array;
    }

    private String md5(String raw) {
        return cn.hutool.crypto.digest.DigestUtil.md5Hex(raw);
    }

    private byte[] sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 执行上下文
     */
    @Getter
    @Builder
    public static class ExecuteContext {
        private String operationId;
        private String operatorId;
        private String operatorName;
    }

    /**
     * 执行计数
     */
    public record ReimportOutcome(int changed, int unchanged, int skipped, int failed) {
        public static ReimportOutcome failed(String reason) {
            return new ReimportOutcome(0, 0, 0, 0);
        }
    }
}
