package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartInstanceNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 零件实例统一变更写服务（CR-071）
 * <p>
 * 所有零件实例本体/兼容投影写入的受控入口：当前读 → 字段 diff（按 repairPolicy）
 * → CAS 更新（expectedRowVersion）→ 属性历史同事务写入。零差异不写日志、不增版本、
 * 不触发业务变化事件。禁止绕过本服务直接裸 Mapper 修改（§7.4/§10.1）。
 * <p>
 * 写策略（§4.1）：
 * - FILL_MISSING：null/trim 后空串可补；0/MANUAL/IN_STOCK 等默认值不自动当空。
 * - CORRECT_EXPLICIT：白名单字段非空覆盖（调用方需独立权限、expectedRowVersion 与原因）。
 * - 软件投影列（softwareVer/softwarePn/softwareSnapshot*）仅由 SSOT 派生路径调用，可显式置 null。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartInfoMutationAppService {

    private final PartInfoRepository partInfoRepository;
    private final PartInfoChangeLogRepository partInfoChangeLogRepository;

    /** 回填策略 */
    public static final String POLICY_FILL_MISSING = "FILL_MISSING";
    public static final String POLICY_CORRECT_EXPLICIT = "CORRECT_EXPLICIT";

    /**
     * 补丁更新零件实例（本体 + 属性历史同事务）
     *
     * @param cmd 补丁命令
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MutationResult patch(PartInfoPatchCmd cmd) {
        PartInfo current = resolve(cmd);
        if (current == null) {
            throw new PartInstanceNotExistException(cmd.getPartCode(), cmd.getSn());
        }

        // 0. 预期版本强校验（CORRECT_EXPLICIT 基于预览版本执行）
        if (cmd.getExpectedRowVersion() != null
                && !cmd.getExpectedRowVersion().equals(current.getRowVersion())) {
            log.warn("零件[{}:{}]预期版本不一致: expected={}, current={}",
                    current.getPartCode(), current.getSn(), cmd.getExpectedRowVersion(), current.getRowVersion());
            return MutationResult.conflict(current.getRowVersion());
        }

        // 1. 计算白名单字段 diff
        Map<String, Object> changed = computeDiff(current, cmd);
        if (changed.isEmpty()) {
            log.debug("零件[{}:{}]无字段差异，NOOP", current.getPartCode(), current.getSn());
            return MutationResult.noop(current.getRowVersion());
        }

        // 2. 构造更新目标（仅含变更字段）
        PartInfo target = PartInfo.builder().id(current.getId()).build();
        applyChanged(target, changed);

        // 3. CAS 更新（期望版本校验）
        String operator = cmd.getOperatorId() != null ? cmd.getOperatorId() : "SYSTEM";
        target.setModifyBy(operator);
        int rows = partInfoRepository.patchByExpectedVersion(
                target, current.getRowVersion(),
                cmd.getNullFields() != null ? cmd.getNullFields() : List.of());
        if (rows == 0) {
            log.warn("零件[{}:{}]CAS版本冲突: expected={}", current.getPartCode(), current.getSn(), current.getRowVersion());
            return MutationResult.conflict(current.getRowVersion());
        }
        int newRowVersion = current.getRowVersion() + 1;

        // 4. 属性历史（同事务）
        PartInfoChangeLog changeLog = buildChangeLog(current, cmd, changed, newRowVersion);
        partInfoChangeLogRepository.insert(changeLog);

        log.info("零件[{}:{}]变更已提交: version {}→{}, changed={}",
                current.getPartCode(), current.getSn(), current.getRowVersion(), newRowVersion, changed.keySet());
        return MutationResult.applied(newRowVersion, changed);
    }

    /**
     * 创建零件实例（CR-071：新实例 + before=null 属性历史同事务）
     * 若 (partCode,sn) 已存在则退化为补丁更新（FILL_MISSING），不产生重复实例
     *
     * @param partInfo 新实例
     * @param ctx 写上下文（复用 PartInfoPatchCmd 的上下文字段）
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MutationResult create(PartInfo partInfo, PartInfoPatchCmd ctx) {
        PartInfo existing = partInfoRepository.selectByPartCodeAndSn(partInfo.getPartCode(), partInfo.getSn());
        if (existing != null) {
            // 已存在：退化为补丁更新（幂等，不重复实例）
            partInfo.setId(existing.getId()); // 回写调用方对象（后续绑定等依赖 id）
            PartInfoPatchCmd patchCmd = PartInfoPatchCmd.builder()
                    .partId(existing.getId())
                    .partCode(partInfo.getPartCode())
                    .sn(partInfo.getSn())
                    .vehicleNodeCode(partInfo.getVehicleNodeCode())
                    .configWord(partInfo.getConfigWord())
                    .supplierCode(partInfo.getSupplierCode())
                    .batchNum(partInfo.getBatchNum())
                    .hardwareVer(partInfo.getHardwareVer())
                    .hardwarePn(partInfo.getHardwarePn())
                    .softwareVer(partInfo.getSoftwareVer())
                    .softwarePn(partInfo.getSoftwarePn())
                    .extra(partInfo.getExtra())
                    .instanceState(partInfo.getInstanceState())
                    .source(partInfo.getSource() != null ? partInfo.getSource().getValue() : null)
                    .partType(partInfo.getPartType())
                    .inboundBatchNo(partInfo.getInboundBatchNo())
                    .sourceEventId(partInfo.getSourceEventId())
                    .lastInboundTime(partInfo.getLastInboundTime())
                    .repairPolicy(POLICY_FILL_MISSING)
                    .changeType(ctx.getChangeType() != null ? ctx.getChangeType() : "INBOUND")
                    .operationSource(ctx.getOperationSource())
                    .operatorId(ctx.getOperatorId())
                    .operatorName(ctx.getOperatorName())
                    .sourceEventTime(ctx.getSourceEventTime())
                    .operationId(ctx.getOperationId())
                    .phase(ctx.getPhase() != null ? ctx.getPhase() : "BASIC")
                    .seq(ctx.getSeq() != null ? ctx.getSeq() : 1)
                    .build();
            return patch(patchCmd);
        }

        // 新实例：insert + 属性历史（before=null）
        partInfo.setFirstSeenTime(java.time.Instant.now());
        partInfoRepository.insert(partInfo);

        String operator = ctx.getOperatorId() != null ? ctx.getOperatorId() : "SYSTEM";
        PartInfoChangeLog changeLog = PartInfoChangeLog.builder()
                .partId(partInfo.getId())
                .changeType(ctx.getChangeType() != null ? ctx.getChangeType() : "INBOUND")
                .beforeJson(null)
                .afterJson(snapshot(partInfo))
                .changedFields(snapshot(partInfo))
                .source(ctx.getSource() != null ? ctx.getSource() : (partInfo.getSource() != null ? partInfo.getSource().getValue() : "MANUAL"))
                .operationSource(ctx.getOperationSource() != null ? ctx.getOperationSource() : "APP")
                .operatorId(operator)
                .operatorName(ctx.getOperatorName())
                .changeTime(LocalDateTime.now(ZoneOffset.UTC))
                .sourceEventTime(ctx.getSourceEventTime() != null
                        ? LocalDateTime.ofInstant(ctx.getSourceEventTime(), ZoneOffset.UTC) : null)
                .sourceEventId(ctx.getSourceEventId() != null ? ctx.getSourceEventId() : partInfo.getSourceEventId())
                .inboundBatchNo(ctx.getInboundBatchNo() != null ? ctx.getInboundBatchNo() : partInfo.getInboundBatchNo())
                .operationId(ctx.getOperationId() != null ? ctx.getOperationId() : "NONE")
                .mutationKey(buildMutationKey(ctx, partInfo.getId()))
                .rowVersion(1)
                .build();
        partInfoChangeLogRepository.insert(changeLog);
        return MutationResult.applied(1, Map.of("created", true));
    }

    /**
     * 按 partId 或 (partCode,sn) 定位当前实例
     */
    private PartInfo resolve(PartInfoPatchCmd cmd) {
        if (cmd.getPartId() != null) {
            return partInfoRepository.selectById(cmd.getPartId());
        }
        return partInfoRepository.selectByPartCodeAndSn(cmd.getPartCode(), cmd.getSn());
    }

    /**
     * 白名单字段 diff（§4.1 写策略）
     *
     * @return 变更字段名 → 新值
     */
    private Map<String, Object> computeDiff(PartInfo current, PartInfoPatchCmd cmd) {
        Map<String, Object> changed = new LinkedHashMap<>();
        boolean correct = POLICY_CORRECT_EXPLICIT.equals(cmd.getRepairPolicy());

        diffString(changed, current.getVehicleNodeCode(), cmd.getVehicleNodeCode(), "vehicleNodeCode", correct);
        diffString(changed, current.getConfigWord(), cmd.getConfigWord(), "configWord", correct);
        diffString(changed, current.getSupplierCode(), cmd.getSupplierCode(), "supplierCode", correct);
        diffString(changed, current.getBatchNum(), cmd.getBatchNum(), "batchNum", correct);
        diffString(changed, current.getHardwareVer(), cmd.getHardwareVer(), "hardwareVer", correct);
        diffString(changed, current.getHardwarePn(), cmd.getHardwarePn(), "hardwarePn", correct);
        diffString(changed, current.getSoftwareVer(), cmd.getSoftwareVer(), "softwareVer", correct);
        diffString(changed, current.getSoftwarePn(), cmd.getSoftwarePn(), "softwarePn", correct);
        diffString(changed, current.getSource() != null ? current.getSource().getValue() : null,
                cmd.getSource(), "source", correct);
        diffString(changed, current.getPartType(), cmd.getPartType(), "partType", correct);
        diffString(changed, current.getInboundBatchNo(), cmd.getInboundBatchNo(), "inboundBatchNo", correct);
        diffString(changed, current.getSourceEventId(), cmd.getSourceEventId(), "sourceEventId", correct);
        diffString(changed, current.getSoftwareSnapshotState(), cmd.getSoftwareSnapshotState(), "softwareSnapshotState", correct);
        diffString(changed, current.getSoftwareSnapshotTarget(), cmd.getSoftwareSnapshotTarget(), "softwareSnapshotTarget", correct);
        diffString(changed, current.getSoftwareSnapshotSlot(), cmd.getSoftwareSnapshotSlot(), "softwareSnapshotSlot", correct);

        diffObject(changed, current.getInstanceState(), cmd.getInstanceState(), "instanceState", correct);
        diffObject(changed, current.getLastInboundTime(), cmd.getLastInboundTime(), "lastInboundTime", correct);
        diffObject(changed, current.getSoftwareSnapshotInstallationId(), cmd.getSoftwareSnapshotInstallationId(),
                "softwareSnapshotInstallationId", correct);
        diffObject(changed, current.getSoftwareSnapshotInventoryVersion(), cmd.getSoftwareSnapshotInventoryVersion(),
                "softwareSnapshotInventoryVersion", correct);

        // extra：type-schema 合并（未提交键保留；冲突默认保留当前）
        if (cmd.getExtra() != null && !cmd.getExtra().isBlank()) {
            String mergedExtra = mergeExtra(current.getExtra(), cmd.getExtra(), correct);
            if (!Objects.equals(normalizeExtra(current.getExtra()), normalizeExtra(mergedExtra))) {
                changed.put("extra", mergedExtra);
            }
        }

        // 显式置 null（软件投影列清空）
        if (cmd.getNullFields() != null) {
            for (String field : cmd.getNullFields()) {
                if (isProjectionNullable(field) && !changed.containsKey(field)) {
                    Object cur = readField(current, field);
                    if (cur != null) {
                        changed.put(field, null);
                    }
                }
            }
        }
        return changed;
    }

    /**
     * 字符串字段 diff：FILL_MISSING 仅空值可补；CORRECT_EXPLICIT 非空覆盖
     */
    private void diffString(Map<String, Object> changed, String current, String target,
                            String field, boolean correct) {
        if (target == null || target.isBlank()) {
            return;
        }
        boolean fillable = correct || isBlank(current);
        if (fillable && !Objects.equals(current, target)) {
            changed.put(field, target);
        }
    }

    /**
     * 对象字段 diff（数字/时间/枚举）：FILL_MISSING 仅 null 可补；0/默认值不视为空
     */
    private void diffObject(Map<String, Object> changed, Object current, Object target,
                            String field, boolean correct) {
        if (target == null) {
            return;
        }
        boolean fillable = correct || current == null;
        if (fillable && !Objects.equals(current, target)) {
            changed.put(field, target);
        }
    }

    /**
     * 应用变更字段到更新目标
     */
    private void applyChanged(PartInfo target, Map<String, Object> changed) {
        changed.forEach((field, value) -> setField(target, field, value));
    }

    /**
     * 构造属性历史
     */
    private PartInfoChangeLog buildChangeLog(PartInfo current, PartInfoPatchCmd cmd,
                                             Map<String, Object> changed, int newRowVersion) {
        String beforeJson = snapshot(current);
        JSONObject afterJson = new JSONObject();
        JSONObject before = JSONUtil.parseObj(beforeJson);
        afterJson.putAll(before);
        changed.forEach(afterJson::set);

        JSONObject changedJson = new JSONObject();
        changed.forEach((field, value) -> changedJson.set(field, value));

        return PartInfoChangeLog.builder()
                .partId(current.getId())
                .changeType(cmd.getChangeType() != null ? cmd.getChangeType() : "ATTRIBUTE_UPDATE")
                .beforeJson(beforeJson)
                .afterJson(afterJson.toString())
                .changedFields(changedJson.toString())
                .source(cmd.getSource() != null ? cmd.getSource() : "MANUAL")
                .operationSource(cmd.getOperationSource() != null ? cmd.getOperationSource() : "APP")
                .operatorId(cmd.getOperatorId() != null ? cmd.getOperatorId() : "SYSTEM")
                .operatorName(cmd.getOperatorName())
                .changeTime(LocalDateTime.now(ZoneOffset.UTC))
                .sourceEventTime(cmd.getSourceEventTime() != null
                        ? LocalDateTime.ofInstant(cmd.getSourceEventTime(), ZoneOffset.UTC) : null)
                .sourceEventId(cmd.getSourceEventId())
                .inboundBatchNo(cmd.getInboundBatchNo())
                .operationId(cmd.getOperationId() != null ? cmd.getOperationId() : "NONE")
                .mutationKey(buildMutationKey(cmd, current.getId()))
                .rowVersion(newRowVersion)
                .build();
    }

    /**
     * 白名单属性快照（JSON）
     */
    private String snapshot(PartInfo partInfo) {
        JSONObject json = new JSONObject();
        json.set("vehicleNodeCode", partInfo.getVehicleNodeCode());
        json.set("configWord", partInfo.getConfigWord());
        json.set("supplierCode", partInfo.getSupplierCode());
        json.set("batchNum", partInfo.getBatchNum());
        json.set("hardwareVer", partInfo.getHardwareVer());
        json.set("hardwarePn", partInfo.getHardwarePn());
        json.set("softwareVer", partInfo.getSoftwareVer());
        json.set("softwarePn", partInfo.getSoftwarePn());
        json.set("extra", partInfo.getExtra());
        json.set("instanceState", partInfo.getInstanceState());
        json.set("source", partInfo.getSource() != null ? partInfo.getSource().getValue() : null);
        json.set("partType", partInfo.getPartType());
        json.set("inboundBatchNo", partInfo.getInboundBatchNo());
        json.set("sourceEventId", partInfo.getSourceEventId());
        json.set("lastInboundTime", partInfo.getLastInboundTime());
        json.set("softwareSnapshotState", partInfo.getSoftwareSnapshotState());
        json.set("softwareSnapshotInstallationId", partInfo.getSoftwareSnapshotInstallationId());
        json.set("softwareSnapshotTarget", partInfo.getSoftwareSnapshotTarget());
        json.set("softwareSnapshotSlot", partInfo.getSoftwareSnapshotSlot());
        json.set("softwareSnapshotInventoryVersion", partInfo.getSoftwareSnapshotInventoryVersion());
        return json.toString();
    }

    /**
     * extra 合并（type-schema；冲突默认保留当前，CORRECT_EXPLICIT 覆盖）
     */
    private String mergeExtra(String currentExtra, String cmdExtra, boolean correct) {
        JSONObject result = new JSONObject();
        if (currentExtra != null && !currentExtra.isBlank()) {
            result.putAll(JSONUtil.parseObj(currentExtra));
        }
        JSONObject incoming = JSONUtil.parseObj(cmdExtra);
        incoming.forEach((key, value) -> {
            if (correct) {
                result.set(key, value);
            } else if (!result.containsKey(key)
                    || result.get(key) == null || result.getStr(key).isBlank()) {
                result.set(key, value);
            }
        });
        return result.isEmpty() ? null : result.toString();
    }

    /**
     * 规范化 extra（空对象视为 null）
     */
    private String normalizeExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }
        JSONObject obj = JSONUtil.parseObj(extra);
        return obj.isEmpty() ? null : obj.toString();
    }

    /**
     * mutation_key：SHA-256(operationId,entityId,phase,seq)
     */
    private byte[] buildMutationKey(PartInfoPatchCmd cmd, Long partId) {
        String raw = String.join(":",
                cmd.getOperationId() != null ? cmd.getOperationId() : "NONE",
                String.valueOf(partId),
                cmd.getPhase() != null ? cmd.getPhase() : "BASIC",
                String.valueOf(cmd.getSeq() != null ? cmd.getSeq() : 1));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 可显式置 null 的投影字段白名单
     */
    private boolean isProjectionNullable(String field) {
        return switch (field) {
            case "softwareVer", "softwarePn",
                 "softwareSnapshotInstallationId", "softwareSnapshotTarget",
                 "softwareSnapshotSlot", "softwareSnapshotInventoryVersion" -> true;
            default -> false;
        };
    }

    /**
     * 读取当前字段值（nullFields 判断用）
     */
    private Object readField(PartInfo current, String field) {
        return switch (field) {
            case "softwareVer" -> current.getSoftwareVer();
            case "softwarePn" -> current.getSoftwarePn();
            case "softwareSnapshotInstallationId" -> current.getSoftwareSnapshotInstallationId();
            case "softwareSnapshotTarget" -> current.getSoftwareSnapshotTarget();
            case "softwareSnapshotSlot" -> current.getSoftwareSnapshotSlot();
            case "softwareSnapshotInventoryVersion" -> current.getSoftwareSnapshotInventoryVersion();
            default -> null;
        };
    }

    /**
     * 写入目标字段
     */
    private void setField(PartInfo target, String field, Object value) {
        switch (field) {
            case "vehicleNodeCode" -> target.setVehicleNodeCode((String) value);
            case "configWord" -> target.setConfigWord((String) value);
            case "supplierCode" -> target.setSupplierCode((String) value);
            case "batchNum" -> target.setBatchNum((String) value);
            case "hardwareVer" -> target.setHardwareVer((String) value);
            case "hardwarePn" -> target.setHardwarePn((String) value);
            case "softwareVer" -> target.setSoftwareVer((String) value);
            case "softwarePn" -> target.setSoftwarePn((String) value);
            case "extra" -> target.setExtra((String) value);
            case "instanceState" -> target.setInstanceState((Integer) value);
            case "source" -> target.setSource(value != null
                    ? net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType.valOf((String) value) : null);
            case "partType" -> target.setPartType((String) value);
            case "inboundBatchNo" -> target.setInboundBatchNo((String) value);
            case "sourceEventId" -> target.setSourceEventId((String) value);
            case "lastInboundTime" -> target.setLastInboundTime((java.time.Instant) value);
            case "softwareSnapshotState" -> target.setSoftwareSnapshotState((String) value);
            case "softwareSnapshotInstallationId" -> target.setSoftwareSnapshotInstallationId((Long) value);
            case "softwareSnapshotTarget" -> target.setSoftwareSnapshotTarget((String) value);
            case "softwareSnapshotSlot" -> target.setSoftwareSnapshotSlot((String) value);
            case "softwareSnapshotInventoryVersion" -> target.setSoftwareSnapshotInventoryVersion((Long) value);
            default -> throw new IllegalArgumentException("未知白名单字段: " + field);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 变更结果
     */
    public record MutationResult(
            String outcome, // APPLIED / NOOP / CONFLICT
            Integer rowVersion,
            Map<String, Object> changedFields
    ) {
        public static MutationResult applied(Integer rowVersion, Map<String, Object> changed) {
            return new MutationResult("APPLIED", rowVersion, changed);
        }

        public static MutationResult noop(Integer rowVersion) {
            return new MutationResult("NOOP", rowVersion, Map.of());
        }

        public static MutationResult conflict(Integer rowVersion) {
            return new MutationResult("CONFLICT", rowVersion, Map.of());
        }

        public boolean isApplied() {
            return "APPLIED".equals(outcome);
        }

        public boolean isNoop() {
            return "NOOP".equals(outcome);
        }
    }
}
