package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR ExtendedKeyUsage 冲突异常
 * <p>
 * VMD-DSN-CR-063：逻辑 Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 ExtendedKeyUsage 仅含
 * clientAuth；anyExtendedKeyUsage / serverAuth / 其他用途视为 EKU 过宽，拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateCsrEkuConflictException extends VmdBaseException {

    public CertificateCsrEkuConflictException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_EKU_CONFLICT, detail);
        log.warn("证书CSR ExtendedKeyUsage冲突：{}", detail);
    }

}
