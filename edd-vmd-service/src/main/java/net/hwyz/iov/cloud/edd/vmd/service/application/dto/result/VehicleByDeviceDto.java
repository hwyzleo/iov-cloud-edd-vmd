package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;

import java.time.Instant;

/**
 * 设备→当前绑定车辆查询结果（VMD-DSN-CR-076）
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleByDeviceDto {

    /**
     * 绑定结论：BOUND / NO_ACTIVE_BINDING / BINDING_CONFLICT
     */
    private VehicleBindingState bindingState;

    /**
     * 车架号：仅 BOUND 时非空
     */
    private String vin;

    /**
     * 绑定事实聚合摘要：仅 BOUND 时非空
     */
    private String factRef;

    /**
     * 绑定事实最后更新时间：仅 BOUND 时非空
     */
    private Instant updatedAt;

}
