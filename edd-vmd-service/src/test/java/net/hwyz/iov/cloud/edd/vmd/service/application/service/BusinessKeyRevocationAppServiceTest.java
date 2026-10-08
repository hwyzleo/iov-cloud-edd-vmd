package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyOutcomeUnknownException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRevocationFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyNotFoundException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RevocationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyRevocationAppService 单元测试（CR-055 F20 §6.5）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyRevocationAppServiceTest {

    private static final String REQUEST_ID = "BK-REV-0001";
    private static final String KEY_ID = "KEY-0001";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String PURPOSE = "SESSION_ENC";

    @Mock
    private DeviceBusinessKeyRepository repository;
    @Mock
    private BusinessKeyAuthorizationPolicy authorizationPolicy;
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

    private BusinessKeyRevocationAppService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        lenient().when(businessKeyMaterialTemplateProvider.getIfAvailable()).thenReturn(materialTemplate);
        lenient().when(cacheInvalidatorProvider.getIfAvailable()).thenReturn(cacheInvalidator);
        service = new BusinessKeyRevocationAppService(repository, authorizationPolicy,
                businessKeyMaterialTemplateProvider, publisher, cacheInvalidatorProvider, transactionManager);
    }

    private DeviceBusinessKey activeKey() {
        return DeviceBusinessKey.builder()
                .id(1L)
                .keyId(KEY_ID)
                .kmsKeyRef("kms/ref/" + KEY_ID)
                .businessKeyVersion(2L)
                .keyState(BusinessKeyState.ACTIVE)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn("SN001")
                .rowVersion(3)
                .build();
    }

    private BusinessKeyRevokeCmd cmd() {
        return BusinessKeyRevokeCmd.builder()
                .requestId(REQUEST_ID)
                .keyId(KEY_ID)
                .reason("泄露")
                .build();
    }

    @Test
    void revoke_success_revokingThenRevoked() {
        DeviceBusinessKey key = activeKey();
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key, key, key);
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1"));
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenReturn(null);

        BusinessKeyRevokeResult result = service.revoke(cmd());

        assertEquals("REVOKED", result.getState());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKING"));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
        verify(cacheInvalidator).invalidateKey(KEY_ID);
    }

    @Test
    void revoke_notFound_throws() {
        when(repository.selectByKeyId(KEY_ID)).thenReturn(null);
        assertThrows(BusinessKeyNotExistException.class, () -> service.revoke(cmd()));
        verify(materialTemplate, never()).revoke(any(), any());
    }

    @Test
    void revoke_kmsNotFound_idempotentSuccess() {
        DeviceBusinessKey key = activeKey();
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key, key, key);
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1"));
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new BusinessKeyNotFoundException("already gone"));

        BusinessKeyRevokeResult result = service.revoke(cmd());
        assertEquals("REVOKED", result.getState());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
    }

    @Test
    void revoke_outcomeUnknown_reconcileRequired() {
        DeviceBusinessKey key = activeKey();
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key, key, key);
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1"));
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new CryptoOperationOutcomeUnknownException("unknown"));

        assertThrows(BusinessKeyOutcomeUnknownException.class, () -> service.revoke(cmd()));

        // 状态不得恢复 ACTIVE；markReconcileRequired 已写入
        org.mockito.ArgumentCaptor<DeviceBusinessKey> captor = org.mockito.ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        boolean anyReconcile = captor.getAllValues().stream()
                .anyMatch(k -> k.getKeyState() == BusinessKeyState.RECONCILE_REQUIRED);
        assertTrue(anyReconcile, "结果未知应转 RECONCILE_REQUIRED");
    }

    @Test
    void revoke_failed_keepsRevoking() {
        DeviceBusinessKey key = activeKey();
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key, key);
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1"));
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyRevocationFailedException("nope"));

        assertThrows(BusinessKeyRevocationFailedException.class, () -> service.revoke(cmd()));
        // REVOKING 保持阻断，未发布 REVOKED
        verify(publisher, never()).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
    }

    @Test
    void revoke_stateNotAllowed_pendingRejected() {
        DeviceBusinessKey key = activeKey();
        key.setKeyState(BusinessKeyState.PENDING);
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, java.util.Set.of(),
                        java.util.Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        java.time.Duration.ofDays(365), java.time.Duration.ofDays(7), "v1"));
        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException.class,
                () -> service.revoke(cmd()));
    }
}
