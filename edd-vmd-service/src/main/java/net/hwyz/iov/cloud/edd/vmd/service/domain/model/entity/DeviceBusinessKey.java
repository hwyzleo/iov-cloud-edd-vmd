package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 设备业务密钥目录领域实体
 * <p>
 * CR-055：VMD 权威维护 (device_sn, business_domain, purpose) → active keyId/businessKeyVersion。
 * 每业务版本独立成行、不覆盖历史；同一设备+业务域+用途最多一个 ACTIVE。
 * 仅存 KMS 引用与元数据，禁止明文密钥、解封密钥、设备私钥和完整 Wrapped Key。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceBusinessKey extends BaseDo<Long> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 设备实例序列号快照
     */
    private String deviceSn;

    /**
     * 创建时 active vehicle_part.id
     */
    private Long bindingId;

    /**
     * 设备物理实例，关联 part_info.id
     */
    private Long partId;

    /**
     * 创建时权威安全芯片 UID 快照
     */
    private String hsmUid;

    /**
     * 受治理业务域代码
     */
    private String businessDomain;

    /**
     * 受治理用途代码
     */
    private String purpose;

    /**
     * framework/KMS 返回的不透明标识，唯一
     */
    private String keyId;

    /**
     * VMD 业务版本，同上下文单调递增
     */
    private Long businessKeyVersion;

    /**
     * KMS 引用，不是密钥本体
     */
    private String kmsKeyRef;

    /**
     * KMS/provider 内部版本，仅审计与对账
     */
    private Integer kmsKeyVersion;

    /**
     * KMS 提供方
     */
    private String kmsProvider;

    /**
     * 算法快照
     */
    private String algorithm;

    /**
     * 密钥规格快照
     */
    private String keySpec;

    /**
     * 业务密钥状态
     */
    private BusinessKeyState keyState;

    /**
     * 有效窗口开始
     */
    private LocalDateTime validFrom;

    /**
     * 有效窗口结束
     */
    private LocalDateTime validTo;

    /**
     * DEPRECATED 仅解密截止时间
     */
    private LocalDateTime decryptUntil;

    /**
     * 首期固定 DEVICE_CERT_PUBLIC_KEY
     */
    private String wrapMode;

    /**
     * 最近设备封装使用的证书序列号，仅审计
     */
    private String lastRecipientCertSn;

    /**
     * 调用幂等键
     */
    private String requestId;

    /**
     * 规范化请求摘要（参数冲突识别）
     */
    private String requestDigest;

    /**
     * 创建时授权/密码学策略版本
     */
    private String policyVersion;

    /**
     * 最近失败原因，失败重试与诊断
     */
    private String failReason;

    /**
     * 最近尝试时间
     */
    private LocalDateTime lastAttemptTime;

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
     * 置为 ACTIVE（创建成功或轮换切换时调用，由调用方保证单 ACTIVE 约束）
     */
    public void activate() {
        this.keyState = BusinessKeyState.ACTIVE;
    }

    /**
     * 置为 DEPRECATED（轮换后旧版本）
     */
    public void deprecate(LocalDateTime decryptUntil) {
        this.keyState = BusinessKeyState.DEPRECATED;
        this.decryptUntil = decryptUntil;
    }

    /**
     * 置为 REVOKING（吊销阻断态，立即阻断新加解密）
     */
    public void markRevoking(String reason) {
        this.keyState = BusinessKeyState.REVOKING;
        this.failReason = reason;
    }

    /**
     * 置为 REVOKED（吊销成功，终态）
     */
    public void markRevoked() {
        this.keyState = BusinessKeyState.REVOKED;
        this.decryptUntil = null;
    }

    /**
     * 置为 EXPIRED（超过 decrypt_until）
     */
    public void markExpired() {
        this.keyState = BusinessKeyState.EXPIRED;
    }

    /**
     * 置为 FAILED（创建失败，可重试/补偿）
     */
    public void markFailed(String reason) {
        this.keyState = BusinessKeyState.FAILED;
        this.failReason = reason;
        this.lastAttemptTime = LocalDateTime.now();
    }

    /**
     * 置为 RECONCILE_REQUIRED（framework 结果未知，需以原幂等键对账）
     */
    public void markReconcileRequired(String reason) {
        this.keyState = BusinessKeyState.RECONCILE_REQUIRED;
        this.failReason = reason;
        this.lastAttemptTime = LocalDateTime.now();
    }
}
