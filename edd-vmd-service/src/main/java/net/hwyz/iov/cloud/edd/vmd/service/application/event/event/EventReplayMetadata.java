package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 事件补发元数据
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * <p>
 * 携带补发相关的元数据信息，用于审计和幂等控制
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReplayMetadata {

    /**
     * 是否为补发事件
     */
    private Boolean replay;

    /**
     * 原批次号
     */
    private String originalBatchNum;

    /**
     * 补发请求ID（幂等键）
     */
    private String replayId;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 补发时间
     */
    private LocalDateTime replayedAt;
}
