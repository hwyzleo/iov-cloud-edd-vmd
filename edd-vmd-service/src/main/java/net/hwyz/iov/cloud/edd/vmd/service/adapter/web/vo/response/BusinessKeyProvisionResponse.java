package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 业务密钥在线申请响应（CR-055 §7.1 down/keyprov 载荷）
 * <p>
 * Wrapped Key 仅本次响应返回，不落库、不进日志/事件。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisionResponse {

    private String requestId;
    private String keyId;
    private Long businessKeyVersion;
    private String wrappedKeyBase64;
    private String algorithm;
    private String keySpec;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Map<String, String> parameters;
    private String state;
    private boolean reused;
}
