package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyPolicyProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 版本化业务密钥策略注册表（CR-055 §3.3）
 * <p>
 * 管理调用方、设备类别、业务域、用途、允许操作、算法、有效期和重叠窗口。
 * 数据来自 {@link BusinessKeyPolicyProperties}，默认空（fail-closed）；
 * 同 (businessDomain, purpose) 重复登记启动期 fail-fast。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessKeyPolicyRegistry {

    private final BusinessKeyPolicyProperties properties;

    /**
     * (domain:purpose 小写) → 策略条目
     */
    private final Map<String, BusinessKeyPolicyEntry> entries = new HashMap<>();

    /**
     * 域默认用途（域小写 → 默认用途），供 framework 运行时目录解析（purpose 为空）使用
     */
    private final Map<String, String> defaultPurposeByDomain = new HashMap<>();

    @PostConstruct
    public void init() {
        if (properties.getEntries() == null || properties.getEntries().isEmpty()) {
            log.warn("业务密钥授权策略未配置任何条目（fail-closed）：vmd.business-key.policy.entries 为空，域/用途授权将全部拒绝");
            return;
        }
        for (BusinessKeyPolicyProperties.Entry entry : properties.getEntries()) {
            String domain = normalize(entry.getBusinessDomain());
            String purpose = normalize(entry.getPurpose());
            String key = contextKey(domain, purpose);
            if (entries.containsKey(key)) {
                throw new IllegalStateException("业务密钥策略重复登记: businessDomain=" + entry.getBusinessDomain()
                        + ", purpose=" + entry.getPurpose());
            }
            BusinessKeyPolicyEntry policyEntry = new BusinessKeyPolicyEntry(
                    domain,
                    purpose,
                    entry.getDeviceCategories() == null ? java.util.Set.of() : entry.getDeviceCategories().stream().map(String::toUpperCase).collect(Collectors.toSet()),
                    entry.getAllowedActions() == null ? java.util.Set.of() : java.util.Set.copyOf(entry.getAllowedActions()),
                    entry.getAlgorithm(),
                    entry.getKeySpec(),
                    entry.getValidity(),
                    entry.getDecryptWindow(),
                    properties.getVersion());
            entries.put(key, policyEntry);
            if (entry.getDefaultPurpose() != null && !entry.getDefaultPurpose().isBlank()) {
                defaultPurposeByDomain.putIfAbsent(domain, entry.getDefaultPurpose());
            }
        }
        log.info("业务密钥授权策略已加载: version={}, entries={}", properties.getVersion(), entries.size());
    }

    /**
     * 按 (businessDomain, purpose) 解析策略
     *
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return 策略条目，未登记返回 empty
     */
    public Optional<BusinessKeyPolicyEntry> resolve(String businessDomain, String purpose) {
        if (businessDomain == null || purpose == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(contextKey(normalize(businessDomain), normalize(purpose))));
    }

    /**
     * 解析域默认用途（framework 运行时 purpose 为空时）
     *
     * @param businessDomain 业务域
     * @return 默认用途，未配置返回 empty
     */
    public Optional<String> defaultPurpose(String businessDomain) {
        return Optional.ofNullable(defaultPurposeByDomain.get(normalize(businessDomain)));
    }

    /**
     * 策略版本
     */
    public String version() {
        return properties.getVersion();
    }

    private static String contextKey(String domain, String purpose) {
        return domain + ":" + purpose;
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }
}
