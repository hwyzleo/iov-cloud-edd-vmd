package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException;
import org.springframework.stereotype.Service;

/**
 * 默认业务密钥授权策略实现（CR-055 §3.3）
 * <p>
 * 基于版本化 {@link BusinessKeyPolicyRegistry} 统一校验；未登记/设备类别越权/操作越权一律拒绝。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultBusinessKeyAuthorizationPolicy implements BusinessKeyAuthorizationPolicy {

    private final BusinessKeyPolicyRegistry policyRegistry;

    @Override
    public BusinessKeyPolicyEntry authorize(BusinessKeyAction action, String deviceCategory, String businessDomain, String purpose) {
        BusinessKeyPolicyEntry entry = policyRegistry.resolve(businessDomain, purpose)
                .orElseThrow(() -> new BusinessKeyDomainNotAuthorizedException(
                        "业务域或用途未登记: businessDomain=" + businessDomain + ", purpose=" + purpose));

        if (!entry.allowsDeviceCategory(deviceCategory)) {
            log.warn("业务密钥授权拒绝：设备类别越权 deviceCategory={}, domain={}, purpose={}, action={}",
                    deviceCategory, businessDomain, purpose, action);
            throw new BusinessKeyDomainNotAuthorizedException(
                    "设备类别不允许: deviceCategory=" + deviceCategory + ", domain=" + businessDomain + ", purpose=" + purpose);
        }
        if (!entry.allowsAction(action)) {
            log.warn("业务密钥授权拒绝：操作越权 action={}, domain={}, purpose={}", action, businessDomain, purpose);
            throw new BusinessKeyDomainNotAuthorizedException(
                    "操作不允许: action=" + action + ", domain=" + businessDomain + ", purpose=" + purpose);
        }
        return entry;
    }

    @Override
    public String defaultPurpose(String businessDomain) {
        return policyRegistry.defaultPurpose(businessDomain)
                .orElseThrow(() -> new BusinessKeyDomainNotAuthorizedException(
                        "业务域未配置默认用途: businessDomain=" + businessDomain));
    }
}
