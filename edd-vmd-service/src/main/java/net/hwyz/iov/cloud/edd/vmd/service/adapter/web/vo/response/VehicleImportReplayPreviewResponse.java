package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车辆导入补发预检响应
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImportReplayPreviewResponse {

    /**
     * 导入类型（PRODUCE/TOL/EOL）
     */
    private String importType;

    /**
     * 原批次号
     */
    private String batchNum;

    /**
     * 候选车辆数（去重VIN）
     */
    private int candidateVinCount;

    /**
     * 可执行动作列表
     */
    private List<ReplayActionPreviewResponse> actions;
}
