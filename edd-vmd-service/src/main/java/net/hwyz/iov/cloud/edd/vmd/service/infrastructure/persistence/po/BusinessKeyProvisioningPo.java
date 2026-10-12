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
 * 业务密钥多用途开通批次表 持久化对象（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_business_key_provisioning")
public class BusinessKeyProvisioningPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("request_digest")
    private String requestDigest;

    @TableField("ack_digest")
    private String ackDigest;

    @TableField("device_sn")
    private String deviceSn;

    @TableField("binding_id")
    private Long bindingId;

    @TableField("part_id")
    private Long partId;

    @TableField("hsm_uid_digest")
    private String hsmUidDigest;

    @TableField("certificate_sn_digest")
    private String certificateSnDigest;

    @TableField("business_domain")
    private String businessDomain;

    @TableField("status")
    private String status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}
