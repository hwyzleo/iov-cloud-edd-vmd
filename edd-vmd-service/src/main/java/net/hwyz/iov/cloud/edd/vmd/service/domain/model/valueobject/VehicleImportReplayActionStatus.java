package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 车辆导入事件补发逐项动作状态
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum VehicleImportReplayActionStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    QUEUED("QUEUED", "事件已入Outbox"),
    SUCCESS("SUCCESS", "成功"),
    SKIPPED("SKIPPED", "已跳过"),
    FAILED_RETRYABLE("FAILED_RETRYABLE", "可重试失败"),
    FAILED_FINAL("FAILED_FINAL", "终态失败");

    private final String value;
    private final String label;

    public static VehicleImportReplayActionStatus valOf(String val) {
        return Arrays.stream(VehicleImportReplayActionStatus.values())
                .filter(status -> status.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
