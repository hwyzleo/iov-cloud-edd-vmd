package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆节点模式值对象
 * <p>
 * 描述车辆节点的配置模式，包括是否需要安全常量预置等属性
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNodeSchema {

    /**
     * 车辆节点编码
     */
    private String vehicleNodeCode;

    /**
     * HSM唯一标识字段名
     */
    private String hsmUid;

    /**
     * 是否需要安全常量预置
     */
    private boolean needsSecurityConstantPreset;

    /**
     * 描述
     */
    private String description;
}
