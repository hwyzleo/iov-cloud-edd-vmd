package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 业务密钥开通批次逐用途 ACK 明细领域实体（CR-058 D35）
 * <p>
 * 每批次每 purpose 一行，关联 device_business_key.id；仅保存导入证明摘要，
 * 不保存 Wrapped Key、parameters 原文或任何可恢复密钥材料。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisioningItem extends BaseDo<Long> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 批次外键
     */
    private Long provisioningId;

    /**
     * UPLINK_AEAD 或 DOWNLINK_AEAD
     */
    private String purpose;

    /**
     * 对应 device_business_key.key_id
     */
    private String keyId;

    /**
     * VMD 业务版本
     */
    private Long businessKeyVersion;

    /**
     * device_business_key.id
     */
    private Long directoryKeyId;

    /**
     * 设备导入结果
     */
    private ProvisioningDeviceResult deviceResult;

    /**
     * 非敏感导入证明摘要，可空
     */
    private String importProofDigest;

    /**
     * 单用途确认时间
     */
    private LocalDateTime confirmedAt;

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
     * 置为导入成功并记录证明摘要
     */
    public void markSucceeded(String importProofDigest, LocalDateTime confirmedAt) {
        this.deviceResult = ProvisioningDeviceResult.SUCCEEDED;
        this.importProofDigest = importProofDigest;
        this.confirmedAt = confirmedAt;
    }

    /**
     * 置为明确导入失败
     */
    public void markFailed(String importProofDigest) {
        this.deviceResult = ProvisioningDeviceResult.FAILED;
        this.importProofDigest = importProofDigest;
    }

    /**
     * 置为导入结果未知
     */
    public void markUnknown(String importProofDigest) {
        this.deviceResult = ProvisioningDeviceResult.UNKNOWN;
        this.importProofDigest = importProofDigest;
    }
}
