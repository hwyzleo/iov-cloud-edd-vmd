package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * KL1 交付租约领域实体（VMD-DSN-CR-069 §1/§2）。
 * <p>
 * 每次应用投递一条租约（批次+租约序号唯一，messageId 唯一）。承载本次完整响应 bytes
 * 的受保护存储（payload 或 payloadRef 二选一）与摘要，保证「同一 messageId 不得对应不同
 * bytes」；续签只替换外层 field4，内容字节（C）不变。
 * <p>
 * 仅保存非敏感摘要与审计字段；payload 为 AES-GCM 加密 BLOB 或受控对象引用，禁止明文
 * 密钥/Wrapped Key/parameters 原文。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyProvisioningLease extends BaseDo<Long> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 批次外键（tb_business_key_provisioning.id）
     */
    private Long provisioningId;

    /**
     * 本批次租约序号（从 1 递增，原子签发）
     */
    private Integer leaseSeq;

    /**
     * 租约签发时间（服务端 UTC）
     */
    private LocalDateTime issuedAt;

    /**
     * KL1 E=min(now+60s, B-30s)
     */
    private LocalDateTime expireAt;

    /**
     * 本次应用投递新 messageId（唯一）
     */
    private String messageId;

    /**
     * 应用投递 correlationId（→Request）
     */
    private String correlationId;

    /**
     * 本次完整响应 bytes 的 SHA-256 hex（含本次 E）
     */
    private String deliveryPayloadDigest;

    /**
     * 不可变内容摘要（=批次 immutable_content_digest，租约级校验）
     */
    private String contentDigest;

    /**
     * 受保护原传输 bytes（AES-GCM 加密 BLOB；与 payloadRef 二选一）
     */
    private byte[] payload;

    /**
     * 不可变受控对象引用（与 payload 二选一）
     */
    private String payloadRef;

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
}
