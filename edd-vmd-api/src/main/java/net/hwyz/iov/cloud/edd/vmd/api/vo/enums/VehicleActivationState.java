package net.hwyz.iov.cloud.edd.vmd.api.vo.enums;

import java.util.Arrays;

/**
 * 车辆首次激活事实三态结论（VMD-DSN-CR-077 / D-VMD-077-09）
 * <p>
 * 显式区分三种「查询成功」结论，避免调用方以 404、空对象或 null 猜测是否已激活：
 * <ul>
 *   <li>{@link #RECORDED}：投影已存在（可能来自向前纠正），返回首次激活事实</li>
 *   <li>{@link #PENDING}：台账已接收事实但未作用于投影（QUARANTINED / FAILED 且 applied=0），
 *       显式 {@code firstActivatedAt=null}，调用方不得据此推断「已激活」</li>
 *   <li>{@link #NOT_RECORDED}：无任何事实（未收到事件或收到前即上线）</li>
 * </ul>
 * 任何技术失败（存储不可用 / 超时 / 未知异常）<strong>不得</strong>降级为 {@link #NOT_RECORDED}，
 * 必须 fail-closed 抛 806137（ACTIVATION_FACT_QUERY_UNAVAILABLE）。
 *
 * @author hwyz_leo
 */
public enum VehicleActivationState {

    /**
     * 已记录：投影存在
     */
    RECORDED,

    /**
     * 已接收未生效：台账存在且未作用于投影（隔离 / 失败）
     */
    PENDING,

    /**
     * 无任何事实
     */
    NOT_RECORDED;

    /**
     * 宽松反序列化：未知值返回 null，不抛出异常
     *
     * @param val 字符串值
     * @return 枚举值或 null
     */
    public static VehicleActivationState valOf(String val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(state -> state.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
