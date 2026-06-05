package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.experimental.SuperBuilder;
import lombok.*;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 品牌持久化对象 - 对应 tb_veh_brand 表
 * 
 * <p>该表同时承载 MDM Brand 投影数据（source=MDM）和历史手动维护数据（source=MANUAL）。</p>
 * 
 * <p>MDM 投影字段（source, external_ref_id, external_version, last_sync_time）
 * 由 MdmSyncAppService 在事件订阅和 Bootstrap 时写入。</p>
 * 
 * <p>数据来源规则：</p>
 * <ul>
 *   <li>source=MDM：只读，禁止通过 MPT 后台修改/删除</li>
 *   <li>source=MANUAL：兼容期遗留数据，允许有限维护</li>
 * </ul>
 * 
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_brand")
public class VehBrandPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 品牌代码（brandCode 关联键）
     */
    @TableField("code")
    private String code;

    /**
     * 品牌名称
     */
    @TableField("name")
    private String name;

    /**
     * 品牌英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 是否启用
     */
    @TableField("enable")
    private Boolean enable;

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
