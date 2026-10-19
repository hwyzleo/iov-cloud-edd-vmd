package net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 官方 Keyprov Service 开通申请请求（VMD-DSN-CR-059 §3/§4）。
 * <p>
 * wire 承载可信设备上下文与 canonical payload bytes；{@code payloadBytes} 为
 * {@code security.keyprov.v1.KeyProvisioningRequest} 的 canonical 序列化结果，
 * 属敏感材料，禁止 toString/日志/持久化。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "payloadBytes")
public class VmdKeyProvisioningRequest {

    /**
     * VAGW 已认证会话派生的可信设备上下文
     */
    private VagwAuthenticatedDeviceContext deviceContext;

    /**
     * canonical payload 完整类型名，必须为 security.keyprov.v1.KeyProvisioningRequest
     */
    private String payloadType;

    /**
     * canonical 协议版本（keyprov-v1）
     */
    private String schemaVersion;

    /**
     * 调用方声明的 PAR-PROTO release（受 VMD 固定值精确 allowlist 门禁；VMD-DSN-CR-062 §4.1）
     */
    private String protocolRelease;

    /**
     * 调用方声明的 PAR-PROTO DescriptorSet hash（受 VMD 固定值精确 allowlist 门禁；VMD-DSN-CR-062 §4.1）
     */
    private String descriptorSetHash;

    /**
     * canonical payload bytes（敏感，禁日志）
     */
    private byte[] payloadBytes;

    /**
     * payload 摘要（sha-256 hex，用于完整性校验与幂等关联）
     */
    private String payloadDigest;
}
