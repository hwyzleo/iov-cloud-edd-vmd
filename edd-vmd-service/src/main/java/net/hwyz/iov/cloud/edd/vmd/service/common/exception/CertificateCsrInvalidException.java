package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR非法异常
 * <p>
 * CR-054：CSR 格式/ASN.1 畸形或 PKCS#10 自签名（PoP）校验失败，fail-closed 拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateCsrInvalidException extends VmdBaseException {

    public CertificateCsrInvalidException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_INVALID, detail);
        log.warn("证书CSR非法：{}", detail);
    }

}
