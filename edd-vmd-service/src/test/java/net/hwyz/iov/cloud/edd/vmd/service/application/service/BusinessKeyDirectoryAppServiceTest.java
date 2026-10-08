package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyMultipleActiveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.BusinessKeyMappingProperties;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyDescriptor;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.DeviceKeyContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.KeyOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * BusinessKeyDirectoryAppService 单元测试（CR-055 F20 §6.2/§6.3）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyDirectoryAppServiceTest {

    private static final String DEVICE_SN = "SN001";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";
    private static final String KEY_ID = "KEY-0001";

    @Mock
    private DeviceBusinessKeyRepository repository;

    private BusinessKeyDirectoryAppService directoryService;

    @BeforeEach
    void setUp() {
        BusinessKeyAuthorizationPolicy authPolicy = new BusinessKeyAuthorizationPolicy() {
            @Override
            public BusinessKeyPolicyEntry authorize(BusinessKeyAction action, String deviceCategory,
                                                    String businessDomain, String purpose) {
                return new BusinessKeyPolicyEntry(businessDomain, purpose, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1");
            }

            @Override
            public String defaultPurpose(String businessDomain) {
                return PURPOSE;
            }
        };
        BusinessKeyMappingProperties mappingProps = new BusinessKeyMappingProperties();
        mappingProps.getBizTypeDomain().put("TBOX_DEVICE_ROOT", DOMAIN);
        BusinessKeyBizTypeMapper bizTypeMapper = new BusinessKeyBizTypeMapper(mappingProps, authPolicy);
        directoryService = new BusinessKeyDirectoryAppService(repository, authPolicy, bizTypeMapper);
    }

    private DeviceBusinessKey activeKey() {
        return DeviceBusinessKey.builder()
                .keyId(KEY_ID)
                .businessKeyVersion(2L)
                .kmsKeyRef("kms/ref/" + KEY_ID)
                .keyState(BusinessKeyState.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(364))
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn(DEVICE_SN)
                .build();
    }

    @Test
    void resolveActive_ok() {
        when(repository.selectActiveByContext(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(activeKey());
        when(repository.countActiveByContext(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1);
        BusinessKeyDescriptor descriptor = directoryService.resolveActive(
                new DeviceKeyContext(DEVICE_SN, net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT, PURPOSE, null));
        assertNotNull(descriptor);
        assertEquals(KEY_ID, descriptor.keyId());
        assertEquals(2L, descriptor.businessKeyVersion());
        assertEquals(net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.CryptoKeyState.ACTIVE, descriptor.state());
    }

    @Test
    void resolveActive_noActive_notExist() {
        when(repository.selectActiveByContext(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(null);
        assertThrows(BusinessKeyNotExistException.class, () ->
                directoryService.resolveActive(new BusinessKeyDirectoryQuery(DEVICE_SN, DOMAIN, PURPOSE)));
    }

    @Test
    void resolveActive_multiActive_failClosed() {
        when(repository.selectActiveByContext(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(activeKey());
        when(repository.countActiveByContext(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(2);
        assertThrows(BusinessKeyMultipleActiveException.class, () ->
                directoryService.resolveActive(new BusinessKeyDirectoryQuery(DEVICE_SN, DOMAIN, PURPOSE)));
    }

    @Test
    void resolveByKeyId_active_ok() {
        when(repository.selectByKeyId(KEY_ID)).thenReturn(activeKey());
        BusinessKeyDescriptor descriptor = directoryService.resolveByKeyId(KEY_ID, KeyOperation.DECRYPT);
        assertEquals(KEY_ID, descriptor.keyId());
    }

    @Test
    void resolveByKeyId_deprecatedWithinWindow_ok() {
        DeviceBusinessKey deprecated = activeKey();
        deprecated.setKeyState(BusinessKeyState.DEPRECATED);
        deprecated.setDecryptUntil(LocalDateTime.now().plusDays(3));
        when(repository.selectByKeyId(KEY_ID)).thenReturn(deprecated);
        BusinessKeyDescriptor descriptor = directoryService.resolveByKeyId(KEY_ID, KeyOperation.DECRYPT);
        assertEquals(net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.CryptoKeyState.DEPRECATED, descriptor.state());
    }

    @Test
    void resolveByKeyId_deprecatedPastWindow_rejected() {
        DeviceBusinessKey deprecated = activeKey();
        deprecated.setKeyState(BusinessKeyState.DEPRECATED);
        deprecated.setDecryptUntil(LocalDateTime.now().minusDays(1));
        when(repository.selectByKeyId(KEY_ID)).thenReturn(deprecated);
        assertThrows(BusinessKeyStateNotAllowedException.class,
                () -> directoryService.resolveByKeyId(KEY_ID, KeyOperation.DECRYPT));
    }

    @Test
    void resolveByKeyId_revoked_rejected() {
        DeviceBusinessKey revoked = activeKey();
        revoked.setKeyState(BusinessKeyState.REVOKED);
        when(repository.selectByKeyId(KEY_ID)).thenReturn(revoked);
        assertThrows(BusinessKeyStateNotAllowedException.class,
                () -> directoryService.resolveByKeyId(KEY_ID, KeyOperation.DECRYPT));
    }

    @Test
    void resolveByKeyId_notFound_notExist() {
        when(repository.selectByKeyId("NOPE")).thenReturn(null);
        assertThrows(BusinessKeyNotExistException.class,
                () -> directoryService.resolveByKeyId("NOPE", KeyOperation.DECRYPT));
    }

    @Test
    void getMetadata_ok() {
        when(repository.selectByKeyId(KEY_ID)).thenReturn(activeKey());
        var meta = directoryService.getMetadata(KEY_ID);
        assertEquals(KEY_ID, meta.getKeyId());
        assertEquals("kms/ref/" + KEY_ID, meta.getKmsKeyRef());
        assertEquals(2L, meta.getBusinessKeyVersion());
    }

    @Test
    void queryByDevice_filtered() {
        DeviceBusinessKey k1 = activeKey();
        k1.setPurpose(PURPOSE);
        k1.setBusinessKeyVersion(1L);
        DeviceBusinessKey k2 = activeKey();
        k2.setPurpose("OTHER");
        k2.setBusinessKeyVersion(2L);
        when(repository.selectByDeviceSn(DEVICE_SN)).thenReturn(java.util.List.of(k1, k2));
        var result = directoryService.queryByDevice(DEVICE_SN, DOMAIN, PURPOSE);
        assertEquals(1, result.size());
        assertEquals(PURPOSE, result.get(0).getPurpose());
    }
}
