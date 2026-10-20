package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR 未知 critical 扩展异常
 * <p>
 * VMD-DSN-CR-063：逻辑 Profile TBOX_DEVICE_ECDH_ES_P256_V1 采用 OID + criticality +
 * semantic validator allowlist 校验扩展；未知 critical 扩展或语义不允许（如 basicConstraints
 * CA=true）fail-closed 拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateCsrUnsupportedCriticalExtensionException extends VmdBaseException {

    public CertificateCsrUnsupportedCriticalExtensionException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_UNSUPPORTED_CRITICAL_EXTENSION, detail);
        log.warn("证书CSR含未知critical扩展或语义不允许扩展：{}", detail);
    }

}
