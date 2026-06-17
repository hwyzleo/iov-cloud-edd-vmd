package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车辆导入数据 DTO
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehImportDataCmd {

    private Long id;
    private String batchNum;
    private String type;
    private String version;
    private String data;
    private String description;

}
