package net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VAGW 已认证 MQTT 会话派生的可信设备上下文（VMD-DSN-CR-059 §4）。
 * <p>
 * 由 VAGW 从 EMQX 已认证会话生成，与 canonical payload 分离传输；VMD 不以 payload
 * 自报身份作为授权依据，而是将本上下文与当前 active 绑定、设备状态和证书状态交叉校验。
 * <p>
 * 本对象不含 Wrapped Key、parameters、importProof 等敏感材料，可安全序列化与审计。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VagwAuthenticatedDeviceContext {

    /**
     * 设备序列号（TBOX SN）；仅作交叉校验提示，不作为授权凭证
     */
    private String deviceSn;

    /**
     * 归一化 HSM UID；与 ecuUid 至少提供一个，作为 VMD 定位 active 绑定的权威线索
     */
    private String hsmUid;

    /**
     * ECU UID；与 hsmUid 至少提供一个
     */
    private String ecuUid;

    /**
     * broker 侧会话标识（审计与旧会话结果防误用）
     */
    private String brokerSessionId;

    /**
     * 连接代际（重连/session 替换递增）
     */
    private long connectionGeneration;

    /**
     * 设备证书序列号（认证期透传，可空）
     */
    private String certificateSerial;

    /**
     * 设备证书指纹（认证期透传，可空）
     */
    private String certificateFingerprint;

    /**
     * broker 接收时间（UTC epoch ms）
     */
    private long mqttReceivedAt;

    /**
     * 是否携带可用于定位绑定的设备身份（hsmUid 或 ecuUid）
     */
    public boolean hasDeviceIdentity() {
        return (hsmUid != null && !hsmUid.isBlank()) || (ecuUid != null && !ecuUid.isBlank());
    }
}
