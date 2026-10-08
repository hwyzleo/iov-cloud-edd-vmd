package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 业务密钥非敏感元数据（CR-055 §7.2 getBusinessKeyMetadata）
 * <p>
 * 含内部 kmsKeyRef，仅限受信服务间契约，不对外部客户端暴露。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyMetadataResult {

    private String keyId;
    private Long businessKeyVersion;
    private String kmsKeyRef;
    private Integer kmsKeyVersion;
    private String kmsProvider;
    private String algorithm;
    private String keySpec;
    private String state;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime decryptUntil;
    private String deviceSn;
    private String businessDomain;
    private String purpose;
}
