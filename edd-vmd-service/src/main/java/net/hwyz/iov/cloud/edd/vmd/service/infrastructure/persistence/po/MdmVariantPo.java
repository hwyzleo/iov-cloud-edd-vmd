package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import lombok.experimental.SuperBuilder;
import lombok.*;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 车辆版本表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-01-19
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_mdm_variant")
public class MdmVariantPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车型代码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 版本代码
     */
    @TableField("code")
    private String code;

    /**
     * 版本名称
     */
    @TableField("name")
    private String name;

    /**
     * 版本本地化名称
     */
    @TableField("name_local")
    private String nameLocal;

    /**
     * 数据来源：MDM=来自MDM系统，MANUAL=本地手动维护
     */
    @TableField("source")
    private String source;

    /**
     * MDM侧实体主键ID
     */
    @TableField("external_ref_id")
    private String externalRefId;

    /**
     * MDM侧实体版本号
     */
    @TableField("external_version")
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;
}
