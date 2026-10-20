package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发结果公钥绑定不匹配异常
 * <p>
 * VMD-DSN-CR-063：签发后复验发现叶子证书 SPKI 与 CSR SPKI 不一致（PKI 未按请求公钥签发），
 * 拒绝置 ACTIVE。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificatePublicKeyBindingMismatchException extends VmdBaseException {

    public CertificatePublicKeyBindingMismatchException(String detail) {
        super(VmdErrorCode.CERTIFICATE_PUBLIC_KEY_BINDING_MISMATCH, detail);
        log.warn("证书签发结果公钥绑定不匹配：{}", detail);
    }

}
