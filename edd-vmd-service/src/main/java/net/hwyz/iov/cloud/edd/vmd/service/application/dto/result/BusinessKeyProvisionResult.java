package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 业务密钥在线申请结果（CR-055 §7.1 down/keyprov 载荷）
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
public class BusinessKeyProvisionResult {

    /**
     * 调用幂等键
     */
    private String requestId;

    /**
     * framework/KMS 返回的不透明标识
     */
    private String keyId;

    /**
     * VMD 业务版本
     */
    private Long businessKeyVersion;

    /**
     * 设备证书公钥 Wrap 后的密钥密文（Base64，一次性下发，不落库）
     */
    private String wrappedKeyBase64;

    /**
     * 算法
     */
    private String algorithm;

    /**
     * 密钥规格
     */
    private String keySpec;

    /**
     * 有效窗口开始
     */
    private LocalDateTime validFrom;

    /**
     * 有效窗口结束
     */
    private LocalDateTime validTo;

    /**
     * framework Wrap 返回的参数（如 IV/盐等，仅本次下发）
     */
    private Map<String, String> parameters;

    /**
     * 业务密钥状态（ACTIVE）
     */
    private String state;

    /**
     * 是否幂等复用既有 ACTIVE（true 表示未新建材料）
     */
    private boolean reused;
}
