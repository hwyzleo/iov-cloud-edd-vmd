package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyPolicyProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 版本化业务密钥策略注册表（CR-055 §3.3 / CR-058 多用途）
 * <p>
 * 管理调用方、设备类别、业务域、用途、允许操作、算法、有效期和重叠窗口。
 * 数据来自 {@link BusinessKeyPolicyProperties}，默认空（fail-closed）；
 * 同 (businessDomain, purpose) 重复登记启动期 fail-fast。
 * CR-058 支持同一域声明多用途集合（purposes），逐用途登记但共享同一策略条目。
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

    /**
     * 域 → 完整用途集合（CR-058 多用途域）
     */
    private final Map<String, Set<String>> purposesByDomain = new HashMap<>();

    @PostConstruct
    public void init() {
        if (properties.getEntries() == null || properties.getEntries().isEmpty()) {
            log.warn("业务密钥授权策略未配置任何条目（fail-closed）：vmd.business-key.policy.entries 为空，域/用途授权将全部拒绝");
            return;
        }
        for (BusinessKeyPolicyProperties.Entry entry : properties.getEntries()) {
            String domain = normalize(entry.getBusinessDomain());
            Set<String> purposes = resolveConfiguredPurposes(entry);
            if (domain == null || purposes.isEmpty()) {
                throw new IllegalStateException("业务密钥策略条目缺少 businessDomain 或 purpose(s): " + entry);
            }
            Set<String> deviceCategories = entry.getDeviceCategories() == null ? Set.of()
                    : entry.getDeviceCategories().stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            Set<BusinessKeyAction> allowedActions = entry.getAllowedActions() == null ? Set.of()
                    : Set.copyOf(entry.getAllowedActions());

            for (String purpose : purposes) {
                String key = contextKey(domain, purpose);
                if (entries.containsKey(key)) {
                    throw new IllegalStateException("业务密钥策略重复登记: businessDomain=" + entry.getBusinessDomain()
                            + ", purpose=" + purpose);
                }
                entries.put(key, new BusinessKeyPolicyEntry(
                        domain, purpose, purposes, deviceCategories, allowedActions,
                        entry.getAlgorithm(), entry.getKeySpec(), entry.getValidity(), entry.getDecryptWindow(),
                        properties.getVersion()));
            }
            purposesByDomain.merge(domain, new LinkedHashSet<>(purposes), (a, b) -> {
                a.addAll(b);
                return a;
            });
            if (entry.getDefaultPurpose() != null && !entry.getDefaultPurpose().isBlank()) {
                defaultPurposeByDomain.putIfAbsent(domain, normalize(entry.getDefaultPurpose()));
            }
        }
        log.info("业务密钥授权策略已加载: version={}, entries={}, domains={}",
                properties.getVersion(), entries.size(), purposesByDomain.size());
    }

    private Set<String> resolveConfiguredPurposes(BusinessKeyPolicyProperties.Entry entry) {
        if (entry.getPurposes() != null && !entry.getPurposes().isEmpty()) {
            return entry.getPurposes().stream()
                    .map(s -> normalize(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        String purpose = normalize(entry.getPurpose());
        if (purpose == null) {
            return Set.of();
        }
        Set<String> single = new LinkedHashSet<>();
        single.add(purpose);
        return single;
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
     * 解析业务域登记的完整用途集合（CR-058 多用途域）
     *
     * @param businessDomain 业务域
     * @return 用途集合，未登记返回 empty
     */
    public Optional<Set<String>> resolvePurposes(String businessDomain) {
        if (businessDomain == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(purposesByDomain.get(normalize(businessDomain)));
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
