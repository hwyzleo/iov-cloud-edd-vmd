package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * KL1 交付租约表 持久化对象（VMD-DSN-CR-069）
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_keyprov_delivery_lease")
public class KeyProvisioningLeasePo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("provisioning_id")
    private Long provisioningId;

    @TableField("lease_seq")
    private Integer leaseSeq;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("message_id")
    private String messageId;

    @TableField("correlation_id")
    private String correlationId;

    @TableField("delivery_payload_digest")
    private String deliveryPayloadDigest;

    @TableField("content_digest")
    private String contentDigest;

    @TableField("payload")
    private byte[] payload;

    @TableField("payload_ref")
    private String payloadRef;

    @TableField("row_version")
    private Integer rowVersion;
}
