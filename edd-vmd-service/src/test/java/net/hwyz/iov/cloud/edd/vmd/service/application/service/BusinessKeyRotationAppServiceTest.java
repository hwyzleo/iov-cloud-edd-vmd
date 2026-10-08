package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRotationConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyWrapFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyRotationAppService 单元测试（CR-055 F20 §6.4）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyRotationAppServiceTest {

    private static final String REQUEST_ID = "BK-ROT-0001";
    private static final String DEVICE_SN = "SN001";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";
    private static final String OLD_KEY_ID = "KEY-0001";
    private static final String NEW_KEY_ID = "KEY-0002";

    @Mock
    private DeviceBusinessKeyRepository repository;
    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    @Mock
    private ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    @Mock
    private BusinessKeyAuthorizationPolicy authorizationPolicy;
    @Mock
    private BusinessKeyBizTypeMapper bizTypeMapper;
    @Mock
    private BusinessKeyMaterialTemplate materialTemplate;

    @Mock
    private org.springframework.beans.factory.ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    @Mock
    private BusinessKeyChangedPublisher publisher;
    @Mock
    private BusinessKeyCacheInvalidator cacheInvalidator;

    @Mock
    private org.springframework.beans.factory.ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private BusinessKeyRotationAppService service;

    private BoundDeviceIdentity identity;
    private BusinessKeyPolicyEntry policyEntry;
    private DeviceBusinessKey oldActive;
    private DeviceBusinessKey newRow;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        lenient().when(businessKeyMaterialTemplateProvider.getIfAvailable()).thenReturn(materialTemplate);
        lenient().when(cacheInvalidatorProvider.getIfAvailable()).thenReturn(cacheInvalidator);
        service = new BusinessKeyRotationAppService(repository, boundDeviceIdentityResolver,
                activeDeviceCertificateResolver, authorizationPolicy, bizTypeMapper,
                businessKeyMaterialTemplateProvider, publisher, cacheInvalidatorProvider, transactionManager);
        identity = new BoundDeviceIdentity("HWYZTEST900000001", 10L, 20L, DEVICE_SN, "TBOX",
                "00000000000000000000000000000001", "BOTH");
        policyEntry = new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of("TBOX"),
                java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1");
        oldActive = DeviceBusinessKey.builder()
                .id(1L)
                .keyId(OLD_KEY_ID)
                .kmsKeyRef("kms/ref/" + OLD_KEY_ID)
                .businessKeyVersion(1L)
                .keyState(BusinessKeyState.ACTIVE)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn(DEVICE_SN)
                .rowVersion(5)
                .build();
        newRow = DeviceBusinessKey.builder()
                .id(2L)
                .requestId(REQUEST_ID)
                .businessKeyVersion(2L)
                .keyState(BusinessKeyState.PENDING)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn(DEVICE_SN)
                .rowVersion(1)
                .build();
    }

    private BusinessKeyRotateCmd cmd() {
        return BusinessKeyRotateCmd.builder()
                .requestId(REQUEST_ID)
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .build();
    }

    private BusinessKeyMaterial material() {
        return new BusinessKeyMaterial(NEW_KEY_ID, "kms/ref/" + NEW_KEY_ID, 1, "kms", "AES", "AES-256-GCM",
                Instant.now().minusSeconds(1), Instant.now().plusSeconds(31536000));
    }

    @Test
    void rotate_success_versionIncrementedAndSwitched() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(oldActive);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            k.setRowVersion(1);
            return 1;
        });
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn("CR055-CERT-0001");
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class))).thenReturn(material());
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(new WrappedBusinessKey(new byte[]{1}, NEW_KEY_ID, 1, "AES", Instant.now().plusSeconds(60), Map.of()));
        // switchActive 重读
        when(repository.selectById(1L)).thenReturn(oldActive);
        when(repository.selectById(2L)).thenReturn(newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        BusinessKeyRotationResult result = service.rotate(cmd());

        assertEquals(NEW_KEY_ID, result.getNewKeyId());
        assertEquals(2L, result.getNewBusinessKeyVersion());
        assertEquals(OLD_KEY_ID, result.getOldKeyId());
        // 旧行被置 DEPRECATED，新行置 ACTIVE
        org.mockito.ArgumentCaptor<DeviceBusinessKey> captor = org.mockito.ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeast(2)).update(captor.capture());
        boolean oldDeprecated = captor.getAllValues().stream()
                .anyMatch(k -> k.getKeyId() != null && k.getKeyId().equals(OLD_KEY_ID)
                        && k.getKeyState() == BusinessKeyState.DEPRECATED);
        assertTrue(oldDeprecated, "旧 ACTIVE 应转为 DEPRECATED");
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("ROTATED"));
        verify(cacheInvalidator).invalidateKey(OLD_KEY_ID);
    }

    @Test
    void rotate_noActive_notExist() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(null);
        assertThrows(BusinessKeyNotExistException.class, () -> service.rotate(cmd()));
        verify(materialTemplate, never()).create(any(BusinessKeyCreateRequest.class));
    }

    @Test
    void rotate_createFailure_doesNotDegradeOld() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(oldActive);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            k.setRowVersion(1);
            return 1;
        });
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class)))
                .thenThrow(new CryptoDependencyUnavailableException("kms down"));
        when(repository.selectById(2L)).thenReturn(newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        assertThrows(BusinessKeyKmsUnavailableException.class, () -> service.rotate(cmd()));

        // 旧 ACTIVE 不得被降级
        verify(repository, never()).update(argThat(k -> k.getId() != null && k.getId().equals(1L)
                && k.getKeyState() == BusinessKeyState.DEPRECATED));
    }

    @Test
    void rotate_wrapVerifyFailure_newRowFailed() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(oldActive);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            k.setRowVersion(1);
            return 1;
        });
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn("CR055-CERT-0001");
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class))).thenReturn(material());
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenThrow(new net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyWrapFailedException("verify nope"));
        when(repository.selectById(2L)).thenReturn(newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        assertThrows(BusinessKeyWrapFailedException.class, () -> service.rotate(cmd()));

        // 新行失败留痕，旧行不得降级
        org.mockito.ArgumentCaptor<DeviceBusinessKey> captor = org.mockito.ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        boolean newFailed = captor.getAllValues().stream()
                .anyMatch(k -> k.getId() != null && k.getId().equals(2L) && k.getKeyState() == BusinessKeyState.FAILED);
        assertTrue(newFailed, "新行应标记 FAILED");
    }

    @Test
    void rotate_switchConflict_oldUpdateZeroRows() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(oldActive);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            k.setRowVersion(1);
            return 1;
        });
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn("CR055-CERT-0001");
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class))).thenReturn(material());
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(new WrappedBusinessKey(new byte[]{1}, NEW_KEY_ID, 1, "AES", Instant.now().plusSeconds(60), Map.of()));
        // 旧 ACTIVE 乐观锁失败：update 返回 0
        when(repository.selectById(1L)).thenReturn(oldActive);
        when(repository.update(argThat(k -> k.getId() != null && k.getId().equals(1L)))).thenReturn(0);

        assertThrows(BusinessKeyRotationConflictException.class, () -> service.rotate(cmd()));
    }
}
