package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆配置项 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleConfigItemDto {

    private Long id;
    private String vin;
    private String version;
    private String configItemCode;
    private String configItemValue;
    private String configItemOptionCode;

}
