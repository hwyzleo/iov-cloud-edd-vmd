package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 车辆导入事件补发主任务状态
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展
 * <p>
 * 沿用 PENDING/RUNNING/QUEUED/PARTIAL_FAILED/FAILED（PRODUCE 纯事件场景保持 QUEUED 向后兼容），
 * 新增 SUCCESS（无事件动作、仅生命周期补齐等成功动作）与 SUCCEEDED_WITH_SKIPS（成功 + 跳过，无失败）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum VehicleImportReplayMasterStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    QUEUED("QUEUED", "已入队"),
    SUCCESS("SUCCESS", "成功"),
    SUCCEEDED_WITH_SKIPS("SUCCEEDED_WITH_SKIPS", "成功但有跳过"),
    PARTIAL_FAILED("PARTIAL_FAILED", "部分失败"),
    FAILED("FAILED", "失败");

    private final String value;
    private final String label;

    public static VehicleImportReplayMasterStatus valOf(String val) {
        return Arrays.stream(VehicleImportReplayMasterStatus.values())
                .filter(status -> status.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
