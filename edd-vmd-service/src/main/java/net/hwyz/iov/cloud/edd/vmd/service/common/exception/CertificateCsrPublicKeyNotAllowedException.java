package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR公钥不允许异常
 * <p>
 * VMD-DSN-CR-063：逻辑 Profile TBOX_DEVICE_ECDH_ES_P256_V1 仅允许 EC P-256（secp256r1），
 * RSA / P-384 / P-521 等其他算法/曲线 fail-closed 拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateCsrPublicKeyNotAllowedException extends VmdBaseException {

    public CertificateCsrPublicKeyNotAllowedException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_PUBLIC_KEY_NOT_ALLOWED, detail);
        log.warn("证书CSR公钥不允许：{}", detail);
    }

}
