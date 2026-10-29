package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.PrimarySoftwareTargetPolicy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 软件兼容投影服务（CR-071 §5）
 * <p>
 * 从软件实装时态表（唯一完整 SSOT）重算 tb_part_info 的软件兼容投影：
 * software_pn/software_ver 成对更新并携带可判定状态与来源引用
 * （software_snapshot_state/installation_id/target/slot/inventory_version）。
 * <p>
 * 投影规则（§5.1）：主 Target 下 install_state=ACTIVE AND effective_to IS NULL
 * AND is_active_slot=true AND is_confirmed=true 的唯一记录；
 * 无主映射/多候选/仅 provisional/无数据 均给出显式状态，绝不取 SOFTWARE[0]。
 * <p>
 * 写入经 PartInfoMutationAppService（SOFTWARE_SNAPSHOT 通道），与属性历史同事务；
 * 重算结果与当前一致时为 NOOP，不写日志、不增版本、不发伪造软件事件。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartSoftwareSnapshotProjectionService {

    private final PartInfoRepository partInfoRepository;
    private final PartSoftwareInstallationRepository partSoftwareInstallationRepository;
    private final PartInfoMutationAppService partInfoMutationAppService;
    private final PrimarySoftwareTargetPolicy primarySoftwareTargetPolicy;

    /** 投影状态 */
    public static final String STATE_CURRENT = "CURRENT";
    public static final String STATE_AMBIGUOUS = "AMBIGUOUS";
    public static final String STATE_PROVISIONAL = "PROVISIONAL";
    public static final String STATE_UNAVAILABLE = "UNAVAILABLE";
    public static final String STATE_LEGACY_UNVERIFIED = "LEGACY_UNVERIFIED";

    /** 歧义原因 */
    public static final String REASON_PRIMARY_TYPE_UNMAPPED = "PRIMARY_TYPE_UNMAPPED";
    public static final String REASON_MULTIPLE_CURRENT_CANDIDATES = "MULTIPLE_CURRENT_CANDIDATES";

    /**
     * 从当前 SSOT 重算并刷新零件软件兼容投影
     *
     * @param partId 零件实例ID
     * @param context 投影上下文（操作来源/人/阶段）
     * @return 投影结果
     */
    public ProjectionResult refreshFromCurrentSsot(Long partId, ProjectionContext context) {
        PartInfo partInfo = partInfoRepository.selectById(partId);
        if (partInfo == null) {
            return ProjectionResult.skipped("PART_NOT_FOUND");
        }

        // 1. 全部 confirmed ACTIVE 候选（active slot；跨 Target/Slot）
        List<PartSoftwareInstallation> allRecords = partSoftwareInstallationRepository.selectByPartId(partId);
        List<PartSoftwareInstallation> candidates = allRecords.stream()
                .filter(r -> "ACTIVE".equals(r.getInstallState()))
                .filter(r -> r.getEffectiveTo() == null)
                .filter(r -> Boolean.TRUE.equals(r.getIsActiveSlot()))
                .filter(r -> Boolean.TRUE.equals(r.getIsConfirmed()))
                .toList();

        // 2. 按 SOFTWARE_TYPE 优先级选主软件（未配置/缺失类型不参与主选）
        PartSoftwareInstallation selected = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestCount = 0;
        String primaryType = null;
        for (PartSoftwareInstallation candidate : candidates) {
            int priority = primarySoftwareTargetPolicy.priorityOf(candidate.getSoftwareType());
            if (priority < 0) {
                continue;
            }
            if (priority < bestPriority) {
                bestPriority = priority;
                selected = candidate;
                bestCount = 1;
                primaryType = candidate.getSoftwareType();
            } else if (priority == bestPriority) {
                bestCount++;
            }
        }

        // 3. 状态判定
        String state;
        String reason = null;
        String softwareVer = null;
        String softwarePn = null;
        Long installationId = null;
        String slot = null;
        Long inventoryVersion = null;

        if (candidates.isEmpty()) {
            // 无 confirmed 候选：检查是否存在 provisional（ACTIVE + effective_to null + 非 confirmed）
            boolean hasProvisional = allRecords.stream().anyMatch(r ->
                    "ACTIVE".equals(r.getInstallState())
                            && r.getEffectiveTo() == null
                            && !Boolean.TRUE.equals(r.getIsConfirmed()));
            state = hasProvisional ? STATE_PROVISIONAL : STATE_UNAVAILABLE;
        } else if (selected == null) {
            // 有 confirmed 候选但类型缺失/未配置 → 不可判定
            state = STATE_AMBIGUOUS;
            reason = REASON_PRIMARY_TYPE_UNMAPPED;
        } else if (bestCount > 1) {
            // 同优先级多候选
            state = STATE_AMBIGUOUS;
            reason = REASON_MULTIPLE_CURRENT_CANDIDATES;
        } else {
            state = STATE_CURRENT;
            softwareVer = selected.getSoftwareVersion();
            softwarePn = selected.getSoftwarePartNo();
            installationId = selected.getId();
            slot = selected.getSlot();
            inventoryVersion = selected.getInventoryVersion();
        }

        // 4. 构造投影补丁（SOFTWARE_SNAPSHOT 通道，显式覆盖投影列）
        PartInfoPatchCmd.PartInfoPatchCmdBuilder builder = PartInfoPatchCmd.builder()
                .partId(partId)
                .repairPolicy(PartInfoMutationAppService.POLICY_CORRECT_EXPLICIT)
                .changeType("SOFTWARE_SNAPSHOT")
                .operationSource(context.getOperationSource() != null ? context.getOperationSource() : "SOFTWARE_SNAPSHOT")
                .operatorId(context.getOperatorId() != null ? context.getOperatorId() : "SYSTEM")
                .operatorName(context.getOperatorName())
                .source(context.getSource() != null ? context.getSource() : "SYSTEM")
                .sourceEventId(context.getSourceEventId())
                .operationId(context.getOperationId())
                .phase("SOFTWARE_SNAPSHOT")
                .seq(context.getSeq() != null ? context.getSeq() : 1)
                .softwareSnapshotState(state)
                .softwareSnapshotTarget(primaryType);

        if (STATE_CURRENT.equals(state)) {
            builder.softwareVer(softwareVer)
                    .softwarePn(softwarePn)
                    .softwareSnapshotInstallationId(installationId)
                    .softwareSnapshotSlot(slot)
                    .softwareSnapshotInventoryVersion(inventoryVersion);
        } else {
            builder.nullFields(List.of(
                    "softwareVer", "softwarePn",
                    "softwareSnapshotInstallationId", "softwareSnapshotTarget",
                    "softwareSnapshotSlot", "softwareSnapshotInventoryVersion"));
        }

        PartInfoMutationAppService.MutationResult result = partInfoMutationAppService.patch(builder.build());

        // 5. 结果归一
        String outcome;
        switch (result.outcome()) {
            case "APPLIED" -> outcome = "APPLIED";
            case "CONFLICT" -> outcome = "CONFLICT";
            default -> outcome = "NOOP";
        }
        if (!"NOOP".equals(outcome)) {
            log.info("软件投影刷新: partId={}, state={}, outcome={}, reason={}",
                    partId, state, outcome, reason);
        } else {
            log.debug("软件投影刷新无变化: partId={}, state={}", partId, state);
        }
        return ProjectionResult.of(outcome, state, reason, result.rowVersion());
    }

    /**
     * 投影上下文
     */
    @Getter
    @Builder
    public static class ProjectionContext {
        private String operationId;
        private String operationSource;
        private String operatorId;
        private String operatorName;
        private String source;
        private String sourceEventId;
        private Integer seq;
    }

    /**
     * 投影结果
     */
    public record ProjectionResult(
            String outcome, // APPLIED / NOOP / CONFLICT / SKIPPED
            String state,
            String reason,
            Integer rowVersion
    ) {
        public static ProjectionResult of(String outcome, String state, String reason, Integer rowVersion) {
            return new ProjectionResult(outcome, state, reason, rowVersion);
        }

        public static ProjectionResult skipped(String reason) {
            return new ProjectionResult("SKIPPED", null, reason, null);
        }
    }
}
