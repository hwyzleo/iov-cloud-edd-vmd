package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA根下发响应
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaRootDeriveResponse {

    /**
     * KMS密钥引用
     */
    private String kmsKeyRef;

    /**
     * KCV密钥校验值（hex编码）
     */
    private String kcv;

    /**
     * 封装密文（Base64编码，一次性下发）
     */
    private String wrapped;
}
