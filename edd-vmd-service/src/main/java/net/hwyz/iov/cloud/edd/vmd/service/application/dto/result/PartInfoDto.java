package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

/**
 * 物理零件实例 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartInfoDto {

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
    private Instant firstSeenTime;
    private String partName;
    /**
     * 节点设备类别快照（CR-072）：只读，可空；节点已知但历史未补齐时为空（非 OTHER）
     */
    private String deviceCategory;
    private String description;
    private Date createTime;

}
