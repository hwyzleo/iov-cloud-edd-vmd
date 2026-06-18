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
 * 车载节点表 持久化对象
 *
 * <p>由 tb_device 重命名迁移而来（CR-020）。
 * MDM VehicleNode（车载节点，原Device设备）字典/类型主数据本地投影。</p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_mdm_vehicle_node")
public class MdmVehicleNodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车载节点代码（原deviceCode，CR-020重命名）
     */
    @TableField("code")
    private String code;

    /**
     * 车载节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 车载节点本地化名称
     */
    @TableField("name_local")
    private String nameLocal;

    /**
     * 设备分类
     */
    @TableField("device_category")
    private String deviceCategory;

    /**
     * 功能域
     */
    @TableField("func_domain")
    private String funcDomain;

    /**
     * 节点类型
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * OTA支持类型
     */
    @TableField("ota_support")
    private String otaSupport;

    /**
     * 是否核心设备
     */
    @TableField("core")
    private Boolean core;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

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
