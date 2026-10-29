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
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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

    /** 执行器标识（租约 owner 前缀） */
    private static final String OWNER_PREFIX = "reimport-worker";

    /**
     * 执行车辆域定向重导（PRODUCE/TOL/EOL）
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
                String outcome = reimportVehicle(batchNum, type, vin, itemJson, context);
                switch (outcome) {
                    case "CHANGED" -> changed++;
                    case "NOOP" -> unchanged++;
                    default -> skipped++;
                }
                recordItem(operation, vin, outcome, context);
            } catch (Exception e) {
                failed++;
                log.warn("重导车辆[{}]失败: {}", vin, e.getMessage(), e);
                recordItem(operation, vin, "FAILED", context);
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
     *
     * @param operation 操作
     * @param scopeKeys 候选 partCode:sn 子集（null=整批）
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
                skipped++;
                continue;
            }
            try {
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
                } else {
                    failed++;
                    log.warn("零件域重导入站失败: partCode={}, sn={}", partCode, sn);
                }
                recordItem(operation, candidateKey, ok ? "CHANGED" : "FAILED", context, "PART_INSTANCE");
            } catch (Exception e) {
                failed++;
                log.warn("零件域重导失败: partCode={}, sn={}, error={}", partCode, sn, e.getMessage());
                recordItem(operation, candidateKey, "FAILED", context, "PART_INSTANCE");
            }
        }
        return new ReimportOutcome(changed, unchanged, skipped, failed);
    }

    /**
     * 记录逐项审计
     */
    private void recordItem(ImportOperation operation, String candidateKey, String outcome, ExecuteContext context) {
        recordItem(operation, candidateKey, outcome, context, "VEHICLE_BASIC");
    }

    /**
     * 记录逐项审计（指定实体类型）
     */
    private void recordItem(ImportOperation operation, String candidateKey, String outcome,
                            ExecuteContext context, String entityType) {
        ImportOperationItem item = ImportOperationItem.builder()
                .operationId(operation.getOperationId())
                .itemKey(sha256(String.join(":",
                        operation.getDomain(), candidateKey, "REIMPORT", operation.getOperationType())))
                .candidateKey(candidateKey)
                .entityType(entityType)
                .vin(operation.getDomain().equals("VEHICLE") ? candidateKey : null)
                .actionType("REIMPORT")
                .phase("ALL")
                .status(mapOutcome(outcome))
                .source(operation.getOperationSource())
                .attemptCount(1)
                .startedAt(LocalDateTime.now(ZoneOffset.UTC))
                .finishedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        try {
            importOperationRepository.insertItem(item);
        } catch (Exception e) {
            log.warn("重导逐项审计写入失败: operationId={}, candidate={}, error={}",
                    operation.getOperationId(), candidateKey, e.getMessage());
        }
    }

    private String mapOutcome(String outcome) {
        return switch (outcome) {
            case "CHANGED" -> "SUCCEEDED";
            case "NOOP" -> "NOOP";
            case "FAILED" -> "FAILED_RETRYABLE";
            default -> "SKIPPED";
        };
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
