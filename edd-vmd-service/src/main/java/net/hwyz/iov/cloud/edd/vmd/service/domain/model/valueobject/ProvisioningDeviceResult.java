package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import java.util.Arrays;

/**
 * 开通批次逐用途设备导入结果值对象（CR-058 D35）
 * <p>
 * 与 canonical {@code security.keyprov.v1.KeyImportResult} 对齐（脱敏后落库）：
 * PENDING 为批次内初始占位；SUCCEEDED/FAILED/UNKNOWN 为设备 ACK 提交结果。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public enum ProvisioningDeviceResult {

    PENDING("PENDING", "待设备确认"),
    SUCCEEDED("SUCCEEDED", "导入成功"),
    FAILED("FAILED", "导入失败"),
    UNKNOWN("UNKNOWN", "导入结果未知");

    private final String value;
    private final String label;

    ProvisioningDeviceResult(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static ProvisioningDeviceResult valOf(String val) {
        return Arrays.stream(values())
                .filter(result -> result.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
