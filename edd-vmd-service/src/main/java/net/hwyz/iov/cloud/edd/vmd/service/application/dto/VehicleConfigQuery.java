package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆配置查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleConfigQuery {

    private String vin;
    private String version;
    private Date beginTime;
    private Date endTime;

}
