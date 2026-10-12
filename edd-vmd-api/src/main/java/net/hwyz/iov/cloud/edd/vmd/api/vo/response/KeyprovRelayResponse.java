package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * keyprov canonical relay 响应（CR-058 / EDD-VAGW-DSN-CR-011 §3.5）。
 * <p>
 * 只承载 canonical 下行 payload 类型/bytes 与非敏感路由结果；不返回 KMS 内部 keyRef。
 * {@code downlinkPayload} 为敏感密文，不得写入日志/持久化/异常正文。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyprovRelayResponse {

    /**
     * 原上行 requestId（幂等/关联）
     */
    private String requestId;

    /**
     * canonical 下行 PayloadType（仅允许 security.keyprov.v1 三类）
     */
    private String downlinkPayloadType;

    /**
     * canonical 下行 payload bytes（敏感密文）
     */
    private byte[] downlinkPayload;

    /**
     * 下行 TTL（VMD 提供时）
     */
    private Long expireAtMs;

    /**
     * VMD 返回的目标 hsmUid（仅用于与认证上下文交叉校验）
     */
    private String routingHsmUid;

    /**
     * 传输/路由层结果类别：SUCCESS / REJECTED / UNAVAILABLE / OUTCOME_UNKNOWN
     */
    private String outcome;

    /**
     * 非敏感稳定 reason（审计）
     */
    private String reasonCode;

    public boolean hasPayload() {
        return downlinkPayloadType != null && !downlinkPayloadType.isBlank()
                && downlinkPayload != null;
    }
}
