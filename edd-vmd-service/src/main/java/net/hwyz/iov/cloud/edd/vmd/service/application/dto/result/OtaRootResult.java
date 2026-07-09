package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA根下发结果
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaRootResult {

    /**
     * KMS密钥引用
     */
    private String kmsKeyRef;

    /**
     * KCV密钥校验值（hex编码）
     */
    private String kcv;

    /**
     * 封装密文（一次性下发，不落库）
     */
    private byte[] wrapped;
}
