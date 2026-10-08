package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDomainNotAuthorizedException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyMappingProperties;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.CallerIdentity;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.DeviceKeyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessKeyBizTypeMapper 单元测试（CR-055 §7.2）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
class BusinessKeyBizTypeMapperTest {

    private BusinessKeyMappingProperties mappingProperties;
    private BusinessKeyAuthorizationPolicy authorizationPolicy;
    private BusinessKeyBizTypeMapper mapper;

    @BeforeEach
    void setUp() {
        mappingProperties = new BusinessKeyMappingProperties();
        authorizationPolicy = new BusinessKeyAuthorizationPolicy() {
            @Override
            public BusinessKeyPolicyEntry authorize(BusinessKeyAction action, String deviceCategory,
                                                    String businessDomain, String purpose) {
                return null;
            }

            @Override
            public String defaultPurpose(String businessDomain) {
                if ("SECURE_CHANNEL".equals(businessDomain)) {
                    return "SESSION_ENC";
                }
                throw new BusinessKeyDomainNotAuthorizedException("no default: " + businessDomain);
            }
        };
        mapper = new BusinessKeyBizTypeMapper(mappingProperties, authorizationPolicy);
    }

    @Test
    void toDirectoryQuery_identityDomainAndContextPurpose() {
        DeviceKeyContext ctx = new DeviceKeyContext(
                "SN001", BizType.TBOX_DEVICE_ROOT, "SESSION_ENC", CallerIdentity.anonymous());
        BusinessKeyDirectoryQuery query = mapper.toDirectoryQuery(ctx);
        assertEquals("SN001", query.deviceSn());
        assertEquals("TBOX_DEVICE_ROOT", query.businessDomain());
        assertEquals("SESSION_ENC", query.purpose());
    }

    @Test
    void toDirectoryQuery_mappedDomainAndDefaultPurpose() {
        mappingProperties.getBizTypeDomain().put("TBOX_DEVICE_ROOT", "SECURE_CHANNEL");
        DeviceKeyContext ctx = new DeviceKeyContext(
                "SN001", BizType.TBOX_DEVICE_ROOT, null, null);
        BusinessKeyDirectoryQuery query = mapper.toDirectoryQuery(ctx);
        assertEquals("SECURE_CHANNEL", query.businessDomain());
        assertEquals("SESSION_ENC", query.purpose());
    }

    @Test
    void toDirectoryQuery_noDefaultPurpose_failClosed() {
        DeviceKeyContext ctx = new DeviceKeyContext(
                "SN001", BizType.TBOX_DEVICE_ROOT, null, null);
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () -> mapper.toDirectoryQuery(ctx));
    }

    @Test
    void resolveBizType_identityMapping() {
        assertEquals(BizType.TBOX_DEVICE_ROOT, mapper.resolveBizType("TBOX_DEVICE_ROOT"));
    }

    @Test
    void resolveBizType_mappedDomain() {
        mappingProperties.getDomainBizType().put("SECURE_CHANNEL", "TBOX_DEVICE_ROOT");
        assertEquals(BizType.TBOX_DEVICE_ROOT, mapper.resolveBizType("SECURE_CHANNEL"));
    }

    @Test
    void resolveBizType_unmappable_failClosed() {
        assertThrows(BusinessKeyDomainNotAuthorizedException.class, () -> mapper.resolveBizType("NO_SUCH_BIZ_TYPE"));
    }
}
