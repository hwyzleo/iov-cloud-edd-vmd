package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * 零件导入数据查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class PartImportDataQuery {

    private String batchNum;
    private String partCode;
    private Boolean handle;

}
