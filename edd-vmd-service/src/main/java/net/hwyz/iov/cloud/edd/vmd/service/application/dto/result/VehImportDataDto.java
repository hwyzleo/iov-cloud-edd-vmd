package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
public class VehImportDataDto {

    private Long id;
    private String batchNum;
    private String type;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private LocalDateTime createTime;

    /**
     * 是否允许补发事件
     * <p>
     * VMD-DSN-CR-039: 车辆导入成功事件人工补发
     */
    private Boolean eventReplayable;

    /**
     * 不允许补发的原因
     * <p>
     * VMD-DSN-CR-039: 车辆导入成功事件人工补发
     */
    private String eventReplayReason;

    /**
     * 允许补发的动作类型列表（PRODUCE→PRODUCE_EVENT；TOL/EOL→生命周期+绑定+软件实装）
     * <p>
     * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
     */
    private java.util.List<String> replayActionTypes;

}
