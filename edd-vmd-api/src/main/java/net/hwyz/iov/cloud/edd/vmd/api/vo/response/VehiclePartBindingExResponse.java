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

    /**
     * 安全芯片/HSM 唯一标识（= tb_part_security_constant.chip_uid）
     * <p>
     * SEC-VIAM-DSN-CR-006：供 VIAM 登记安全主体使用；缺失时为 null，调用方不得以 SN/VIN 顶替。
     * 仅追加字段，既有调用方（TSP 等）反序列化行为不变。
     * </p>
     */
    private String chipUid;

}