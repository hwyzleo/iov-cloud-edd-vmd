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
 * 安全灌注机注册表 持久化对象
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_prov_facility_device")
public class ProvFacilityDevicePo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("facility_uid")
    private String facilityUid;

    @TableField("facility_type")
    private String facilityType;

    @TableField("preset_state")
    private String presetState;

    @TableField("kms_provider")
    private String kmsProvider;

    @TableField("kms_key_ref")
    private String kmsKeyRef;

    @TableField("key_spec")
    private String keySpec;

    @TableField("algorithm")
    private String algorithm;

    @TableField("kcv")
    private String kcv;

    @TableField("fail_reason")
    private String failReason;

    @TableField("gen_time")
    private LocalDateTime genTime;

    @TableField("last_attempt_time")
    private LocalDateTime lastAttemptTime;
}
