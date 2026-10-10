package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件导入后置处理重放主任务状态
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum PartPostProcessReplayStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    PARTIAL_SUCCESS("PARTIAL_SUCCESS", "部分成功"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String value;
    private final String label;

    public static PartPostProcessReplayStatus valOf(String val) {
        return Arrays.stream(PartPostProcessReplayStatus.values())
                .filter(status -> status.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
