package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleQuery {

    /**
     * 车架号
     */
    private String vin;

    /**
     * 生产配置代码
     */
    private String buildConfigCode;

    /**
     * 开始时间
     */
    private Date beginTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 是否下线
     */
    private Boolean isEol;

    /**
     * 是否有订单
     */
    private Boolean isOrder;

}
