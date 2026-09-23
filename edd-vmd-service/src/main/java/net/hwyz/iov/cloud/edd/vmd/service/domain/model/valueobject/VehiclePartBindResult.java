package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

/**
 * 车辆零件幂等绑定结果枚举
 * <p>
 * 对齐 VMD-DSN-CR-029 F9 时序与 §3.3 幂等约束：
 * - BOUND：新建 active 绑定成功
 * - SKIPPED_IDEMPOTENT：幂等命中（零件已绑定同一车辆/同一装车槽位），视为成功跳过
 *
 * @author hwyz_leo
 */
public enum VehiclePartBindResult {

    /**
     * 新建绑定成功
     */
    BOUND,

    /**
     * 幂等命中，跳过
     */
    SKIPPED_IDEMPOTENT

}
