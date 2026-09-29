package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书设备HSM UID不可用异常
 * <p>
 * CR-054：active TBOX 绑定的权威 hsm_uid 缺失，或多来源（part_info.extra.HSM / part_security_constant.chip_uid）
 * 规范化后不一致时 fail-closed，不得退回 device_sn 或任选其一继续签发。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateDeviceUidUnavailableException extends VmdBaseException {

    public CertificateDeviceUidUnavailableException(String deviceSn, String detail) {
        super(VmdErrorCode.CERTIFICATE_DEVICE_UID_UNAVAILABLE, detail);
        log.warn("设备[{}]HSM UID不可用：{}", deviceSn, detail);
    }

}
