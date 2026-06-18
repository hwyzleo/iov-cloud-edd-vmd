package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆零件查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehiclePartQuery {

    private String vin;
    private Long partId;
    private String vehicleNodeCode;
    private Integer bindState;
    private Date beginTime;
    private Date endTime;

}
