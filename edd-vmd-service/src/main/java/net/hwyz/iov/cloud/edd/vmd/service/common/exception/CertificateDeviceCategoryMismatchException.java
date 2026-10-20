package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书设备类别不匹配异常
 * <p>
 * 设备类别与证书 Profile 允许的设备类别不一致时拒绝签发（VMD-DSN-CR-063 §4.1）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateDeviceCategoryMismatchException extends VmdBaseException {

    public CertificateDeviceCategoryMismatchException(String deviceCategory, String allowedCategory) {
        super(VmdErrorCode.CERTIFICATE_DEVICE_CATEGORY_MISMATCH,
                "设备类别=" + deviceCategory + " 与Profile允许类别=" + allowedCategory + " 不一致");
        log.warn("证书设备类别不匹配：deviceCategory=[{}] allowedCategory=[{}]", deviceCategory, allowedCategory);
    }

}
