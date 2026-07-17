package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理后台零件导入数据响应
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartImportDataResponse {

    private Long id;
    private String batchNum;
    private String partCode;
    private String partName;
    private String vehicleNodeCode;
    private String vehicleNodeName;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private LocalDateTime createTime;
}
