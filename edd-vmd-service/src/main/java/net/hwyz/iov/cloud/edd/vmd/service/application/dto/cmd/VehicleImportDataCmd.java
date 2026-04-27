package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车辆导入数据 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImportDataCmd {

    private Long id;
    private String batchNum;
    private String type;
    private String data;
    private Boolean handle;
    private String errorMsg;
    private Instant handleTime;
    private String description;

}
