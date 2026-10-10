package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private String partName;
    private String vehicleNodeCode;
    private String vehicleNodeName;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private LocalDateTime createTime;

    /**
     * 是否允许执行「重放后置处理」（US-062：记录存在、候选可识别且无执行中重放任务）
     */
    private Boolean postProcessReplayable;

    /**
     * 不可重放原因（postProcessReplayable=false 时）
     */
    private String postProcessReplayReason;

    /**
     * 可执行的动作预览（供前端二次确认）
     */
    private List<PostProcessActionPreview> postProcessActionPreview;
}
