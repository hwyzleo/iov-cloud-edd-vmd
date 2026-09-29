package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR禁止项异常
 * <p>
 * CR-054：CSR Subject / SAN 不得携带 VIN 或 device_sn，命中即拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateCsrContainsForbiddenValueException extends VmdBaseException {

    public CertificateCsrContainsForbiddenValueException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_CONTAINS_VIN, detail);
        log.warn("证书CSR包含禁止标识：{}", detail);
    }

}
