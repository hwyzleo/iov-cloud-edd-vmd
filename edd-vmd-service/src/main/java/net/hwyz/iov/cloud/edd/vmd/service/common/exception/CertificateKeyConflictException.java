package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书换钥冲突异常
 * <p>
 * CR-054：同 vin + hsm_uid + certificate_profile 已存在不同公钥（SPKI）的有效/处理中申请时，
 * 普通申请/补偿拒绝，进入独立 reissue/rekey 授权，不得返回旧证书。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateKeyConflictException extends VmdBaseException {

    public CertificateKeyConflictException(String vin, String hsmUid, String certificateProfile) {
        super(VmdErrorCode.CERTIFICATE_KEY_CONFLICT);
        log.warn("证书换钥冲突：vin=[{}] hsmUid=[{}] profile=[{}] 已绑定其他公钥，需授权换钥",
                vin, hsmUid, certificateProfile);
    }

}
