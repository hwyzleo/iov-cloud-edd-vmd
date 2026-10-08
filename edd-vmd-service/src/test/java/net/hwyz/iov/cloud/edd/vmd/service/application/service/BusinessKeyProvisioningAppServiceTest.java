package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyProvisionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyProvisionResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.*;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyWrapFailedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * BusinessKeyProvisioningAppService 单元测试（CR-055 F20 §6.1）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyProvisioningAppServiceTest {

    private static final String REQUEST_ID = "BK-REQ-0001";
    private static final String DEVICE_SN = "SN001";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";
    private static final String KEY_ID = "KEY-0001";
    private static final String CERT_SN = "CR055-CERT-0001";
    private static final String KMS_REF = "kms/ref/KEY-0001";

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
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private BusinessKeyProvisioningAppService service;

    private BoundDeviceIdentity identity;
    private BusinessKeyPolicyEntry policyEntry;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        lenient().when(businessKeyMaterialTemplateProvider.getIfAvailable()).thenReturn(materialTemplate);
        identity = new BoundDeviceIdentity("HWYZTEST900000001", 10L, 20L, DEVICE_SN, "TBOX",
                "00000000000000000000000000000001", "BOTH");
        policyEntry = new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of("TBOX"),
                java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1");
    }

    private BusinessKeyProvisionCmd cmd() {
        return BusinessKeyProvisionCmd.builder()
                .requestId(REQUEST_ID)
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .build();
    }

    private BusinessKeyMaterial material() {
        return new BusinessKeyMaterial(KEY_ID, KMS_REF, 3, "kms", "AES", "AES-256-GCM",
                Instant.now().minusSeconds(1), Instant.now().plusSeconds(31536000));
    }

    private WrappedBusinessKey wrapped() {
        return new WrappedBusinessKey(new byte[]{1, 2, 3}, KEY_ID, 3, "AES",
                Instant.now().plusSeconds(3600), Map.of("iv", "abcd"));
    }

    @Test
    void provision_freshCreate_success() {
        when(repository.selectByRequestId(REQUEST_ID)).thenReturn(null);
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.PROVISION, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn(CERT_SN);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);

        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class))).thenReturn(material());
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(wrapped());
        when(repository.selectById(any())).thenAnswer(inv -> {
            DeviceBusinessKey row = new DeviceBusinessKey();
            row.setId(100L);
            row.setKeyState(BusinessKeyState.ACTIVE);
            return row;
        });

        BusinessKeyProvisionResult result = service.provision(cmd());

        assertEquals(KEY_ID, result.getKeyId());
        assertEquals(2L, result.getBusinessKeyVersion());
        assertFalse(result.isReused());
        assertNotNull(result.getWrappedKeyBase64());
        verify(materialTemplate).create(any(BusinessKeyCreateRequest.class));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("ACTIVE"));
    }

    @Test
    void provision_idempotentReuse_active() {
        DeviceBusinessKey existing = DeviceBusinessKey.builder()
                .id(100L)
                .requestId(REQUEST_ID)
                .requestDigest("SN001|TBOX|SECURE_CHANNEL|SESSION_ENC")
                .keyId(KEY_ID)
                .kmsKeyRef(KMS_REF)
                .businessKeyVersion(2L)
                .keyState(BusinessKeyState.ACTIVE)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn(DEVICE_SN)
                .build();
        when(repository.selectByRequestId(REQUEST_ID)).thenReturn(existing);
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn(CERT_SN);
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(wrapped());
        when(repository.selectById(any())).thenReturn(existing);

        BusinessKeyProvisionResult result = service.provision(cmd());

        assertTrue(result.isReused());
        verify(materialTemplate, never()).create(any(BusinessKeyCreateRequest.class));
        verify(publisher, never()).publish(any(DeviceBusinessKey.class), any());
    }

    @Test
    void provision_sameRequestDifferentDigest_conflict() {
        DeviceBusinessKey existing = DeviceBusinessKey.builder()
                .requestId(REQUEST_ID)
                .requestDigest("OTHER|DIGEST")
                .keyId(KEY_ID)
                .keyState(BusinessKeyState.ACTIVE)
                .build();
        when(repository.selectByRequestId(REQUEST_ID)).thenReturn(existing);
        assertThrows(BusinessKeyIdempotencyConflictException.class, () -> service.provision(cmd()));
        verify(materialTemplate, never()).create(any(BusinessKeyCreateRequest.class));
    }

    @Test
    void provision_sessionMismatch_rejected_noFrameworkCall() {
        BusinessKeyProvisionCmd mismatch = BusinessKeyProvisionCmd.builder()
                .requestId(REQUEST_ID)
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .sessionDeviceSn("OTHER-DEVICE")
                .build();
        assertThrows(BusinessKeyDeviceSessionMismatchException.class, () -> service.provision(mismatch));
        verify(materialTemplate, never()).create(any(BusinessKeyCreateRequest.class));
        verify(materialTemplate, never()).wrap(any(), any(), any());
    }

    @Test
    void provision_createOutcomeUnknown_reconcileRequired() {
        when(repository.selectByRequestId(REQUEST_ID)).thenReturn(null);
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.PROVISION, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn(CERT_SN);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class)))
                .thenThrow(new CryptoOperationOutcomeUnknownException("boom"));

        assertThrows(BusinessKeyOutcomeUnknownException.class, () -> service.provision(cmd()));

        // 断言行被标记 RECONCILE_REQUIRED（捕获最后写入的实体）
        org.mockito.ArgumentCaptor<DeviceBusinessKey> captor = org.mockito.ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        DeviceBusinessKey updated = captor.getValue();
        assertNotNull(updated);
        assertEquals(BusinessKeyState.RECONCILE_REQUIRED, updated.getKeyState());
    }

    @Test
    void provision_wrapFailure_throwsWrapFailed() {
        when(repository.selectByRequestId(REQUEST_ID)).thenReturn(null);
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.PROVISION, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(activeDeviceCertificateResolver.resolveActiveCertSn(identity)).thenReturn(CERT_SN);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(net.hwyz.iov.cloud.framework.security.crypto.model.BizType.TBOX_DEVICE_ROOT);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.create(any(BusinessKeyCreateRequest.class))).thenReturn(material());
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenThrow(new BusinessKeyWrapFailedException("wrap nope"));

        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyWrapFailedException.class,
                () -> service.provision(cmd()));
    }
}
