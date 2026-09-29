package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书CSR主体不匹配异常
 * <p>
 * CR-054：CSR Subject CN 缺失、重复、格式非法，或与请求 ecu_uid / 绑定 hsm_uid 不一致时拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateCsrSubjectMismatchException extends VmdBaseException {

    public CertificateCsrSubjectMismatchException(String detail) {
        super(VmdErrorCode.CERTIFICATE_CSR_SUBJECT_MISMATCH, detail);
        log.warn("证书CSR主体不匹配：{}", detail);
    }

}
