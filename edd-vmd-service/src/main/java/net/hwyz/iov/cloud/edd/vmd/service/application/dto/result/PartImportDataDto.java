package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 零件导入数据 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartImportDataDto {

    private Long id;
    private String batchNum;
    private String partCode;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private LocalDateTime createTime;

}
