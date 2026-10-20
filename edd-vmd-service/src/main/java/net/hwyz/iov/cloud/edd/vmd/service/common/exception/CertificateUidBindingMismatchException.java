package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发结果 UID 绑定不匹配异常
 * <p>
 * VMD-DSN-CR-063：签发后复验发现叶子证书 Subject CN 与权威 hsm_uid/ecu_uid 不一致，
 * 拒绝置 ACTIVE。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateUidBindingMismatchException extends VmdBaseException {

    public CertificateUidBindingMismatchException(String detail) {
        super(VmdErrorCode.CERTIFICATE_UID_BINDING_MISMATCH, detail);
        log.warn("证书签发结果UID绑定不匹配：{}", detail);
    }

}
