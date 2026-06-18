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
    private Long partId;
    private String vehicleNodeCode;
    private String deviceItem;
    private String position;
    private Instant bindTime;
    private String bindType;
    private String bindBy;
    private String bindOrg;
    private Instant unbindTime;
    private String unbindReason;
    private String unbindBy;
    private String unbindOrg;
    private Integer bindState;
    private Long replaceOfBindingId;
    private String description;

}
