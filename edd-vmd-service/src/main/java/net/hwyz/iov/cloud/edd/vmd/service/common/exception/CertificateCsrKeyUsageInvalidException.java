package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR KeyUsage 非法异常
 * <p>
 * VMD-DSN-CR-063：逻辑 Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 KeyUsage 恰好为
 * digitalSignature + keyAgreement；缺失任一或携带 keyCertSign/CA 意图等冲突位时拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateCsrKeyUsageInvalidException extends VmdBaseException {

    public CertificateCsrKeyUsageInvalidException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_KEY_USAGE_INVALID, detail);
        log.warn("证书CSR KeyUsage非法：{}", detail);
    }

}
