package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    private Long id;
    private String vin;
    private String plantCode;
    private String brandCode;
    private String platformCode;
    private String carLineCode;
    private String modelCode;
    private String variantCode;
    private String configurationCode;
    private Instant eolTime;
    private String orderNum;

}
