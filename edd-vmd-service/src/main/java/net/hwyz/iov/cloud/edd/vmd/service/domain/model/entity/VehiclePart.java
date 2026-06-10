package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 车辆-零件绑定关系领域对象
 * <p>
 * 纯绑定关系，承载装车位置、时间、状态、换件溯源
 * 实例本体属性在 PartInfo 中
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehiclePart implements DomainObj<VehiclePart> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号（关联tb_veh_basic_info.vin）
     */
    private String vin;

    /**
     * 零件实例ID（关联tb_part_info.id）
     */
    private Long partId;

    /**
     * 车载节点代码（关联tb_mdm_vehicle_node.code）
     */
    private String vehicleNodeCode;

    /**
     * 设备项（安装位置快照）
     */
    private String deviceItem;

    /**
     * 绑定时间
     */
    private Instant bindTime;

    /**
     * 绑定类型
     */
    private String bindType;

    /**
     * 绑定者
     */
    private String bindBy;

    /**
     * 绑定机构
     */
    private String bindOrg;

    /**
     * 解绑时间
     */
    private Instant unbindTime;

    /**
     * 解绑理由
     */
    private String unbindReason;

    /**
     * 解绑者
     */
    private String unbindBy;

    /**
     * 解绑机构
     */
    private String unbindOrg;

    /**
     * 绑定状态：0-已解绑，1-绑定中
     */
    private Integer bindState;

    /**
     * 换件溯源：被替换的绑定ID
     */
    private Long replaceOfBindingId;

}
