package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleInfoChangeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 车辆基础信息统一变更写服务（CR-071）
 * <p>
 * 车辆 basic 白名单关键字段（工厂/产品代码/配置/订单/下线结果等）的受控写入入口：
 * 当前读 → 字段 diff（按 repairPolicy）→ CAS 更新 → 属性历史同事务写入。
 * detail/vehicle_option 命名空间由调用方在 after_json 中提供本次实际修改部分。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleInfoMutationAppService {

    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleInfoChangeLogRepository vehicleInfoChangeLogRepository;

    public static final String POLICY_FILL_MISSING = "FILL_MISSING";
    public static final String POLICY_CORRECT_EXPLICIT = "CORRECT_EXPLICIT";

    /**
     * 补丁更新车辆基础信息（basic + 属性历史同事务）
     *
     * @param cmd 补丁命令
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MutationResult patch(VehicleInfoPatchCmd cmd) {
        VehicleBasicInfo current = resolve(cmd);
        if (current == null) {
            throw new VehicleBasicInfoNotExistException(cmd.getVin());
        }

        // 0. 预期版本强校验（CORRECT_EXPLICIT 基于预览版本执行）
        if (cmd.getExpectedRowVersion() != null
                && !cmd.getExpectedRowVersion().equals(current.getRowVersion())) {
            log.warn("车辆[{}]预期版本不一致: expected={}, current={}",
                    cmd.getVin(), cmd.getExpectedRowVersion(), current.getRowVersion());
            return MutationResult.conflict(current.getRowVersion());
        }

        Map<String, Object> changed = computeDiff(current, cmd);
        if (changed.isEmpty()) {
            log.debug("车辆[{}]无字段差异，NOOP", cmd.getVin());
            return MutationResult.noop(current.getRowVersion());
        }

        VehicleBasicInfo target = VehicleBasicInfo.builder().id(current.getId()).build();
        applyChanged(target, changed);

        String operator = cmd.getOperatorId() != null ? cmd.getOperatorId() : "SYSTEM";
        target.setModifyBy(operator);
        int rows = vehBasicInfoRepository.patchByExpectedVersion(target, current.getRowVersion());
        if (rows == 0) {
            log.warn("车辆[{}]CAS版本冲突: expected={}", cmd.getVin(), current.getRowVersion());
            return MutationResult.conflict(current.getRowVersion());
        }
        int newRowVersion = current.getRowVersion() + 1;

        VehicleInfoChangeLog changeLog = buildChangeLog(current, cmd, changed, newRowVersion);
        vehicleInfoChangeLogRepository.insert(changeLog);

        log.info("车辆[{}]变更已提交: version {}→{}, changed={}",
                cmd.getVin(), current.getRowVersion(), newRowVersion, changed.keySet());
        return MutationResult.applied(newRowVersion, changed);
    }

    /**
     * 创建车辆基础信息（CR-071：新车辆 + before=null 属性历史同事务）
     * 若 VIN 已存在则退化为补丁更新（FILL_MISSING），不产生重复记录
     *
     * @param vehicleBasicInfo 新车辆
     * @param ctx 写上下文（复用 VehicleInfoPatchCmd 的上下文字段）
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MutationResult create(VehicleBasicInfo vehicleBasicInfo, VehicleInfoPatchCmd ctx) {
        VehicleBasicInfo existing = vehBasicInfoRepository.selectByVin(vehicleBasicInfo.getVin());
        if (existing != null) {
            // 已存在：退化为补丁更新（幂等）
            VehicleInfoPatchCmd patchCmd = VehicleInfoPatchCmd.builder()
                    .vehicleId(existing.getId())
                    .vin(vehicleBasicInfo.getVin())
                    .plantCode(vehicleBasicInfo.getPlantCode())
                    .brandCode(vehicleBasicInfo.getBrandCode())
                    .platformCode(vehicleBasicInfo.getPlatformCode())
                    .carLineCode(vehicleBasicInfo.getCarLineCode())
                    .modelCode(vehicleBasicInfo.getModelCode())
                    .variantCode(vehicleBasicInfo.getVariantCode())
                    .configurationCode(vehicleBasicInfo.getConfigurationCode())
                    .eolTime(vehicleBasicInfo.getEolTime())
                    .pdiTime(vehicleBasicInfo.getPdiTime())
                    .orderNum(vehicleBasicInfo.getOrderNum())
                    .vehicleBaseVersion(vehicleBasicInfo.getVehicleBaseVersion())
                    .eolResult(vehicleBasicInfo.getEolResult())
                    .source(ctx.getSource())
                    .repairPolicy(POLICY_FILL_MISSING)
                    .changeType(ctx.getChangeType() != null ? ctx.getChangeType() : "INBOUND")
                    .operationSource(ctx.getOperationSource())
                    .operatorId(ctx.getOperatorId())
                    .operatorName(ctx.getOperatorName())
                    .sourceEventTime(ctx.getSourceEventTime())
                    .sourceEventId(ctx.getSourceEventId())
                    .operationId(ctx.getOperationId())
                    .phase(ctx.getPhase() != null ? ctx.getPhase() : "BASIC")
                    .seq(ctx.getSeq() != null ? ctx.getSeq() : 1)
                    .build();
            return patch(patchCmd);
        }

        // 新车辆：insert + 属性历史（before=null）
        vehBasicInfoRepository.insert(vehicleBasicInfo);

        String operator = ctx.getOperatorId() != null ? ctx.getOperatorId() : "SYSTEM";
        VehicleInfoChangeLog changeLog = VehicleInfoChangeLog.builder()
                .vehicleId(vehicleBasicInfo.getId())
                .vin(vehicleBasicInfo.getVin())
                .changeType(ctx.getChangeType() != null ? ctx.getChangeType() : "INBOUND")
                .beforeJson(null)
                .afterJson(snapshot(vehicleBasicInfo).toString())
                .changedFields(snapshot(vehicleBasicInfo).toString())
                .source(ctx.getSource() != null ? ctx.getSource() : "MANUAL")
                .operationSource(ctx.getOperationSource() != null ? ctx.getOperationSource() : "APP")
                .operatorId(operator)
                .operatorName(ctx.getOperatorName())
                .changeTime(LocalDateTime.now(ZoneOffset.UTC))
                .sourceEventTime(ctx.getSourceEventTime() != null
                        ? LocalDateTime.ofInstant(ctx.getSourceEventTime(), ZoneOffset.UTC) : null)
                .sourceEventId(ctx.getSourceEventId())
                .operationId(ctx.getOperationId() != null ? ctx.getOperationId() : "NONE")
                .mutationKey(buildMutationKey(ctx, vehicleBasicInfo.getId()))
                .rowVersion(1)
                .build();
        vehicleInfoChangeLogRepository.insert(changeLog);
        return MutationResult.applied(1, java.util.Map.of("created", true));
    }

    private VehicleBasicInfo resolve(VehicleInfoPatchCmd cmd) {
        if (cmd.getVehicleId() != null) {
            return vehBasicInfoRepository.selectById(cmd.getVehicleId());
        }
        return vehBasicInfoRepository.selectByVin(cmd.getVin());
    }

    /**
     * 白名单字段 diff
     */
    private Map<String, Object> computeDiff(VehicleBasicInfo current, VehicleInfoPatchCmd cmd) {
        Map<String, Object> changed = new LinkedHashMap<>();
        boolean correct = POLICY_CORRECT_EXPLICIT.equals(cmd.getRepairPolicy());

        diffString(changed, current.getPlantCode(), cmd.getPlantCode(), "plantCode", correct);
        diffString(changed, current.getBrandCode(), cmd.getBrandCode(), "brandCode", correct);
        diffString(changed, current.getPlatformCode(), cmd.getPlatformCode(), "platformCode", correct);
        diffString(changed, current.getCarLineCode(), cmd.getCarLineCode(), "carLineCode", correct);
        diffString(changed, current.getModelCode(), cmd.getModelCode(), "modelCode", correct);
        diffString(changed, current.getVariantCode(), cmd.getVariantCode(), "variantCode", correct);
        diffString(changed, current.getConfigurationCode(), cmd.getConfigurationCode(), "configurationCode", correct);
        diffString(changed, current.getOrderNum(), cmd.getOrderNum(), "orderNum", correct);
        diffString(changed, current.getVehicleBaseVersion(), cmd.getVehicleBaseVersion(), "vehicleBaseVersion", correct);
        diffString(changed, current.getEolResult(), cmd.getEolResult(), "eolResult", correct);

        diffObject(changed, current.getEolTime(), cmd.getEolTime(), "eolTime", correct);
        diffObject(changed, current.getPdiTime(), cmd.getPdiTime(), "pdiTime", correct);

        return changed;
    }

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

    private void applyChanged(VehicleBasicInfo target, Map<String, Object> changed) {
        changed.forEach((field, value) -> setField(target, field, value));
    }

    private void setField(VehicleBasicInfo target, String field, Object value) {
        switch (field) {
            case "plantCode" -> target.setPlantCode((String) value);
            case "brandCode" -> target.setBrandCode((String) value);
            case "platformCode" -> target.setPlatformCode((String) value);
            case "carLineCode" -> target.setCarLineCode((String) value);
            case "modelCode" -> target.setModelCode((String) value);
            case "variantCode" -> target.setVariantCode((String) value);
            case "configurationCode" -> target.setConfigurationCode((String) value);
            case "eolTime" -> target.setEolTime((java.time.Instant) value);
            case "pdiTime" -> target.setPdiTime((java.time.Instant) value);
            case "orderNum" -> target.setOrderNum((String) value);
            case "vehicleBaseVersion" -> target.setVehicleBaseVersion((String) value);
            case "eolResult" -> target.setEolResult((String) value);
            default -> throw new IllegalArgumentException("未知白名单字段: " + field);
        }
    }

    /**
     * 构造属性历史（basic 命名空间；detail/options 由调用方附加）
     */
    private VehicleInfoChangeLog buildChangeLog(VehicleBasicInfo current, VehicleInfoPatchCmd cmd,
                                                Map<String, Object> changed, int newRowVersion) {
        JSONObject before = snapshot(current);
        JSONObject after = new JSONObject();
        after.putAll(before);
        changed.forEach(after::set);

        JSONObject changedJson = new JSONObject();
        changed.forEach((field, value) -> changedJson.set(field, value));

        return VehicleInfoChangeLog.builder()
                .vehicleId(current.getId())
                .vin(current.getVin())
                .changeType(cmd.getChangeType() != null ? cmd.getChangeType() : "ATTRIBUTE_UPDATE")
                .beforeJson(before.toString())
                .afterJson(after.toString())
                .changedFields(changedJson.toString())
                .source(cmd.getSource() != null ? cmd.getSource() : "MANUAL")
                .operationSource(cmd.getOperationSource() != null ? cmd.getOperationSource() : "APP")
                .operatorId(cmd.getOperatorId() != null ? cmd.getOperatorId() : "SYSTEM")
                .operatorName(cmd.getOperatorName())
                .changeTime(LocalDateTime.now(ZoneOffset.UTC))
                .sourceEventTime(cmd.getSourceEventTime() != null
                        ? LocalDateTime.ofInstant(cmd.getSourceEventTime(), ZoneOffset.UTC) : null)
                .sourceEventId(cmd.getSourceEventId())
                .operationId(cmd.getOperationId() != null ? cmd.getOperationId() : "NONE")
                .mutationKey(buildMutationKey(cmd, current.getId()))
                .rowVersion(newRowVersion)
                .build();
    }

    private JSONObject snapshot(VehicleBasicInfo basicInfo) {
        JSONObject json = new JSONObject();
        json.set("plantCode", basicInfo.getPlantCode());
        json.set("brandCode", basicInfo.getBrandCode());
        json.set("platformCode", basicInfo.getPlatformCode());
        json.set("carLineCode", basicInfo.getCarLineCode());
        json.set("modelCode", basicInfo.getModelCode());
        json.set("variantCode", basicInfo.getVariantCode());
        json.set("configurationCode", basicInfo.getConfigurationCode());
        json.set("eolTime", basicInfo.getEolTime());
        json.set("pdiTime", basicInfo.getPdiTime());
        json.set("orderNum", basicInfo.getOrderNum());
        json.set("vehicleBaseVersion", basicInfo.getVehicleBaseVersion());
        json.set("eolResult", basicInfo.getEolResult());
        return json;
    }

    private byte[] buildMutationKey(VehicleInfoPatchCmd cmd, Long vehicleId) {
        String raw = String.join(":",
                cmd.getOperationId() != null ? cmd.getOperationId() : "NONE",
                String.valueOf(vehicleId),
                cmd.getPhase() != null ? cmd.getPhase() : "BASIC",
                String.valueOf(cmd.getSeq() != null ? cmd.getSeq() : 1));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 变更结果
     */
    public record MutationResult(
            String outcome,
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
    }

    /**
     * 车辆基础信息不存在异常
     */
    public static class VehicleBasicInfoNotExistException extends RuntimeException {
        public VehicleBasicInfoNotExistException(String vin) {
            super("车辆基础信息不存在: " + vin);
        }
    }
}
