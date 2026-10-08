package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * 业务密钥授权策略（CR-055 §4）
 * <p>
 * 统一校验调用方/设备类别/业务域/用途/操作；未登记或越权一律 fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public interface BusinessKeyAuthorizationPolicy {

    /**
     * 校验操作授权并返回策略条目
     *
     * @param action         操作动作
     * @param deviceCategory 设备类别
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return 策略条目（含算法/规格/有效期等密码学参数）
     * @throws net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException 域/用途未登记或越权
     */
    BusinessKeyPolicyEntry authorize(BusinessKeyAction action, String deviceCategory, String businessDomain, String purpose);

    /**
     * 解析域默认用途（framework 运行时 purpose 为空时）
     *
     * @param businessDomain 业务域
     * @return 默认用途
     * @throws net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException 域未登记默认用途
     */
    String defaultPurpose(String businessDomain);
}
