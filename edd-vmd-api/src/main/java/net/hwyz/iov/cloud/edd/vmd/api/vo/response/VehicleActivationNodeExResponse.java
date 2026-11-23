package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;

import java.time.Instant;

/**
 * 车辆首次激活事实只读结果（VMD-DSN-CR-077 §7.2）
 * <p>
 * 最小披露：仅返回激活事实元数据（参与决定首次激活的事件证据与时刻），
 * 不含车主、账号、订单或任何客户 PII。
 * <p>
 * 三态以 200 + {@link VehicleActivationState} 表达，不使用 404 / 空对象：
 * {@code RECORDED} 返回首次激活事实；{@code PENDING} 仅返回最近一条未生效事实的
 * {@code eventId / occurredAt / receivedAt} 且 {@code firstActivatedAt=null}；
 * {@code NOT_RECORDED} 全字段为 null（含 {@code vinResolved}，避免把「未建档」与「未激活」混淆）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleActivationNodeExResponse {

    /**
     * 三态结论：RECORDED / PENDING / NOT_RECORDED
     */
    private VehicleActivationState state;

    /**
     * 车架号（键与入参规范化一致）
     */
    private String vin;

    /**
     * 首次激活绝对时刻（UTC）：projection.occurred_at 最小值；非 RECORDED 时为 null
     */
    private Instant firstActivatedAt;

    /**
     * 决定首次激活的绑定操作 ID：非 RECORDED 时为 null
     */
    private String bindingOperationId;

    /**
     * 来源事实引用（不含 PII）：非 RECORDED 时为 null
     */
    private String sourceFactRef;

    /**
     * 决定投影的事件 ID；PENDING 时为最近一条未生效事实的事件 ID
     */
    private String eventId;

    /**
     * 事实发生绝对时刻（UTC）
     */
    private Instant occurredAt;

    /**
     * VMD 接收该事实的绝对时刻（UTC）
     */
    private Instant receivedAt;

    /**
     * VIN 是否已在 VMD 建档：仅 RECORDED 时有意义，NOT_RECORDED 时为 null
     */
    private Boolean vinResolved;

    /**
     * 首次激活事实聚合摘要：{@code vehicleActivation:<vin>:<eventId>:<firstActivatedAt>}
     * <p>仅 RECORDED 时返回；不暴露自增主键</p>
     */
    private String factRef;

}
