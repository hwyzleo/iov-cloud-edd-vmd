package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRotationConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyAnchor;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
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
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyRotationAppService 单元测试（CR-055 F20 §6.4 / CR-061 §9.3 材料路由）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyRotationAppServiceTest {

    private static final String REQUEST_ID = "BK-ROT-0001";
    private static final String DEVICE_SN = "SN001";
    private static final String DOMAIN = "SECURE_CHANNEL";
    private static final String V2C_DOMAIN = "V2C_COMM";
    private static final String PURPOSE = "UPLINK_AEAD";
    private static final String OLD_KEY_ID = "KEY-0001";
    private static final String NEW_KEY_ID = "KEY-0002";

    @Mock
    private DeviceBusinessKeyRepository repository;
    @Mock
    private BusinessKeyProvisioningItemRepository itemRepository;
    @Mock
    private BusinessKeyProvisioningMetrics metrics;
    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    @Mock
    private ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    @Mock
    private BusinessKeyAuthorizationPolicy authorizationPolicy;
    @Mock
    private BusinessKeyBizTypeMapper bizTypeMapper;
    @Mock
    private BusinessKeyMaterialRouter materialRouter;
    @Mock
    private KeyProvisioningTemplate derivedTemplate;
    @Mock
    private BusinessKeyMaterialTemplate randomTemplate;
    @Mock
    private BusinessKeyChangedPublisher publisher;
    @Mock
    private BusinessKeyCacheInvalidator cacheInvalidator;
    @Mock
    private ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
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
        lenient().when(cacheInvalidatorProvider.getIfAvailable()).thenReturn(cacheInvalidator);
        LogicalKeyGenerationPolicy generationPolicy = new LogicalKeyGenerationPolicy();
        BusinessKeyDerivedMaterialService derivedMaterialService = new BusinessKeyDerivedMaterialService(
                repository, itemRepository, generationPolicy, metrics);
        lenient().when(itemRepository.selectByDirectoryKeyId(any())).thenReturn(null);
        service = new BusinessKeyRotationAppService(repository, boundDeviceIdentityResolver,
                activeDeviceCertificateResolver, authorizationPolicy, bizTypeMapper, materialRouter,
                publisher, derivedMaterialService, metrics, cacheInvalidatorProvider, transactionManager);
        identity = new BoundDeviceIdentity("HWYZTEST900000001", 10L, 20L, DEVICE_SN, "TBOX",
                "00000000000000000000000000000001", "BOTH");
        policyEntry = new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, Set.of("TBOX"),
                Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                Duration.ofDays(365), Duration.ofDays(7), "v1");
    }

    private BusinessKeyRotateCmd cmd() {
        return cmd(DOMAIN);
    }

    private BusinessKeyRotateCmd cmd(String domain) {
        return BusinessKeyRotateCmd.builder()
                .requestId(REQUEST_ID)
                .deviceSn(DEVICE_SN)
                .deviceCategory("TBOX")
                .businessDomain(domain)
                .purpose(PURPOSE)
                .build();
    }

    private DeviceBusinessKey oldActive(String materialModel) {
        return DeviceBusinessKey.builder()
                .id(1L)
                .keyId(OLD_KEY_ID)
                .kmsKeyRef("kms/ref/" + OLD_KEY_ID)
                .businessKeyVersion(1L)
                .keyState(BusinessKeyState.ACTIVE)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .deviceSn(DEVICE_SN)
                .materialModel(materialModel)
                .rowVersion(5)
                .build();
    }

    private DeviceBusinessKey newRow() {
        return DeviceBusinessKey.builder()
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

    /** 通用桩：身份/授权/旧 ACTIVE/新版本占位/派生封装。 */
    private void stubCommonForDerived(BizType bizType, DeviceBusinessKey old) {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(bizType);
        when(materialRouter.isDerivedRoot(bizType)).thenReturn(true);
        when(materialRouter.materialModel(bizType)).thenReturn(KeyMaterialModel.DERIVED_ROOT);
        when(materialRouter.requireDerivedRoot(bizType)).thenReturn(derivedTemplate);
        // CR-066：新行材料回填（attachDerivedMaterial 成功）
        lenient().when(repository.attachDerivedMaterial(any(DeviceBusinessKey.class))).thenReturn(1);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(old);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            k.setRowVersion(1);
            newRow = k;
            return 1;
        });
        lenient().when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
    }

    @Test
    @DisplayName("根派生轮换成功：版本递增、旧 DEPRECATED、新 ACTIVE、ROTATED 事件 + 缓存失效")
    void rotate_derivedRoot_success() {
        DeviceBusinessKey old = oldActive(KeyMaterialModel.DERIVED_ROOT.name());
        stubCommonForDerived(BizType.TBOX_DEVICE_ROOT, old);
        when(activeDeviceCertificateResolver.resolveActive(identity))
                .thenReturn(new ActiveDeviceCertificate("CR061-CERT-0001", "PKI-REQ-ROT-1", "a".repeat(64)));
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(KeyprovEnvelopeTestFixtures.derivedWrappedKey(NEW_KEY_ID,
                        net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD,
                        3L, 3L, "v1", "OP-1"));
        when(repository.selectById(1L)).thenReturn(old);
        when(repository.selectById(2L)).thenAnswer(inv -> newRow);

        BusinessKeyRotationResult result = service.rotate(cmd());

        assertEquals(NEW_KEY_ID, result.getNewKeyId());
        assertEquals(2L, result.getNewBusinessKeyVersion());
        assertEquals(OLD_KEY_ID, result.getOldKeyId());

        ArgumentCaptor<DeviceBusinessKey> captor = ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeast(2)).update(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(k -> OLD_KEY_ID.equals(k.getKeyId())
                && k.getKeyState() == BusinessKeyState.DEPRECATED), "旧 ACTIVE 应转 DEPRECATED");
        // 新行回填派生非敏感字段
        assertTrue(captor.getAllValues().stream().anyMatch(k -> k.getId() != null && k.getId().equals(2L)
                && NEW_KEY_ID.equals(k.getKeyId())
                && k.getRootKeyVersion() != null && k.getRootKeyVersion() == 3L
                && "v1".equals(k.getDerivationVersion())
                && "OP-1".equals(k.getOperationId())
                && k.getKeyState() == BusinessKeyState.ACTIVE));
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("ROTATED"));
        verify(cacheInvalidator).invalidateKey(OLD_KEY_ID);

        // DEVICE 锚 → UID(deviceSn)；收件携带 serial + pkiRequestId 直接证据
        ArgumentCaptor<KeyAnchor> anchorCaptor = ArgumentCaptor.forClass(KeyAnchor.class);
        ArgumentCaptor<KeyPurpose> purposeCaptor = ArgumentCaptor.forClass(KeyPurpose.class);
        ArgumentCaptor<RecipientRef> recipientCaptor = ArgumentCaptor.forClass(RecipientRef.class);
        verify(derivedTemplate).deriveAndWrap(any(), anchorCaptor.capture(), purposeCaptor.capture(),
                recipientCaptor.capture(), any(), any(), any(), any(), any());
        assertEquals(KeyAnchor.AnchorType.UID, anchorCaptor.getValue().type());
        assertEquals(DEVICE_SN, anchorCaptor.getValue().value());
        assertEquals(KeyPurpose.UPLINK_AEAD, purposeCaptor.getValue());
        RecipientRef.DeviceCertificate recipient = (RecipientRef.DeviceCertificate) recipientCaptor.getValue();
        assertEquals("CR061-CERT-0001", recipient.certSerial());
        assertEquals("PKI-REQ-ROT-1", recipient.requestId());
    }

    @Test
    @DisplayName("VEHICLE 锚（V2C_COMM_ROOT）使用规范化 VIN 作为派生锚")
    void rotate_derivedRoot_vehicleAnchorUsesNormalizedVin() {
        DeviceBusinessKey old = DeviceBusinessKey.builder()
                .id(1L).keyId(OLD_KEY_ID).kmsKeyRef("kms/ref/" + OLD_KEY_ID)
                .businessKeyVersion(1L).keyState(BusinessKeyState.ACTIVE)
                .businessDomain(V2C_DOMAIN).purpose(PURPOSE).deviceSn(DEVICE_SN)
                .materialModel(KeyMaterialModel.DERIVED_ROOT.name()).rowVersion(5).build();
        BoundDeviceIdentity v2cIdentity = new BoundDeviceIdentity("  hwyztest900000001 ", 10L, 20L, DEVICE_SN,
                "TBOX", "00000000000000000000000000000001", "BOTH");
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(v2cIdentity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", V2C_DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(V2C_DOMAIN)).thenReturn(BizType.V2C_COMM_ROOT);
        when(materialRouter.isDerivedRoot(BizType.V2C_COMM_ROOT)).thenReturn(true);
        when(materialRouter.materialModel(BizType.V2C_COMM_ROOT)).thenReturn(KeyMaterialModel.DERIVED_ROOT);
        when(materialRouter.requireDerivedRoot(BizType.V2C_COMM_ROOT)).thenReturn(derivedTemplate);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, V2C_DOMAIN, PURPOSE)).thenReturn(old);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, V2C_DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            newRow = k;
            return 1;
        });
        when(activeDeviceCertificateResolver.resolveActive(v2cIdentity))
                .thenReturn(new ActiveDeviceCertificate("C", "PKI", "a".repeat(64)));
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(KeyprovEnvelopeTestFixtures.derivedWrappedKey(NEW_KEY_ID,
                        net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD,
                        3L, 3L, "v1", "OP-2"));
        when(repository.selectById(1L)).thenReturn(old);
        when(repository.selectById(2L)).thenAnswer(inv -> newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);
        when(repository.attachDerivedMaterial(any(DeviceBusinessKey.class))).thenReturn(1);

        service.rotate(cmd(V2C_DOMAIN));

        ArgumentCaptor<KeyAnchor> anchorCaptor = ArgumentCaptor.forClass(KeyAnchor.class);
        verify(derivedTemplate).deriveAndWrap(any(), anchorCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertEquals(KeyAnchor.AnchorType.VIN, anchorCaptor.getValue().type());
        assertEquals("HWYZTEST900000001", anchorCaptor.getValue().value(), "锚必须为规范化 VIN");
    }

    @Test
    @DisplayName("无 ACTIVE → BusinessKeyNotExist，不调用 framework")
    void rotate_noActive_notExist() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(materialRouter.isDerivedRoot(BizType.TBOX_DEVICE_ROOT)).thenReturn(true);
        when(materialRouter.materialModel(BizType.TBOX_DEVICE_ROOT)).thenReturn(KeyMaterialModel.DERIVED_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(null);

        assertThrows(BusinessKeyNotExistException.class, () -> service.rotate(cmd()));
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("历史 ACTIVE material_model IS NULL → 拒绝自动轮换（禁猜测）")
    void rotate_legacyNullMaterialModel_rejected() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(materialRouter.isDerivedRoot(BizType.TBOX_DEVICE_ROOT)).thenReturn(true);
        when(materialRouter.materialModel(BizType.TBOX_DEVICE_ROOT)).thenReturn(KeyMaterialModel.DERIVED_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(oldActive(null));

        assertThrows(BusinessKeyDerivationRejectedException.class, () -> service.rotate(cmd()));
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("目录行材料模型与目标域不一致 → 拒绝自动轮换")
    void rotate_modelMismatch_rejected() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(materialRouter.isDerivedRoot(BizType.TBOX_DEVICE_ROOT)).thenReturn(true);
        when(materialRouter.materialModel(BizType.TBOX_DEVICE_ROOT)).thenReturn(KeyMaterialModel.DERIVED_ROOT);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE))
                .thenReturn(oldActive(KeyMaterialModel.RANDOM_MATERIAL.name()));

        assertThrows(BusinessKeyDerivationRejectedException.class, () -> service.rotate(cmd()));
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("材料模型不适用（null，如签名条目）→ 拒绝自动轮换")
    void rotate_notApplicableMaterialModel_rejected() {
        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.IDK_KTS_SIGNING);
        when(materialRouter.isDerivedRoot(BizType.IDK_KTS_SIGNING)).thenReturn(false);
        when(materialRouter.isRandomMaterial(BizType.IDK_KTS_SIGNING)).thenReturn(false);

        assertThrows(BusinessKeyDerivationRejectedException.class, () -> service.rotate(cmd()));
        verify(repository, never()).selectActiveByContextForUpdate(any(), any(), any());
    }

    @Test
    @DisplayName("派生依赖不可用 → KMS 不可用，新行留痕 FAILED，旧 ACTIVE 不降级")
    void rotate_derivationFailure_doesNotDegradeOld() {
        DeviceBusinessKey old = oldActive(KeyMaterialModel.DERIVED_ROOT.name());
        stubCommonForDerived(BizType.TBOX_DEVICE_ROOT, old);
        when(activeDeviceCertificateResolver.resolveActive(identity))
                .thenReturn(new ActiveDeviceCertificate("C", "PKI", "a".repeat(64)));
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new CryptoDependencyUnavailableException("transit down"));
        when(repository.selectById(2L)).thenAnswer(inv -> newRow);

        assertThrows(BusinessKeyKmsUnavailableException.class, () -> service.rotate(cmd()));

        verify(repository, never()).update(argThat(k -> k.getId() != null && k.getId().equals(1L)
                && k.getKeyState() == BusinessKeyState.DEPRECATED));
        ArgumentCaptor<DeviceBusinessKey> captor = ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(k -> k.getId() != null && k.getId().equals(2L)
                && k.getKeyState() == BusinessKeyState.FAILED), "新行应标记 FAILED");
    }

    @Test
    @DisplayName("切换时旧行乐观锁失败 → 轮换冲突")
    void rotate_switchConflict_oldUpdateZeroRows() {
        DeviceBusinessKey old = oldActive(KeyMaterialModel.DERIVED_ROOT.name());
        stubCommonForDerived(BizType.TBOX_DEVICE_ROOT, old);
        when(activeDeviceCertificateResolver.resolveActive(identity))
                .thenReturn(new ActiveDeviceCertificate("C", "PKI", "a".repeat(64)));
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(KeyprovEnvelopeTestFixtures.derivedWrappedKey(NEW_KEY_ID,
                        net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD,
                        3L, 3L, "v1", "OP-3"));
        when(repository.selectById(1L)).thenReturn(old);
        when(repository.update(argThat(k -> k.getId() != null && k.getId().equals(1L)))).thenReturn(0);

        assertThrows(BusinessKeyRotationConflictException.class, () -> service.rotate(cmd()));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL 轮换：保留 create + wrap 验证语义（无隐式回退）")
    void rotate_randomMaterial_success() {
        // framework 无真实 RANDOM_MATERIAL 条目且 BizType 为 final 枚举不可 mock：用真实 BizType 载体 + 路由桩
        BizType randomBizType = BizType.TBOX_DEVICE_ROOT;
        DeviceBusinessKey old = oldActive(KeyMaterialModel.RANDOM_MATERIAL.name());

        when(boundDeviceIdentityResolver.resolveByDeviceSn(DEVICE_SN, "TBOX")).thenReturn(identity);
        when(authorizationPolicy.authorize(BusinessKeyAction.ROTATE, "TBOX", DOMAIN, PURPOSE)).thenReturn(policyEntry);
        when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(randomBizType);
        when(materialRouter.materialModel(randomBizType)).thenReturn(KeyMaterialModel.RANDOM_MATERIAL);
        when(materialRouter.isRandomMaterial(randomBizType)).thenReturn(true);
        when(materialRouter.requireRandomMaterial(randomBizType)).thenReturn(randomTemplate);
        when(repository.selectActiveByContextForUpdate(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(old);
        when(repository.maxBusinessKeyVersion(DEVICE_SN, DOMAIN, PURPOSE)).thenReturn(1L);
        when(repository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey k = inv.getArgument(0);
            k.setId(2L);
            newRow = k;
            return 1;
        });
        when(activeDeviceCertificateResolver.resolveActive(identity))
                .thenReturn(new ActiveDeviceCertificate("C", "PKI", "a".repeat(64)));
        when(randomTemplate.create(any(BusinessKeyCreateRequest.class)))
                .thenReturn(new BusinessKeyMaterial(NEW_KEY_ID, "kms/ref/" + NEW_KEY_ID, 1, "kms", "AES", "AES-256-GCM",
                        Instant.now().minusSeconds(1), Instant.now().plusSeconds(31536000)));
        when(randomTemplate.wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class)))
                .thenReturn(new WrappedBusinessKey(new byte[]{1}, NEW_KEY_ID, 1, "AES",
                        Instant.now().plusSeconds(60), Map.of()));
        when(repository.selectById(1L)).thenReturn(old);
        when(repository.selectById(2L)).thenAnswer(inv -> newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        BusinessKeyRotationResult result = service.rotate(cmd());

        assertEquals(NEW_KEY_ID, result.getNewKeyId());
        verify(randomTemplate).create(any(BusinessKeyCreateRequest.class));
        verify(randomTemplate).wrap(any(BusinessKeyRef.class), any(RecipientRef.class), any(WrapContext.class));
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(publisher).publish(any(DeviceBusinessKey.class), eq("ROTATED"));
    }

    @Test
    @DisplayName("CR-066：轮换 Provider 返回与 ACTIVE 相同 keyId → 未发生真实代际变化，拒绝激活新行")
    void rotate_sameKeyIdAsActive_rejected() {
        DeviceBusinessKey old = oldActive(KeyMaterialModel.DERIVED_ROOT.name());
        stubCommonForDerived(BizType.TBOX_DEVICE_ROOT, old);
        when(activeDeviceCertificateResolver.resolveActive(identity))
                .thenReturn(new ActiveDeviceCertificate("C", "PKI", "a".repeat(64)));
        // Provider 返回与当前 ACTIVE 相同的 keyId（rootKeyVersion/derivationVersion/anchor 未变）
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(KeyprovEnvelopeTestFixtures.derivedWrappedKey(OLD_KEY_ID,
                        net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD,
                        3L, 3L, "v1", "OP-SAME"));
        when(repository.selectById(2L)).thenAnswer(inv -> newRow);
        when(repository.update(any(DeviceBusinessKey.class))).thenReturn(1);

        assertThrows(BusinessKeyRotationConflictException.class, () -> service.rotate(cmd()));

        // 新行不得激活（留痕 FAILED），旧 ACTIVE 不降级
        ArgumentCaptor<DeviceBusinessKey> captor = ArgumentCaptor.forClass(DeviceBusinessKey.class);
        verify(repository, atLeastOnce()).update(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(k -> k.getId() != null && k.getId().equals(2L)
                        && k.getKeyState() == BusinessKeyState.FAILED),
                "新行应标记 FAILED（未发生真实代际变化）");
        assertTrue(captor.getAllValues().stream().noneMatch(k -> k.getId() != null && k.getId().equals(2L)
                        && k.getKeyState() == BusinessKeyState.ACTIVE),
                "不得激活新行");
        verify(publisher, never()).publish(any(DeviceBusinessKey.class), eq("ROTATED"));
    }
}
