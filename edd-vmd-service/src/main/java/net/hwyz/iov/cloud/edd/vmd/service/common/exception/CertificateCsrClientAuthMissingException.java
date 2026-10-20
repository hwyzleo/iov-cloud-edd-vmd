package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR clientAuth 缺失异常
 * <p>
 * VMD-DSN-CR-063：逻辑 Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 ExtendedKeyUsage 至少含
 * clientAuth；缺失时返回稳定 reason 拒绝签发（与 EKU 过宽区分）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateCsrClientAuthMissingException extends VmdBaseException {

    public CertificateCsrClientAuthMissingException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_CLIENT_AUTH_MISSING, detail);
        log.warn("证书CSR clientAuth缺失：{}", detail);
    }

}
