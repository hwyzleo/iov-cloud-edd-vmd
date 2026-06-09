package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_option_code")
public class VehOptionCodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("option_family_code")
    private String optionFamilyCode;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("name_en")
    private String nameEn;

    @TableField("val")
    private String val;

    @TableField("enable")
    private Boolean enable;

    @TableField("sort")
    private Integer sort;

    @TableField("source")
    private String source;

    @TableField("external_ref_id")
    private String externalRefId;

    @TableField("external_version")
    private Long externalVersion;

    @TableField("last_sync_time")
    private java.time.LocalDateTime lastSyncTime;
}
