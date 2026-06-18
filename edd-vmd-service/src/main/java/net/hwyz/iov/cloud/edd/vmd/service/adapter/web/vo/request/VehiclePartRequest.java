package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆零件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehiclePartRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 零件实例ID
     */
    private Long partId;

    /**
     * 车载节点代码
     */
    private String vehicleNodeCode;

    /**
     * 设备项
     */
    private String deviceItem;

    /**
     * 安装位置
     */
    private String position;

    /**
     * 绑定时间
     */
    private Date bindTime;

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
    private Date unbindTime;

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

    /**
     * 备注
     */
    private String description;

}
