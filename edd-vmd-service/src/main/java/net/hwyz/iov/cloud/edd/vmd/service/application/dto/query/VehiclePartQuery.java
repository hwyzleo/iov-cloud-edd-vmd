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
    private String pn;
    private String sn;
    private Integer partState;
    private Date beginTime;
    private Date endTime;

}
