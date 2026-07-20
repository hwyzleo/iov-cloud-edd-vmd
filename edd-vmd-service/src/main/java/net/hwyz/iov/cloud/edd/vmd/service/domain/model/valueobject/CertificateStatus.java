package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 证书状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum CertificateStatus {

    /** 已请求 **/
    REQUESTED,
    /** 签发中 **/
    ISSUING,
    /** 待对账 **/
    PENDING_RECONCILE,
    /** 已签发未确认 **/
    ISSUED_NOT_CONFIRMED,
    /** 已激活 **/
    ACTIVE,
    /** 安装失败 **/
    INSTALL_FAILED,
    /** 已取代 **/
    SUPERSEDED,
    /** 已吊销 **/
    REVOKED,
    /** 已过期 **/
    EXPIRED,
    /** 失败 **/
    FAILED;

    public static CertificateStatus valOf(String val) {
        return Arrays.stream(CertificateStatus.values())
                .filter(status -> status.name().equals(val))
                .findFirst()
                .orElse(null);
    }
}
