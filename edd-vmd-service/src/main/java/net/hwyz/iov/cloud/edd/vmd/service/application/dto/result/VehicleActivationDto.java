package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;

import java.time.Instant;

/**
 * 首次激活事实查询结果（VMD-DSN-CR-077 §7.2）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleActivationDto {

    /**
     * 三态结论
     */
    private VehicleActivationState state;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 首次激活绝对时刻（UTC）
     */
    private Instant firstActivatedAt;

    /**
     * 决定首次激活的绑定操作 ID
     */
    private String bindingOperationId;

    /**
     * 来源事实引用
     */
    private String sourceFactRef;

    /**
     * 事件 ID
     */
    private String eventId;

    /**
     * 事实发生绝对时刻
     */
    private Instant occurredAt;

    /**
     * 接收绝对时刻
     */
    private Instant receivedAt;

    /**
     * VIN 是否已建档
     */
    private Boolean vinResolved;

    /**
     * 事实聚合摘要
     */
    private String factRef;

}
