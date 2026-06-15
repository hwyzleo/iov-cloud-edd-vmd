package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 零件导入数据领域实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartImportData extends BaseDo<Long> {

    private Long id;
    private String batchNum;
    private String partCode;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private LocalDateTime createTime;
}
