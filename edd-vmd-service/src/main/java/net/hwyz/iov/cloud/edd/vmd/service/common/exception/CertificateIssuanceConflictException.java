package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发冲突异常
 * <p>
 * CR-053：同一 device_sn + certificate_profile 已存在 ACTIVE 或处理中申请，
 * 且 CSR / 请求摘要不一致，禁止重复签发；相同业务键且摘要一致时应幂等返回既有申请。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CertificateIssuanceConflictException extends VmdBaseException {

    public CertificateIssuanceConflictException(String deviceSn, String certificateProfile) {
        super(VmdErrorCode.CERTIFICATE_ISSUANCE_CONFLICT);
        log.warn("证书签发冲突：设备[{}]Profile[{}]已存在有效或处理中申请", deviceSn, certificateProfile);
    }

}
