package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆零部件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartExService {

    /**
     * 车架号
     */
    private String vin;

    /**
     * 序列号
     */
    private String sn;

}
