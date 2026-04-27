package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车辆零件 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartCmd {

    private Long id;
    private String vin;
    private String pn;
    private String sn;
    private String hardwareVersion;
    private String softwareVersion;
    private String supplierCode;
    private Integer partState;
    private Instant bindTime;
    private Instant unbindTime;
    private String description;

}
