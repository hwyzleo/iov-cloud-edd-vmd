package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.HsmUidFieldResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityBizTypeResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityPresetPolicy;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SecurityPresetBizTypeUnresolvedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityPresetDecision;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.springframework.stereotype.Component;

/**
 * 器件安全常量补偿动作处理器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 复用 SecurityPresetPolicy、SecurityBizTypeResolver 与 PartSecurityPresetAppService 的补偿入口
 * （presetForReplay，不写回 part_import_data.description）：
 * - 当前策略不要求预置 → SKIPPED(POLICY_NOT_APPLICABLE)
 * - part_security_constant.preset_state=PRESET → SKIPPED(ALREADY_PRESET)
 * - 缺失或 FAILED → 允许补偿重试；幂等键保持 (partCode, sn, constantType)
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityPresetActionHandler implements PartPostProcessActionHandler {

    private static final String CONSTANT_TYPE = "ROOT";

    private final SecurityPresetPolicy securityPresetPolicy;
    private final SecurityBizTypeResolver securityBizTypeResolver;
    private final HsmUidFieldResolver hsmUidFieldResolver;
    private final PartSecurityPresetAppService partSecurityPresetAppService;
    private final PartSecurityConstantRepository partSecurityConstantRepository;

    @Override
    public String actionType() {
        return PartPostProcessActionType.SECURITY_PRESET.getValue();
    }

    @Override
    public boolean supports(PartPostProcessActionContext context) {
        return context.getPartInfo() != null;
    }

    @Override
    public String idempotencyKey(PartPostProcessActionContext context) {
        // 幂等键保持 (partCode, sn, constantType)，下游 / KMS 侧按此收敛
        return context.getPartCode() + ":" + context.getSn() + ":" + CONSTANT_TYPE;
    }

    @Override
    public PartPostProcessActionResult execute(PartPostProcessActionContext context) {
        PartInfo partInfo = context.getPartInfo();
        if (partInfo == null) {
            return PartPostProcessActionResult.failedFinal("PART_NOT_FOUND", "候选实例在当前已不存在，不得依据历史报文重建");
        }

        String nodeCode = resolveNodeCode(context);
        if (StrUtil.isBlank(nodeCode)) {
            return PartPostProcessActionResult.skipped("POLICY_NOT_APPLICABLE");
        }

        // 预置资格判定（能力优先，null 走旧注册表兜底）
        VehicleNode vehicleNode = context.getVehicleNode();
        String hsmCapability = vehicleNode != null ? vehicleNode.getHsmCapability() : null;
        SecurityPresetDecision decision = securityPresetPolicy.decide(hsmCapability, nodeCode);
        switch (decision) {
            case PRESET_NOT_REQUIRED:
                log.info("零件[{}:{}]节点[{}]能力[{}]不触发安全常量预置", context.getPartCode(), context.getSn(), nodeCode, hsmCapability);
                return PartPostProcessActionResult.skipped("POLICY_NOT_APPLICABLE");
            case INVALID_CAPABILITY:
                return PartPostProcessActionResult.failedFinal("INVALID_HSM_CAPABILITY",
                        "器件HSM能力值非法或不受支持: " + hsmCapability);
            case PRESET_REQUIRED:
            default:
                break;
        }

        // 已预置 → SKIPPED(ALREADY_PRESET)
        PartSecurityConstant existing = partSecurityConstantRepository.selectByPartCodeAndSn(context.getPartCode(), context.getSn());
        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("零件[{}:{}]安全常量已预置，跳过", context.getPartCode(), context.getSn());
            return PartPostProcessActionResult.skipped("ALREADY_PRESET");
        }

        // 解析 chipUid（本期默认 HSM 字段，源自当前 part_info.extra）
        String hsmUidField = hsmUidFieldResolver.resolve(null);
        String chipUid = null;
        if (StrUtil.isNotBlank(partInfo.getExtra())) {
            try {
                JSONObject extraJson = JSONUtil.parseObj(partInfo.getExtra());
                chipUid = extraJson.getStr(hsmUidField);
            } catch (Exception e) {
                log.warn("解析零件[{}]extra字段失败: {}", context.getPartCode(), e.getMessage());
            }
        }
        if (StrUtil.isBlank(chipUid)) {
            return PartPostProcessActionResult.failedFinal("HSM_UID_MISSING",
                    "缺少安全芯片标识(hsmUidField=" + hsmUidField + ")，无法完成器件安全常量补偿");
        }

        // 解析 BizType（deviceCategory 路由 → 旧节点码兜底 → 失败）
        String deviceCategory = vehicleNode != null ? vehicleNode.getDeviceCategory() : null;
        String funcDomain = vehicleNode != null ? vehicleNode.getFuncDomain() : null;
        BizType bizType;
        try {
            bizType = securityBizTypeResolver.resolve(deviceCategory, funcDomain, nodeCode);
        } catch (SecurityPresetBizTypeUnresolvedException e) {
            return PartPostProcessActionResult.failedFinal("BIZ_TYPE_UNRESOLVED", e.getMessage());
        }

        // 补偿执行（不写回 part_import_data.description）
        String presetError = partSecurityPresetAppService.presetForReplay(
                context.getPartCode(), context.getSn(), chipUid, context.getBatchNum(), nodeCode, bizType);
        if (presetError != null) {
            log.warn("零件[{}:{}]安全常量补偿失败: {}", context.getPartCode(), context.getSn(), presetError);
            return PartPostProcessActionResult.failedRetryable(null, truncate(presetError));
        }
        log.info("零件[{}:{}]安全常量补偿成功", context.getPartCode(), context.getSn());
        return PartPostProcessActionResult.success();
    }

    /**
     * 解析当前实例的车载节点代码（当前 part_info 优先，缺失时回退 MDM Part 投影）
     */
    private String resolveNodeCode(PartPostProcessActionContext context) {
        if (context.getPartInfo() != null && StrUtil.isNotBlank(context.getPartInfo().getVehicleNodeCode())) {
            return context.getPartInfo().getVehicleNodeCode();
        }
        if (context.getMdmPart() != null && StrUtil.isNotBlank(context.getMdmPart().getVehicleNodeCode())) {
            return context.getMdmPart().getVehicleNodeCode();
        }
        return null;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 900 ? message : message.substring(0, 897) + "...";
    }
}
