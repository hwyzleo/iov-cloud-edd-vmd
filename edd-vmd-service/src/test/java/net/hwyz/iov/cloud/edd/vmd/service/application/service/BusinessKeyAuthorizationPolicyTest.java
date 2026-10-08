package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyPolicyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务密钥授权策略单元测试（CR-055 §3.3）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
class BusinessKeyAuthorizationPolicyTest {

    private BusinessKeyPolicyRegistry registry;
    private DefaultBusinessKeyAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {
        BusinessKeyPolicyProperties props = new BusinessKeyPolicyProperties();
        props.setVersion("v1");
        BusinessKeyPolicyProperties.Entry entry = new BusinessKeyPolicyProperties.Entry();
        entry.setBusinessDomain("SECURE_CHANNEL");
        entry.setPurpose("SESSION_ENC");
        entry.setDeviceCategories(List.of("TBOX"));
        entry.setAllowedActions(List.of(BusinessKeyAction.PROVISION, BusinessKeyAction.RESOLVE_ACTIVE,
                BusinessKeyAction.DECRYPT, BusinessKeyAction.ROTATE, BusinessKeyAction.REVOKE));
        entry.setAlgorithm("AES");
        entry.setKeySpec("AES-256-GCM");
        entry.setValidity(Duration.ofDays(365));
        entry.setDecryptWindow(Duration.ofDays(7));
        entry.setDefaultPurpose("SESSION_ENC");
        props.setEntries(List.of(entry));

        registry = new BusinessKeyPolicyRegistry(props);
        registry.init();
        policy = new DefaultBusinessKeyAuthorizationPolicy(registry);
    }

    @Test
    void authorize_allowed() {
        BusinessKeyPolicyEntry result = policy.authorize(
                BusinessKeyAction.PROVISION, "TBOX", "SECURE_CHANNEL", "SESSION_ENC");
        assertNotNull(result);
        assertEquals("AES-256-GCM", result.keySpec());
        assertEquals("v1", result.policyVersion());
    }

    @Test
    void authorize_deviceCategoryMismatch_rejected() {
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () ->
                policy.authorize(BusinessKeyAction.PROVISION, "CCU", "SECURE_CHANNEL", "SESSION_ENC"));
    }

    @Test
    void authorize_actionNotAllowed_rejected() {
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () ->
                policy.authorize(BusinessKeyAction.REVOKE, "TBOX", "SECURE_CHANNEL", "OTHER_PURPOSE"));
    }

    @Test
    void authorize_unknownDomain_rejected() {
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () ->
                policy.authorize(BusinessKeyAction.PROVISION, "TBOX", "UNKNOWN_DOMAIN", "SESSION_ENC"));
    }

    @Test
    void authorize_emptyRegistry_failClosed() {
        BusinessKeyPolicyRegistry empty = new BusinessKeyPolicyRegistry(new BusinessKeyPolicyProperties());
        empty.init();
        DefaultBusinessKeyAuthorizationPolicy emptyPolicy = new DefaultBusinessKeyAuthorizationPolicy(empty);
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () ->
                emptyPolicy.authorize(BusinessKeyAction.PROVISION, "TBOX", "SECURE_CHANNEL", "SESSION_ENC"));
    }

    @Test
    void defaultPurpose_resolved() {
        assertEquals("SESSION_ENC", policy.defaultPurpose("SECURE_CHANNEL"));
    }

    @Test
    void defaultPurpose_unknownDomain_rejected() {
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () -> policy.defaultPurpose("UNKNOWN"));
    }
}
