package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆导入数据 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImportDataDto {

    private Long id;
    private String batchNum;
    private String type;
    private String version;
    private String data;
    private Boolean handle;
    private String errorMsg;
    private String description;
    private Date createTime;

}
