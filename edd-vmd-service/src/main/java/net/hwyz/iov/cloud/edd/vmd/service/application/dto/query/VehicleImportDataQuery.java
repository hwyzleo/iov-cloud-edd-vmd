package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆导入数据查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleImportDataQuery {

    private String batchNum;
    private String type;
    private Boolean handle;
    private Date beginTime;
    private Date endTime;

}
