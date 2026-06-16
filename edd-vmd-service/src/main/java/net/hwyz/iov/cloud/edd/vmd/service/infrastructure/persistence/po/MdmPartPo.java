package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 零件信息表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-26
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_mdm_part")
public class MdmPartPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件号（对齐MDM code）
     */
    @TableField("code")
    private String code;

    /**
     * 零件中文名称
     */
    @TableField("name")
    private String name;

    /**
     * 本地化名称
     */
    @TableField("name_local")
    private String nameLocal;

    /**
     * 零件类型
     */
    @TableField("part_type")
    private String partType;

    /**
     * 是否是软件
     */
    @TableField("is_software")
    @Builder.Default
    private Boolean isSoftware = false;

    /**
     * 零件状态：PRODUCTION-量产，TRIAL-试生产，DISCONTINUE-停用
     */
    @TableField("status")
    private String status;

    /**
     * 是否精准追溯
     */
    @TableField("is_accurately_traced")
    private Boolean isAccuratelyTraced;

    /**
     * 车辆节点代码
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 供应商代码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 是否支持FOTA升级
     */
    @TableField("fota_upgradeable")
    private Boolean fotaUpgradeable;

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
