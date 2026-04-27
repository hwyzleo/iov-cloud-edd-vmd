package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆生命周期查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleLifecycleQuery {

    private String vin;
    private String nodeCode;
    private Date beginTime;
    private Date endTime;

}
