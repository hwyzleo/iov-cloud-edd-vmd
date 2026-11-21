package net.hwyz.iov.cloud.edd.vmd.api.vo.enums;

import java.util.Arrays;

/**
 * 设备→当前绑定车辆结论（VMD-DSN-CR-076 / D-VMD-76-03）
 * <p>
 * 三态显式区分，避免调用方以 404、空对象或“最新一条”猜测绑定关系：
 * 无 active 绑定返回 {@link #NO_ACTIVE_BINDING}；同一设备存在多个不同 VIN 的 active 绑定
 * 返回 {@link #BINDING_CONFLICT}（不返回任一 VIN，由调用方转人工核验）。
 * </p>
 *
 * @author hwyz_leo
 */
public enum VehicleBindingState {

    /**
     * 唯一 active 绑定：返回 VIN
     */
    BOUND,

    /**
     * 无 active 绑定（仅有失效历史记录同样计入本状态）
     */
    NO_ACTIVE_BINDING,

    /**
     * 数据冲突：同一设备存在多个不同 VIN 的 active 绑定，不返回 VIN
     */
    BINDING_CONFLICT;

    /**
     * 宽松反序列化：未知值返回 null，不抛出异常
     *
     * @param val 字符串值
     * @return 枚举值或 null
     */
    public static VehicleBindingState valOf(String val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(state -> state.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
