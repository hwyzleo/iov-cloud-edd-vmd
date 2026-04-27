package net.hwyz.iov.cloud.edd.vmd.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆订单
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleOrderExRequest {

    /**
     * 车架号
     */
    private String vin;

    /**
     * 订单号
     */
    private String orderNum;

}
