package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发结果 Profile 不匹配异常
 * <p>
 * VMD-DSN-CR-063：签发后复验经 FW-SEC TboxDeviceCertificateValidator 校验叶子证书不满足
 * 逻辑 Profile（链/有效期/吊销/曲线/KU/EKU/critical 扩展），拒绝置 ACTIVE。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateIssuedProfileMismatchException extends VmdBaseException {

    public CertificateIssuedProfileMismatchException(String detail) {
        super(VmdErrorCode.CERTIFICATE_ISSUED_PROFILE_MISMATCH, detail);
        log.warn("证书签发结果Profile复验不匹配：{}", detail);
    }

}
