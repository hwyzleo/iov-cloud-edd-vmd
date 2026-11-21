package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;

import java.time.Instant;

/**
 * 设备→当前绑定车辆只读结果（VMD-DSN-CR-076）
 * <p>
 * 最小披露：仅返回 VIN 与绑定事实元数据，不含 sn、车主、账号、订单或客户 PII。
 * 无 active 绑定与数据冲突均以 200 + {@link VehicleBindingState} 表达，不使用 404 / 空对象。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartBindingByDeviceExResponse {

    /**
     * 绑定结论：BOUND / NO_ACTIVE_BINDING / BINDING_CONFLICT
     */
    private VehicleBindingState bindingState;

    /**
     * 车架号：仅 BOUND 时返回，其余状态为 null
     */
    private String vin;

    /**
     * 绑定事实聚合摘要：vehiclePartBinding:&lt;vin&gt;:&lt;绑定事实条数&gt;:&lt;最后更新时间&gt;
     * <p>仅 BOUND 时返回；不暴露自增主键与内部 seq 细节</p>
     */
    private String factRef;

    /**
     * 绑定事实最后更新时间（UTC 绝对时刻）：仅 BOUND 时返回
     */
    private Instant updatedAt;

}
