package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备→绑定事实聚合查询结果（VMD-DSN-CR-076）
 * <p>
 * 非表实体：仅承载 `tb_vehicle_part JOIN tb_part_info` 按 VIN 分组的聚合投影，
 * 供 Repository 转换为领域值对象 {@code DeviceBindingFact}。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartBindingFactPo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 该 VIN 命中的 active 绑定事实条数
     */
    private Long bindingCount;

    /**
     * 该 VIN 命中绑定事实的最后修改时间（DB 侧 UTC 存储）
     */
    private Date updatedAt;

}
