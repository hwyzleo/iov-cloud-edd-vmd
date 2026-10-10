package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车辆导入补发预检结果
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展
 * <p>
 * 返回导入类型、候选车辆数及各动作的可执行/跳过统计，供前端选择动作范围。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImportReplayPreview {

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
    private List<ReplayActionPreview> actions;
}
