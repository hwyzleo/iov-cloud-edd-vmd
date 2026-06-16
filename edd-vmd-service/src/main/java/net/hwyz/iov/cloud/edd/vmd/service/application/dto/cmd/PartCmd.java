package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 零件 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartCmd {

    private Long id;
    private String code;
    private String name;
    private String nameLocal;
    private String partType;
    private Boolean isSoftware;
    private String status;
    private Boolean isAccuratelyTraced;
    private String vehicleNodeCode;
    private String supplierCode;
    private Boolean fotaUpgradeable;
    private String source;
    private String externalRefId;
    private Long externalVersion;
    private java.time.LocalDateTime lastSyncTime;

}
