package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyLogicalIdentityConflict;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

/**
 * 根派生材料落库与 claim-or-reuse（VMD-DSN-CR-066 §6.3）。
 * <p>
 * 替换单纯 UPDATE 回填：PENDING 占位行先执行 {@code attachDerivedMaterial}（乐观锁条件 UPDATE，
 * 仅允许 key_id IS NULL 的 PENDING 行）；撞 {@code uk_key_id} 时按逻辑 keyId 定位既有行，做
 * <strong>确定性 claim-or-reuse</strong>——校验不可变身份完全一致后，item 重挂 canonical 目录行、
 * 占位行置 SUPERSEDED 并记录 superseded_by_id，而不是映射为可重试 806100 无限重试。
 * <ul>
 *   <li>身份一致 → 幂等/并发命中，继续响应，不对外报错；</li>
 *   <li>同 keyId 但不可变元数据冲突 → {@link BusinessKeyLogicalIdentityConflict}（不可重试 + 告警）；</li>
 *   <li>FAILED/RECONCILE_REQUIRED 且证据完整 → 同事务 FAILED→PENDING_RECOVERY→PENDING 恢复复用。</li>
 * </ul>
 * 批量开通与轮换共用本服务（CR-066 §6.5）；轮换无 item 时 {@code itemId=null} 跳过重挂。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyDerivedMaterialService {

    private static final String MATERIAL_MODEL_DERIVED_ROOT = "DERIVED_ROOT";
    private static final String REASON_CLAIMED = "LOGICAL_KEY_ALREADY_CLAIMED";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final LogicalKeyGenerationPolicy generationPolicy;
    private final BusinessKeyProvisioningMetrics metrics;

    /**
     * 身份一致性校验上下文（非敏感）。
     *
     * @param deviceSn           设备实例序列号
     * @param businessDomain     业务域
     * @param purpose            用途
     * @param bizType            业务类型名称（身份摘要输入）
     * @param normalizedAnchor   规范化锚原文（VIN/UID，身份摘要输入；不落库不日志）
     * @param requestId          本次 attempt 幂等键（恢复审计引用）
     */
    public record ClaimContext(String deviceSn, String businessDomain, String purpose,
                               String bizType, String normalizedAnchor, String requestId) {
    }

    /**
     * 回填根派生非敏感材料；撞 uk_key_id 时 claim-or-reuse（CR-066 §6.3）。
     *
     * @param pendingRow         目标 PENDING 占位行（key_id 可为 null 或已有值）
     * @param result             Provider 派生封装结果
     * @param policy             授权/密码学策略
     * @param fallbackOperationId 结果缺失 operationId 时的回退操作标识
     * @param itemId             关联的批次明细主键（轮换路径为 null 跳过重挂）
     * @param ctx                身份一致性上下文
     * @return canonical 目录行主键（可能不同于 {@code pendingRow.id}：claim 后为既有 canonical 行）
     */
    public Long persistOrClaimDerivedMaterial(DeviceBusinessKey pendingRow, DerivedWrappedKey result,
                                              BusinessKeyPolicyEntry policy, String fallbackOperationId,
                                              Long itemId, ClaimContext ctx) {
        if (pendingRow.getKeyId() == null) {
            try {
                if (attach(pendingRow, result, policy, fallbackOperationId, ctx) == 1) {
                    metrics.recordClaim("update", "success", "fresh_attach");
                    return pendingRow.getId();
                }
            } catch (DuplicateKeyException e) {
                return claimOrReuse(pendingRow, result, policy, fallbackOperationId, itemId, ctx);
            }
            // attach 返回 0：乐观锁冲突 / 被并发修改，读回判定
            return resolveConcurrentAttach(pendingRow, result, policy, itemId, ctx);
        }
        // 占位行已带 keyId（重试/恢复/REIMPORT 路径）：校验身份一致后直接复用
        verifySameLogicalIdentity(pendingRow, result, policy, ctx);
        metrics.recordClaim("reuse", "success", "existing_key_id");
        return pendingRow.getId();
    }

    /**
     * 条件 UPDATE 回填（仅 PENDING 且 key_id IS NULL，乐观锁保护）。
     */
    private int attach(DeviceBusinessKey pendingRow, DerivedWrappedKey result,
                       BusinessKeyPolicyEntry policy, String fallbackOperationId, ClaimContext ctx) {
        DeviceBusinessKey attachRow = DeviceBusinessKey.builder()
                .id(pendingRow.getId())
                .rowVersion(pendingRow.getRowVersion())
                .keyId(result.logicalKeyId())
                .kmsKeyRef(result.kmsKeyRef())
                .rootKeyVersion(result.rootKeyVersion())
                .derivationVersion(result.derivationVersion())
                .operationId(result.operationId() != null ? result.operationId() : fallbackOperationId)
                .algorithm(policy.algorithm())
                .keySpec(policy.keySpec())
                .logicalIdentityDigest(generationPolicy.logicalIdentityDigest(
                        ctx.bizType(), ctx.normalizedAnchor(), ctx.purpose(),
                        result.rootKeyVersion(), result.derivationVersion()))
                .build();
        return deviceBusinessKeyRepository.attachDerivedMaterial(attachRow);
    }

    /**
     * 撞 uk_key_id 后的确定性 claim-or-reuse（CR-066 §6.3）。
     */
    private Long claimOrReuse(DeviceBusinessKey pendingRow, DerivedWrappedKey result,
                              BusinessKeyPolicyEntry policy, String fallbackOperationId,
                              Long itemId, ClaimContext ctx) {
        DeviceBusinessKey existing = deviceBusinessKeyRepository.selectByKeyIdForUpdate(result.logicalKeyId());
        if (existing == null) {
            // uk_key_id 冲突但按 keyId 查无既有行（极端竞态/逻辑删除）：重试 attach 一次
            log.warn("keyprov claim 冲突但按 keyId 无既有行，重试回填: requestId={}, purpose={}",
                    ctx.requestId(), ctx.purpose());
            if (attach(pendingRow, result, policy, fallbackOperationId, ctx) == 1) {
                metrics.recordClaim("update", "success", "retry_attach");
                return pendingRow.getId();
            }
            throw new BusinessKeyLogicalIdentityConflict(
                    "uk_key_id 冲突但按 keyId 无既有行，无法收敛: requestId=" + ctx.requestId());
        }
        // 身份一致性校验（完全一致才允许复用；任一不可变事实冲突 → 不可重试）
        verifySameLogicalIdentity(existing, result, policy, ctx);
        // 状态可复用性 + FAILED/RECONCILE_REQUIRED 恢复
        recoverIfNeeded(existing, ctx);
        // item 重挂 canonical 目录行（同一事务）
        if (itemId != null) {
            provisioningItemRepository.rebindDirectoryKey(itemId, existing.getId(),
                    existing.getKeyId(), existing.getBusinessKeyVersion());
        }
        // 占位行置 SUPERSEDED（保留证据，不删除）
        deviceBusinessKeyRepository.markPlaceholderSuperseded(pendingRow.getId(), existing.getId(), REASON_CLAIMED);
        metrics.recordClaim("reuse", "success", "claimed_existing");
        metrics.recordRetryLoopPrevented("claim_converged");
        log.info("keyprov 目录材料 claim-or-reuse 收敛: requestId={}, purpose={}, placeholderId={}, canonicalId={}, state={}",
                ctx.requestId(), ctx.purpose(), pendingRow.getId(), existing.getId(),
                existing.getKeyState() == null ? null : existing.getKeyState().getValue());
        return existing.getId();
    }

    /**
     * attach 返回 0（乐观锁冲突/被并发修改）时的读回判定。
     */
    private Long resolveConcurrentAttach(DeviceBusinessKey pendingRow, DerivedWrappedKey result,
                                         BusinessKeyPolicyEntry policy, Long itemId, ClaimContext ctx) {
        DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(pendingRow.getId());
        if (current == null) {
            throw new BusinessKeyLogicalIdentityConflict(
                    "占位行不存在，无法收敛: id=" + pendingRow.getId() + ", requestId=" + ctx.requestId());
        }
        if (current.getKeyId() != null) {
            // 并发已回填：校验身份一致后复用本行
            verifySameLogicalIdentity(current, result, policy, ctx);
            metrics.recordClaim("reuse", "success", "concurrent_attach");
            return current.getId();
        }
        if (current.getKeyState() == BusinessKeyState.SUPERSEDED && current.getSupersededById() != null) {
            // 本占位已被并发 claim：跟随 canonical
            metrics.recordClaim("reuse", "success", "follow_superseded");
            return current.getSupersededById();
        }
        metrics.recordIdentityConflict("state_not_reusable");
        throw new BusinessKeyLogicalIdentityConflict(
                "占位行被并发修改且无 keyId 证据，无法收敛: id=" + pendingRow.getId() + ", requestId=" + ctx.requestId());
    }

    /**
     * 不可变身份一致性校验（CR-066 §6.3）。
     * <p>设备/VIN 锚：目录行不落锚原文，keyId/kmsKeyRef 为确定性标识，keyId 相等已等价锚一致；
     * 此处比对上下文、材料模型、kmsKeyRef、rootKeyVersion、derivationVersion、算法/keySpec、policyVersion
     * 与（经关联 item 的）KCV 证据。任一不一致 → {@link BusinessKeyLogicalIdentityConflict}（不可重试 + 告警）。
     */
    private void verifySameLogicalIdentity(DeviceBusinessKey existing, DerivedWrappedKey result,
                                           BusinessKeyPolicyEntry policy, ClaimContext ctx) {
        if (!Objects.equals(existing.getDeviceSn(), ctx.deviceSn())
                || !Objects.equals(existing.getBusinessDomain(), ctx.businessDomain())
                || !Objects.equals(existing.getPurpose(), ctx.purpose())) {
            throw conflict("context_mismatch",
                    "同keyId但设备/域/用途上下文不一致: keyId=" + result.logicalKeyId());
        }
        if (existing.getMaterialModel() == null || !MATERIAL_MODEL_DERIVED_ROOT.equals(existing.getMaterialModel())) {
            throw conflict("material_model_mismatch",
                    "既有行材料模型非 DERIVED_ROOT 或来源未知，禁止复用: keyId=" + result.logicalKeyId());
        }
        if (!Objects.equals(existing.getKmsKeyRef(), result.kmsKeyRef())) {
            throw conflict("kms_ref_mismatch", "同keyId但 kmsKeyRef 不一致");
        }
        if (existing.getRootKeyVersion() == null || existing.getRootKeyVersion() != result.rootKeyVersion()) {
            throw conflict("root_mismatch",
                    "同keyId但 rootKeyVersion 不一致: existing=" + existing.getRootKeyVersion()
                            + ", result=" + result.rootKeyVersion());
        }
        if (!Objects.equals(existing.getDerivationVersion(), result.derivationVersion())) {
            throw conflict("derivation_mismatch", "同keyId但 derivationVersion 不一致");
        }
        if (!Objects.equals(existing.getAlgorithm(), policy.algorithm())
                || !Objects.equals(existing.getKeySpec(), policy.keySpec())) {
            throw conflict("algorithm_mismatch", "同keyId但 算法/keySpec 与当前策略不一致");
        }
        if (existing.getPolicyVersion() != null && policy.policyVersion() != null
                && !Objects.equals(existing.getPolicyVersion(), policy.policyVersion())) {
            throw conflict("policy_mismatch", "同keyId但 policyVersion 不一致");
        }
        // KCV：目录行不落 KCV，经既有行关联 item 的 keyCheckValue 证据比对（无证据则跳过）
        BusinessKeyProvisioningItem existingItem = provisioningItemRepository.selectByDirectoryKeyId(existing.getId());
        if (existingItem != null && existingItem.getKeyCheckValue() != null && result.kcv() != null
                && !Arrays.equals(existingItem.getKeyCheckValue(), result.kcv())) {
            throw conflict("kcv_mismatch", "同keyId但 KCV 证据不一致");
        }
    }

    /**
     * 状态可复用性校验与恢复（CR-066 §6.4）。
     * <ul>
     *   <li>ACTIVE/PENDING：直接复用（REIMPORT 收敛 / 并发 claim）；</li>
     *   <li>FAILED/RECONCILE_REQUIRED 且证据完整：同事务 FAILED→PENDING_RECOVERY→PENDING 恢复复用；</li>
     *   <li>证据不完整（keyId/rootKeyVersion/derivationVersion/kmsKeyRef 缺失）→ 不可恢复，冲突；</li>
     *   <li>DEPRECATED/REVOKED/EXPIRED/SUPERSEDED → 不可复用，冲突。</li>
     * </ul>
     */
    private void recoverIfNeeded(DeviceBusinessKey existing, ClaimContext ctx) {
        BusinessKeyState state = existing.getKeyState();
        if (state == BusinessKeyState.ACTIVE || state == BusinessKeyState.PENDING) {
            return;
        }
        if (state == BusinessKeyState.PENDING_RECOVERY) {
            // 并发恢复中间态：完成另一事务发起的恢复（PENDING_RECOVERY→PENDING）
            int r2 = deviceBusinessKeyRepository.confirmRecovery(existing.getId(), ctx.requestId());
            if (r2 != 1) {
                throw conflict("state_not_reusable", "恢复确认失败（状态已变）: id=" + existing.getId());
            }
            metrics.recordClaim("reuse", "recovered", "pending_recovery_confirmed");
            return;
        }
        if (state == BusinessKeyState.FAILED || state == BusinessKeyState.RECONCILE_REQUIRED) {
            // 证据完整性防御（身份校验已保证 root/derivation/kmsKeyRef 一致；此处兜底 keyId 证据）
            if (existing.getKeyId() == null) {
                throw conflict("evidence_incomplete",
                        "FAILED/RECONCILE_REQUIRED 行证据不完整，不可恢复: keyId=" + existing.getKeyId());
            }
            int r1 = deviceBusinessKeyRepository.markPendingRecovery(existing.getId(), "CR066_LOGICAL_KEY_RECOVERY");
            if (r1 != 1) {
                throw conflict("state_not_reusable", "恢复入口失败（状态已变）: id=" + existing.getId());
            }
            int r2 = deviceBusinessKeyRepository.confirmRecovery(existing.getId(), ctx.requestId());
            if (r2 != 1) {
                throw conflict("state_not_reusable", "恢复确认失败（状态已变）: id=" + existing.getId());
            }
            metrics.recordClaim("reuse", "recovered", "failed_evidence_recovered");
            metrics.recordRetryLoopPrevented("failed_recovered");
            log.info("keyprov 目录材料恢复复用: requestId={}, purpose={}, rowId={}, fromState={}",
                    ctx.requestId(), ctx.purpose(), existing.getId(), state.getValue());
            return;
        }
        throw conflict("state_not_reusable",
                "目录行状态不可复用: state=" + (state == null ? null : state.getValue()));
    }

    private BusinessKeyLogicalIdentityConflict conflict(String reason, String detail) {
        metrics.recordIdentityConflict(reason);
        return new BusinessKeyLogicalIdentityConflict(detail);
    }
}
