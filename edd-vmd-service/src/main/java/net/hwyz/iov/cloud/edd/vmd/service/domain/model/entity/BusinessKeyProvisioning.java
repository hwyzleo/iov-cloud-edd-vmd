package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 业务密钥多用途开通批次领域实体（CR-058 D35）
 * <p>
 * 以 TBOX 稳定 requestId 聚合同一次请求中的 UPLINK_AEAD、DOWNLINK_AEAD 两用途；
 * 目录 ACTIVE 与“设备已完成件内导入”分离，由本批次状态与 ACK 明细表达。
 * 仅保存非敏感摘要与审计时间，禁止明文密钥、完整 Wrapped Key、设备私钥、可恢复密钥材料。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisioning extends BaseDo<Long> {

    /**
     * 主键
     */
    private Long id;

    /**
     * TBOX 稳定 requestId（批次幂等键）
     */
    private String requestId;

    /**
     * 规范化请求摘要（幂等冲突识别）
     */
    private String requestDigest;

    /**
     * 规范化 ACK 摘要；未确认时为空
     */
    private String ackDigest;

    /**
     * TBOX 序列号
     */
    private String deviceSn;

    /**
     * 发起时 active vehicle_part.id 快照
     */
    private Long bindingId;

    /**
     * 设备物理实例，关联 part_info.id
     */
    private Long partId;

    /**
     * HSM UID 摘要，仅用于一致性校验和审计
     */
    private String hsmUidDigest;

    /**
     * 设备证书序列号摘要
     */
    private String certificateSnDigest;

    /**
     * 业务域；首期固定 V2C_COMM
     */
    private String businessDomain;

    /**
     * 批次状态
     */
    private BusinessKeyProvisioningState status;

    /**
     * 非敏感失败原因
     */
    private String failReason;

    /**
     * 开通开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 首次完整 ACK 时间
     */
    private LocalDateTime acknowledgedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime modifyTime;

    /**
     * 记录版本（乐观锁）
     */
    private Integer rowVersion;

    /**
     * 初始化实体状态
     */
    public void init() {
        stateInit();
    }

    /**
     * 推进到等待设备导入确认（两用途创建/解析并完成 Wrap）
     * <p>
     * 批次已成功推进，清除上一次可重试失败留下的原因，避免对账时读到过期原因。
     */
    public void markWaitDeviceAck() {
        this.status = BusinessKeyProvisioningState.WAIT_DEVICE_ACK;
        this.failReason = null;
    }

    /**
     * 记录确定性可重试的依赖不可用原因，批次保持未终结（PREPARING），以便车端按
     * {@code RETRY_SAME_REQUEST_ID} 以原 requestId 重新派生（VMD-DSN-CR-061 RD-061-5）。
     * <p>
     * 与 {@link #markFailed(String)} 的终态语义严格区分：根派生/Transit 根依赖不可用是可恢复失败，
     * 不得把批次终态化——否则同 requestId 重试会因状态不可推进被拒（806094），车端陷入死循环。
     *
     * @param reason 非敏感失败原因（Provider 分类文案，不含密钥材料）
     */
    public void noteDependencyBlocked(String reason) {
        this.failReason = reason;
    }

    /**
     * 置为已完成（完整 ACK 均 SUCCEEDED，两目标版本已原子切换 ACTIVE）
     */
    public void markCompleted() {
        this.status = BusinessKeyProvisioningState.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.acknowledgedAt = this.acknowledgedAt != null ? this.acknowledgedAt : this.completedAt;
    }

    /**
     * 置为明确不可恢复失败
     */
    public void markFailed(String reason) {
        this.status = BusinessKeyProvisioningState.FAILED;
        this.failReason = reason;
    }

    /**
     * 置为待对账（ACK/依赖结果未知）
     */
    public void markReconcileRequired(String reason) {
        this.status = BusinessKeyProvisioningState.RECONCILE_REQUIRED;
        this.failReason = reason;
    }

    /**
     * 置为已过期（超过策略时限且无有效确认）
     */
    public void markExpired(String reason) {
        this.status = BusinessKeyProvisioningState.EXPIRED;
        this.failReason = reason;
    }

    /**
     * 运维逻辑终止为 FAILED（VMD-DSN-CR-065 RD-065-2/RD-065-3）。
     * <p>
     * 仅允许 PREPARING / RECONCILE_REQUIRED → FAILED；COMPLETED 已有合法成功事实，永不允许逆转；
     * FAILED/EXPIRED 为终态，重复调用幂等不变更。
     * 由调用方保证安全门禁（成功事实/目录归属/operation 无副作用）通过后才可调用。
     *
     * @param reasonCode       受治理终止原因码（写入 fail_reason 的紧凑前缀）
     * @param sanitizedReason  清理后的自由文本上下文（非敏感、不含操作者/敏感材料）
     * @param now              操作时间
     * @return 是否发生状态变更；false 表示已是 FAILED/EXPIRED 终态或状态不允许
     */
    public boolean terminateByOperator(TerminationReasonCode reasonCode, String sanitizedReason, java.time.LocalDateTime now) {
        if (status == BusinessKeyProvisioningState.FAILED || status == BusinessKeyProvisioningState.EXPIRED) {
            // 幂等：保留原终态
            return false;
        }
        if (status != BusinessKeyProvisioningState.PREPARING
                && status != BusinessKeyProvisioningState.RECONCILE_REQUIRED) {
            // COMPLETED 等状态不允许运维终止
            return false;
        }
        this.status = BusinessKeyProvisioningState.FAILED;
        this.failReason = buildTerminationReason(reasonCode, sanitizedReason);
        this.modifyTime = now;
        return true;
    }

    /**
     * 构造紧凑、非敏感的终止原因：终止码 + 清理后的上下文；审计关联号由调用方拼入上下文。
     */
    private String buildTerminationReason(TerminationReasonCode reasonCode, String sanitizedReason) {
        String code = reasonCode != null ? reasonCode.getValue() : "MANUAL_INCIDENT_RECOVERY";
        if (sanitizedReason == null || sanitizedReason.isBlank()) {
            return code;
        }
        return code + "|" + sanitizedReason;
    }
}
