package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 对外服务车辆-零件绑定关系信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartBindingExResponse {

    /**
     * 绑定ID（= vehicle_part.id）
     */
    private Long bindingId;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 零件编码
     */
    private String partCode;

    /**
     * 零件序列号
     */
    private String sn;

    /**
     * 设备分类（取自 mdm_vehicle_node.device_category）
     */
    private String deviceCategory;

    /**
     * 车载节点代码
     */
    private String vehicleNodeCode;

    /**
     * 绑定时间
     */
    private Instant bindTime;

}