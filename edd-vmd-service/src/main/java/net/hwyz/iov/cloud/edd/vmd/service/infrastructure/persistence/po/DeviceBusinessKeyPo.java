package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 设备业务密钥目录表 持久化对象
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_device_business_key")
public class DeviceBusinessKeyPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备实例序列号快照
     */
    @TableField("device_sn")
    private String deviceSn;

    /**
     * 创建时active vehicle_part.id
     */
    @TableField("binding_id")
    private Long bindingId;

    /**
     * 设备物理实例，关联part_info.id
     */
    @TableField("part_id")
    private Long partId;

    /**
     * 创建时权威安全芯片UID快照
     */
    @TableField("hsm_uid")
    private String hsmUid;

    /**
     * 受治理业务域代码
     */
    @TableField("business_domain")
    private String businessDomain;

    /**
     * 受治理用途代码
     */
    @TableField("purpose")
    private String purpose;

    /**
     * 材料模型：DERIVED_ROOT / RANDOM_MATERIAL（历史记录保持 null）
     */
    @TableField("material_model")
    private String materialModel;

    /**
     * framework/KMS返回的不透明标识，唯一
     */
    @TableField("key_id")
    private String keyId;

    /**
     * VMD业务版本，同上下文单调递增
     */
    @TableField("business_key_version")
    private Long businessKeyVersion;

    /**
     * KMS引用，不是密钥本体
     */
    @TableField("kms_key_ref")
    private String kmsKeyRef;

    /**
     * KMS/provider内部版本，仅审计与对账
     */
    @TableField("kms_key_version")
    private Integer kmsKeyVersion;

    /**
     * Transit/HSM根版本（仅根派生型）
     */
    @TableField("root_key_version")
    private Long rootKeyVersion;

    /**
     * 派生格式版本（首期v1）
     */
    @TableField("derivation_version")
    private String derivationVersion;

    /**
     * KMS提供方
     */
    @TableField("kms_provider")
    private String kmsProvider;

    /**
     * 算法快照
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 密钥规格快照
     */
    @TableField("key_spec")
    private String keySpec;

    /**
     * 业务密钥状态
     */
    @TableField("key_state")
    private String keyState;

    /**
     * 有效窗口开始
     */
    @TableField("valid_from")
    private LocalDateTime validFrom;

    /**
     * 有效窗口结束
     */
    @TableField("valid_to")
    private LocalDateTime validTo;

    /**
     * DEPRECATED仅解密截止时间
     */
    @TableField("decrypt_until")
    private LocalDateTime decryptUntil;

    /**
     * 首期固定DEVICE_CERT_PUBLIC_KEY
     */
    @TableField("wrap_mode")
    private String wrapMode;

    /**
     * 最近设备封装使用的证书序列号，仅审计
     */
    @TableField("last_recipient_cert_sn")
    private String lastRecipientCertSn;

    /**
     * 调用幂等键
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 根派生操作标识（对账主键，不透明）
     */
    @TableField("operation_id")
    private String operationId;

    /**
     * 被取代后指向canonical目录行（SUPERSEDED行必填）
     */
    @TableField("superseded_by_id")
    private Long supersededById;

    /**
     * 非敏感规范化身份摘要（BINARY32，辅助预检与审计）
     */
    @TableField("logical_identity_digest")
    private byte[] logicalIdentityDigest;

    /**
     * 同目录材料恢复次数（FAILED→PENDING_RECOVERY→PENDING）
     */
    @TableField("recovery_count")
    private Integer recoveryCount;

    /**
     * 最近恢复attempt审计引用
     */
    @TableField("last_recovery_request_id")
    private String lastRecoveryRequestId;

    /**
     * 规范化请求摘要（参数冲突识别）
     */
    @TableField("request_digest")
    private String requestDigest;

    /**
     * 创建时授权/密码学策略版本
     */
    @TableField("policy_version")
    private String policyVersion;

    /**
     * 最近失败原因，失败重试与诊断
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 最近尝试时间
     */
    @TableField("last_attempt_time")
    private LocalDateTime lastAttemptTime;
}
