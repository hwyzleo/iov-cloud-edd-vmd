package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 物理零件实例 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartInfoCmd {

    private Long id;
    private String partCode;
    private String sn;
    private String vehicleNodeCode;
    private String configWord;
    private String supplierCode;
    private String batchNum;
    private String hardwareVer;
    private String softwareVer;
    private String hardwarePn;
    private String softwarePn;
    private String extra;
    private Integer instanceState;
    private String description;

}
