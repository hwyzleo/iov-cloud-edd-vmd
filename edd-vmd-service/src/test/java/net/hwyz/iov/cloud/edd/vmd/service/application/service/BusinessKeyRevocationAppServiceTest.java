package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
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
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RevocationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyRevocationAppService 单元测试（CR-055 F20 §6.5 / CR-061 §9.2 材料路由）。
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
    private ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    @Mock
    private BusinessKeyChangedPublisher publisher;
    @Mock
    private BusinessKeyCacheInvalidator cacheInvalidator;
    @Mock
    private ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
    @Mock
    private KeyprovProviderReadinessGate providerReadinessGate;
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
                businessKeyMaterialTemplateProvider, publisher, cacheInvalidatorProvider,
                providerReadinessGate, transactionManager);
    }

    private DeviceBusinessKey activeKey(String materialModel) {
        return DeviceBusinessKey.builder()
                .id(1L)
                .keyId(KEY_ID)
                .kmsKeyRef("kms/ref/" + KEY_ID)
                .businessKeyVersion(2L)
                .keyState(BusinessKeyState.ACTIVE)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn("SN001")
                .materialModel(materialModel)
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

    private void stubAuthorize() {
        when(authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null, DOMAIN, PURPOSE))
                .thenReturn(new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, Set.of(),
                        Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                        Duration.ofDays(365), Duration.ofDays(7), "v1"));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL 吊销：REVOKING 阻断 → Provider revoke → REVOKED + 缓存失效")
    void revoke_randomMaterial_success() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class))).thenReturn(null);

        BusinessKeyRevokeResult result = service.revoke(cmd());

        assertEquals("REVOKED", result.getState());
        verify(materialTemplate).revoke(any(BusinessKeyRef.class), any(RevocationRequest.class));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKING"));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
        verify(cacheInvalidator).invalidateKey(KEY_ID);
    }

    @Test
    @DisplayName("DERIVED_ROOT（含 V2C）吊销：仅目录状态阻断，不调用 Provider per-key revoke")
    void revoke_derivedRoot_stateBlocking_noProviderRevoke() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.DERIVED_ROOT.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        BusinessKeyRevokeResult result = service.revoke(cmd());

        assertEquals("REVOKED", result.getState());
        verify(materialTemplate, never()).revoke(any(), any());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKING"));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
        verify(cacheInvalidator).invalidateKey(KEY_ID);
    }

    @Test
    @DisplayName("历史 material_model IS NULL 吊销：不猜测来源，仅目录状态阻断")
    void revoke_legacyNull_stateBlocking_noProviderRevoke() {
        DeviceBusinessKey key = activeKey(null);
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        BusinessKeyRevokeResult result = service.revoke(cmd());

        assertEquals("REVOKED", result.getState());
        verify(materialTemplate, never()).revoke(any(), any());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
        verify(cacheInvalidator).invalidateKey(KEY_ID);
    }

    @Test
    @DisplayName("密钥不存在 → BusinessKeyNotExist，不调用 Provider")
    void revoke_notFound_throws() {
        when(repository.selectByKeyId(KEY_ID)).thenReturn(null);
        assertThrows(BusinessKeyNotExistException.class, () -> service.revoke(cmd()));
        verify(materialTemplate, never()).revoke(any(), any());
    }

    @Test
    @DisplayName("RANDOM_MATERIAL：KMS 不存在且 Provider READY → 幂等成功")
    void revoke_kmsNotFound_idempotentSuccess() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(providerReadinessGate.isReady()).thenReturn(true);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new BusinessKeyNotFoundException("already gone"));

        BusinessKeyRevokeResult result = service.revoke(cmd());
        assertEquals("REVOKED", result.getState());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL：KMS 404 但 Provider 未就绪 → fail-closed 不收敛为成功")
    void revoke_kmsNotFound_providerNotReady_failClosed() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(providerReadinessGate.isReady()).thenReturn(false);
        when(providerReadinessGate.reason()).thenReturn("crypto.kms.business-key.enabled=false");
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new BusinessKeyNotFoundException("not found"));

        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException.class,
                () -> service.revoke(cmd()));
        verify(publisher, never()).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL：结果未知 → RECONCILE_REQUIRED 继续阻断")
    void revoke_outcomeUnknown_reconcileRequired() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new CryptoOperationOutcomeUnknownException("unknown"));

        assertThrows(BusinessKeyOutcomeUnknownException.class, () -> service.revoke(cmd()));

        ArgumentCaptor<DeviceBusinessKey> captor = ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(k -> k.getKeyState() == BusinessKeyState.RECONCILE_REQUIRED), "结果未知应转 RECONCILE_REQUIRED");
    }

    @Test
    @DisplayName("RANDOM_MATERIAL：Provider 吊销失败 → REVOKING 保持阻断")
    void revoke_failed_keepsRevoking() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(materialTemplate.revoke(any(BusinessKeyRef.class), any(RevocationRequest.class)))
                .thenThrow(new net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyRevocationFailedException("nope"));

        assertThrows(BusinessKeyRevocationFailedException.class, () -> service.revoke(cmd()));
        verify(publisher, never()).publish(any(DeviceBusinessKey.class), eq("REVOKED"));
    }

    @Test
    @DisplayName("状态不允许吊销（PENDING）→ BusinessKeyStateNotAllowed")
    void revoke_stateNotAllowed_pendingRejected() {
        DeviceBusinessKey key = activeKey(KeyMaterialModel.RANDOM_MATERIAL.name());
        key.setKeyState(BusinessKeyState.PENDING);
        when(repository.selectByKeyId(KEY_ID)).thenReturn(key);
        stubAuthorize();
        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException.class,
                () -> service.revoke(cmd()));
    }
}
