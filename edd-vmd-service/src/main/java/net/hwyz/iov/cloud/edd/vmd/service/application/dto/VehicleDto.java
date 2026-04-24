package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆数据传输对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {

    private String vin;
    private String manufacturerCode;
    private String brandCode;
    private String platformCode;
    private String seriesCode;
    private String modelCode;
    private String baseModelCode;
    private String buildConfigCode;
    private Date eolTime;
    private String orderNum;

}
