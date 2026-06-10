package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.Instant;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆-零件绑定关系表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_part")
public class VehiclePartPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号（关联tb_veh_basic_info.vin）
     */
    @TableField("vin")
    private String vin;

    /**
     * 零件实例ID（关联tb_part_info.id）
     */
    @TableField("part_id")
    private Long partId;

    /**
     * 车载节点代码（关联tb_mdm_vehicle_node.code）
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 设备项（安装位置快照）
     */
    @TableField("device_item")
    private String deviceItem;

    /**
     * 绑定时间
     */
    @TableField("bind_time")
    private Instant bindTime;

    /**
     * 绑定类型
     */
    @TableField("bind_type")
    private String bindType;

    /**
     * 绑定者
     */
    @TableField("bind_by")
    private String bindBy;

    /**
     * 绑定机构
     */
    @TableField("bind_org")
    private String bindOrg;

    /**
     * 解绑时间
     */
    @TableField("unbind_time")
    private Instant unbindTime;

    /**
     * 解绑理由
     */
    @TableField("unbind_reason")
    private String unbindReason;

    /**
     * 解绑者
     */
    @TableField("unbind_by")
    private String unbindBy;

    /**
     * 解绑机构
     */
    @TableField("unbind_org")
    private String unbindOrg;

    /**
     * 绑定状态：0-已解绑，1-绑定中
     */
    @TableField("bind_state")
    private Integer bindState;

    /**
     * 换件溯源：被替换的绑定ID
     */
    @TableField("replace_of_binding_id")
    private Long replaceOfBindingId;

    /**
     * 生成列：active时的part_id，用于唯一约束
     */
    @TableField("active_part_id")
    private Long activePartId;

    /**
     * 生成列：active时的vin+vehicle_node_code，用于唯一约束
     */
    @TableField("active_vin_node")
    private String activeVinNode;
}
