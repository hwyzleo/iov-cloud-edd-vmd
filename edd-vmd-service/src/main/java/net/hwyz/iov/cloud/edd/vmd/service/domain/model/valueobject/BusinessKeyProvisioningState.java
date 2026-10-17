package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import java.util.Arrays;

/**
 * 业务密钥开通批次状态值对象（CR-058 D35）
 * <p>
 * VMD 权威维护多用途 V2C 开通批次生命周期：
 * <ul>
 *   <li>PREPARING：批次已创建，用途版本创建/解析与设备证书 Wrap 进行中；根派生依赖不可用（806107）
 *       时可保持未终结，供车端以原 requestId 重试（不因可重试失败终态化）</li>
 *   <li>WAIT_DEVICE_ACK：两用途已下发，等待 TBOX 件内导入 ACK</li>
 *   <li>COMPLETED：完整 ACK 成功，目标版本已原子切换 ACTIVE（终态）</li>
 *   <li>FAILED：明确不可恢复失败（终态，不发成功事件）</li>
 *   <li>RECONCILE_REQUIRED：ACK/依赖结果未知，需以原 requestId 对账</li>
 *   <li>EXPIRED：超过策略时限且无有效确认（终态）</li>
 * </ul>
 * 未终结状态（{@link #isOpen()}）参与“同设备+业务域最多一个未终结初始批次”唯一约束。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public enum BusinessKeyProvisioningState {

    PREPARING("PREPARING", "开通准备中"),
    WAIT_DEVICE_ACK("WAIT_DEVICE_ACK", "等待设备导入确认"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "已失败"),
    RECONCILE_REQUIRED("RECONCILE_REQUIRED", "待对账"),
    EXPIRED("EXPIRED", "已过期");

    private final String value;
    private final String label;

    BusinessKeyProvisioningState(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 是否为未终结（开放）状态；用于生成列 open_flag 与并发唯一约束。
     */
    public boolean isOpen() {
        return this == PREPARING || this == WAIT_DEVICE_ACK || this == RECONCILE_REQUIRED;
    }

    /**
     * 是否允许继续以原 requestId 推进（含 ACK、对账）。
     */
    public boolean canAdvance() {
        return isOpen();
    }

    public static BusinessKeyProvisioningState valOf(String val) {
        return Arrays.stream(values())
                .filter(state -> state.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
