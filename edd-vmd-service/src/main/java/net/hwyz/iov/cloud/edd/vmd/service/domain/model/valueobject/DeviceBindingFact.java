package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 设备→绑定事实聚合摘要（VMD-DSN-CR-076 / D-VMD-76-04）
 * <p>
 * 只读查询的聚合结果：按 VIN 分组的 active 绑定事实条数与最后更新时间。
 * 不承载自增主键与内部 seq，供 factRef 摘要与三态结论使用。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
@Getter
@Builder
@AllArgsConstructor
public class DeviceBindingFact {

    /**
     * 车架号
     */
    private final String vin;

    /**
     * 该 VIN 命中的 active 绑定事实条数
     */
    private final long bindingCount;

    /**
     * 该 VIN 命中绑定事实的最后更新时间（UTC 绝对时刻）
     */
    private final Instant updatedAt;

}
