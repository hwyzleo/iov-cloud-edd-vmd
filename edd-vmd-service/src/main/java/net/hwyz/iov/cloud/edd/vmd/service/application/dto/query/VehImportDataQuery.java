package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车辆导入数据查询 DTO
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Data
@Builder
public class VehImportDataQuery {

    private String batchNum;
    private String type;
    private Boolean handle;
    private Date beginTime;
    private Date endTime;

}
