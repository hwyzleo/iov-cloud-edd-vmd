package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyMappingProperties;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.DeviceKeyContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * framework 运行时目录上下文 → VMD 目录查询上下文 归一化（CR-055 §7.2）
 * <p>
 * framework 运行时（DefaultCryptoTemplate.encrypt/decrypt）以
 * {@code DeviceKeyContext(deviceSn, bizType, purpose[可空], caller[可空])} 调用 VMD 目录；
 * VMD 目录以 (device_sn, business_domain, purpose) 为键。本组件完成归一化：
 * <ol>
 *   <li>business_domain：按 {@link BusinessKeyMappingProperties} 映射 BizType 名，缺省取 BizType 名；</li>
 *   <li>purpose：上下文携带则用之，否则取该域策略登记的默认用途；无默认用途 fail-closed。</li>
 * </ol>
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessKeyBizTypeMapper {

    private final BusinessKeyMappingProperties mappingProperties;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;

    /**
     * 归一化目录查询上下文
     *
     * @param context framework DeviceKeyContext
     * @return VMD 目录查询键
     * @throws net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException 域无默认用途
     */
    public BusinessKeyDirectoryQuery toDirectoryQuery(DeviceKeyContext context) {
        if (context == null || StrUtil.isBlank(context.deviceSn()) || context.bizType() == null) {
            throw new IllegalArgumentException("DeviceKeyContext 不完整: " + context);
        }
        String businessDomain = resolveDomain(context.bizType());
        String purpose = StrUtil.isNotBlank(context.purpose())
                ? context.purpose()
                : authorizationPolicy.defaultPurpose(businessDomain);
        return new BusinessKeyDirectoryQuery(context.deviceSn(), businessDomain, purpose);
    }

    /**
     * 解析 BizType → 业务域（配置映射，缺省 identity）
     */
    private String resolveDomain(BizType bizType) {
        String mapped = mappingProperties.getBizTypeDomain().get(bizType.name());
        return StrUtil.isNotBlank(mapped) ? mapped.toUpperCase(Locale.ROOT) : bizType.name();
    }

    /**
     * 解析业务域 → framework BizType（create 分类标签需要）
     * <p>配置映射优先，缺省取业务域名本身（业务域恰为 BizType 名时）；无法映射 fail-closed。
     *
     * @param businessDomain 受治理业务域代码
     * @return framework BizType
     * @throws net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException 业务域无法映射 BizType
     */
    public BizType resolveBizType(String businessDomain) {
        String bizTypeName = mappingProperties.getDomainBizType().get(businessDomain.toUpperCase(Locale.ROOT));
        if (StrUtil.isBlank(bizTypeName)) {
            bizTypeName = businessDomain;
        }
        try {
            BizType bizType = BizType.fromName(bizTypeName);
            if (bizType == null) {
                throw new net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException(
                        "业务域无法映射 framework BizType: " + businessDomain);
            }
            return bizType;
        } catch (IllegalArgumentException e) {
            throw new net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException(
                    "业务域无法映射 framework BizType: " + businessDomain);
        }
    }
}
