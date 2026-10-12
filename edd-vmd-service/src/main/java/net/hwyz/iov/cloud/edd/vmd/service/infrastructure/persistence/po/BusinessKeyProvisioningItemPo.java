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
 * 业务密钥开通批次 ACK 明细表 持久化对象（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_business_key_provisioning_item")
public class BusinessKeyProvisioningItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("provisioning_id")
    private Long provisioningId;

    @TableField("purpose")
    private String purpose;

    @TableField("key_id")
    private String keyId;

    @TableField("business_key_version")
    private Long businessKeyVersion;

    @TableField("directory_key_id")
    private Long directoryKeyId;

    @TableField("device_result")
    private String deviceResult;

    @TableField("import_proof_digest")
    private String importProofDigest;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
}
