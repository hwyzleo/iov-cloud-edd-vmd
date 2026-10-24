package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.codec.CanonicalDerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import org.springframework.beans.factory.ObjectProvider;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateMaterialUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyAnchor;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RecipientRef;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyProvisioningBatchAppService 单元测试（VMD-DSN-CR-061 根派生改造）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyProvisioningBatchAppServiceTest {

    private static final String REQUEST_ID = "V2C-REQ-0001";
    private static final String DOMAIN = "V2C_COMM";
    private static final String DEVICE_SN = "CR061-TBOX-0001";
    private static final String HSM_UID = "CR061-UID-000001";
    private static final String VIN = "HWYZTEST000000001";
    private static final String CERT_SN = "CR061-CERT-0001";
    private static final String CERT_FINGERPRINT = "f".repeat(64);

    @Mock
    private DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    @Mock
    private BusinessKeyProvisioningRepository provisioningRepository;
    @Mock
    private BusinessKeyProvisioningItemRepository provisioningItemRepository;
    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    @Mock
    private ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    @Mock
    private BusinessKeyAuthorizationPolicy authorizationPolicy;
    @Mock
    private BusinessKeyPolicyRegistry policyRegistry;
    @Mock
    private BusinessKeyBizTypeMapper bizTypeMapper;
    @Mock
    private BusinessKeyMaterialRouter materialRouter;
    @Mock
    private KeyProvisioningTemplate derivedTemplate;
    @Mock
    private TboxEcdhCertificateEligibilityGate tboxEcdhCertificateEligibilityGate;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private BusinessKeyProvisioningBatchAppService service;
    private KeyprovEnvelopeAdapter envelopeAdapter;
    private BusinessKeyProvisioningMetrics metrics;

    private final Map<Long, DeviceBusinessKey> keyStore = new HashMap<>();
    private final List<BusinessKeyProvisioningItem> itemStore = new ArrayList<>();
    private final AtomicLong keyIdSeq = new AtomicLong(1);
    private BusinessKeyProvisioning batchRef;

    private BoundDeviceIdentity identity;
    private BusinessKeyPolicyEntry policyEntry;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<DerivedEnvelopeDigestVerifier> verifierProvider = mock(ObjectProvider.class);
        lenient().when(verifierProvider.getIfAvailable())
                .thenReturn(CanonicalDerivedEnvelopeDigestVerifier.instance());
        envelopeAdapter = new KeyprovEnvelopeAdapter(verifierProvider);
        LogicalKeyGenerationPolicy generationPolicy = new LogicalKeyGenerationPolicy();
        metrics = mock(BusinessKeyProvisioningMetrics.class);
        BusinessKeyDerivedMaterialService derivedMaterialService = new BusinessKeyDerivedMaterialService(
                deviceBusinessKeyRepository, provisioningItemRepository, generationPolicy, metrics);
        service = new BusinessKeyProvisioningBatchAppService(deviceBusinessKeyRepository, provisioningRepository,
                provisioningItemRepository, boundDeviceIdentityResolver, activeDeviceCertificateResolver,
                authorizationPolicy, policyRegistry, bizTypeMapper, materialRouter,
                new BusinessKeyProvisioningDomainService(), envelopeAdapter, tboxEcdhCertificateEligibilityGate,
                derivedMaterialService, metrics, transactionManager);
        ReflectionTestUtils.setField(service, "bundleTtlSeconds", 3600L);
        ReflectionTestUtils.setField(service, "derivationVersion", "v1");

        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        lenient().when(deviceBusinessKeyRepository.insert(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey row = inv.getArgument(0);
            row.setId(keyIdSeq.getAndIncrement());
            keyStore.put(row.getId(), row);
            return 1;
        });
        lenient().when(deviceBusinessKeyRepository.update(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey row = inv.getArgument(0);
            keyStore.put(row.getId(), row);
            return 1;
        });
        // CR-066：attachDerivedMaterial 仅回填 PENDING 占位行（key_id IS NULL），模拟真实 SQL 语义
        lenient().when(deviceBusinessKeyRepository.attachDerivedMaterial(any(DeviceBusinessKey.class))).thenAnswer(inv -> {
            DeviceBusinessKey attach = inv.getArgument(0);
            DeviceBusinessKey current = keyStore.get(attach.getId());
            if (current != null && current.getKeyState() == BusinessKeyState.PENDING && current.getKeyId() == null) {
                current.setKeyId(attach.getKeyId());
                current.setKmsKeyRef(attach.getKmsKeyRef());
                current.setRootKeyVersion(attach.getRootKeyVersion());
                current.setDerivationVersion(attach.getDerivationVersion());
                current.setOperationId(attach.getOperationId());
                current.setAlgorithm(attach.getAlgorithm());
                current.setKeySpec(attach.getKeySpec());
                current.setLogicalIdentityDigest(attach.getLogicalIdentityDigest());
                keyStore.put(attach.getId(), current);
                return 1;
            }
            return 0;
        });
        // CR-066：selectByRequestIdAndPurpose 按请求+用途定位预留行
        lenient().when(deviceBusinessKeyRepository.selectByKeyId(any()))
                .thenAnswer(inv -> keyStore.values().stream()
                        .filter(k -> inv.getArgument(0).equals(k.getKeyId())).findFirst().orElse(null));
        lenient().when(deviceBusinessKeyRepository.selectByKeyIdForUpdate(any()))
                .thenAnswer(inv -> keyStore.values().stream()
                        .filter(k -> inv.getArgument(0).equals(k.getKeyId())).findFirst().orElse(null));
        lenient().when(deviceBusinessKeyRepository.selectById(any())).thenAnswer(inv -> keyStore.get(inv.getArgument(0)));
        lenient().when(deviceBusinessKeyRepository.maxBusinessKeyVersion(any(), any(), any())).thenReturn(0L);
        lenient().when(deviceBusinessKeyRepository.selectActiveByContext(any(), any(), any())).thenReturn(null);
        lenient().when(deviceBusinessKeyRepository.selectByRequestIdAndPurpose(any(), any())).thenAnswer(inv -> {
            String reqId = inv.getArgument(0);
            String purpose = inv.getArgument(1);
            return keyStore.values().stream()
                    .filter(k -> reqId.equals(k.getRequestId()) && purpose.equals(k.getPurpose()))
                    .findFirst().orElse(null);
        });

        lenient().when(provisioningRepository.insert(any(BusinessKeyProvisioning.class))).thenAnswer(inv -> {
            BusinessKeyProvisioning batch = inv.getArgument(0);
            batch.setId(50L);
            batchRef = batch;
            return 1;
        });
        lenient().when(provisioningRepository.update(any())).thenReturn(1);
        lenient().when(provisioningRepository.selectById(any())).thenAnswer(inv -> batchRef);
        lenient().when(provisioningRepository.selectByRequestId(any())).thenAnswer(inv -> {
            String reqId = inv.getArgument(0);
            return batchRef != null && reqId.equals(batchRef.getRequestId()) ? batchRef : null;
        });

        lenient().when(provisioningItemRepository.insert(any(BusinessKeyProvisioningItem.class))).thenAnswer(inv -> {
            BusinessKeyProvisioningItem item = inv.getArgument(0);
            item.setId((long) (itemStore.size() + 1));
            itemStore.add(item);
            return 1;
        });
        lenient().when(provisioningItemRepository.selectByProvisioningId(any()))
                .thenAnswer(inv -> new ArrayList<>(itemStore));
        lenient().when(provisioningItemRepository.selectByProvisioningIdAndPurpose(any(), any())).thenAnswer(inv -> {
            Long pid = inv.getArgument(0);
            String purpose = inv.getArgument(1);
            return itemStore.stream()
                    .filter(i -> pid.equals(i.getProvisioningId()) && purpose.equals(i.getPurpose()))
                    .findFirst().orElse(null);
        });
        lenient().when(provisioningItemRepository.update(any(BusinessKeyProvisioningItem.class))).thenReturn(1);
        lenient().when(provisioningItemRepository.rebindDirectoryKey(any(), any(), any(), any())).thenReturn(1);
        lenient().when(deviceBusinessKeyRepository.markPlaceholderSuperseded(any(), any(), any())).thenReturn(1);
        lenient().when(deviceBusinessKeyRepository.markPendingRecovery(any(), any())).thenReturn(1);
        lenient().when(deviceBusinessKeyRepository.confirmRecovery(any(), any())).thenReturn(1);

        lenient().when(policyRegistry.resolvePurposes(DOMAIN)).thenReturn(java.util.Optional.of(KeyprovConstants.V2C_PURPOSE_SET));
        lenient().when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.V2C_COMM_ROOT);
        lenient().when(materialRouter.requireDerivedRoot(any())).thenReturn(derivedTemplate);

        identity = new BoundDeviceIdentity(VIN, 10L, 20L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        policyEntry = new BusinessKeyPolicyEntry(DOMAIN, "UPLINK_AEAD", Set.of("TBOX"),
                Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                Duration.ofDays(365), Duration.ofDays(7), "v1");
        lenient().when(authorizationPolicy.authorize(eq(BusinessKeyAction.PROVISION), any(), eq(DOMAIN), any()))
                .thenReturn(policyEntry);
        lenient().when(activeDeviceCertificateResolver.resolveActiveEcdhV1(any()))
                .thenReturn(new ActiveDeviceCertificate(CERT_SN, "PKI-REQ-1", CERT_FINGERPRINT));
        lenient().when(tboxEcdhCertificateEligibilityGate.evaluate(any()))
                .thenReturn(new TboxEcdhCertificateEligibilityGate.GateResult(true, false, "ok"));

        lenient().when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
                    String name = purpose == null ? "NULL" : purpose.name();
                    net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
                    return KeyprovEnvelopeTestFixtures.derivedWrappedKey("KEY-" + name, purpose,
                            context.businessKeyVersion(), 3L, "v1", inv.getArgument(5));
                });
    }

    private KeyprovAuthenticatedContext ctx() {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", HSM_UID, "client-1",
                CERT_SN, null, 1, "session-1", System.currentTimeMillis());
    }

    private KeyProvisioningRequest request() {
        return request(REQUEST_ID);
    }

    private KeyProvisioningRequest request(String requestId) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .setTimestampMs(System.currentTimeMillis())
                .build();
    }

    @Test
    @DisplayName("两用途根派生成功：显式 VIN 锚 + 非敏感字段落库，响应含两 Wrapped Key")
    void provision_derivesBothPurposes() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertEquals(2, response.getItemsCount(), "V2C 应下发两个用途");
        for (var item : response.getItemsList()) {
            assertFalse(item.getWrappedKey().isEmpty(), "应携带 Wrapped Key");
        }

        // deriveAndWrap 证据：BizType/anchor(VIN)/purpose/recipient
        ArgumentCaptor<BizType> bizTypeCaptor = ArgumentCaptor.forClass(BizType.class);
        ArgumentCaptor<KeyAnchor> anchorCaptor = ArgumentCaptor.forClass(KeyAnchor.class);
        ArgumentCaptor<KeyPurpose> purposeCaptor = ArgumentCaptor.forClass(KeyPurpose.class);
        ArgumentCaptor<RecipientRef> recipientCaptor = ArgumentCaptor.forClass(RecipientRef.class);
        ArgumentCaptor<DerivationContext> ctxCaptor = ArgumentCaptor.forClass(DerivationContext.class);
        ArgumentCaptor<String> opIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idemCaptor = ArgumentCaptor.forClass(String.class);
        verify(derivedTemplate, times(2)).deriveAndWrap(bizTypeCaptor.capture(), anchorCaptor.capture(),
                purposeCaptor.capture(), recipientCaptor.capture(), ctxCaptor.capture(),
                opIdCaptor.capture(), idemCaptor.capture());

        assertTrue(bizTypeCaptor.getAllValues().stream().allMatch(b -> b == BizType.V2C_COMM_ROOT));
        assertTrue(anchorCaptor.getAllValues().stream().allMatch(a -> a.type() == KeyAnchor.AnchorType.VIN
                && VIN.equals(a.value())), "锚必须是规范化 VIN");
        assertEquals(Set.of(KeyPurpose.UPLINK_AEAD, KeyPurpose.DOWNLINK_AEAD),
                Set.copyOf(purposeCaptor.getAllValues()), "两 purpose 必须域隔离");
        assertTrue(ctxCaptor.getAllValues().stream()
                .allMatch(c -> DerivationContext.V1.equals(c.derivationVersion())));
        assertTrue(opIdCaptor.getAllValues().stream().allMatch(v -> v != null && !v.isBlank()));
        assertTrue(idemCaptor.getAllValues().stream().allMatch(v -> v != null && !v.isBlank()));
        for (RecipientRef ref : recipientCaptor.getAllValues()) {
            RecipientRef.DeviceCertificate cert = assertInstanceOf(RecipientRef.DeviceCertificate.class, ref);
            assertEquals(CERT_SN, cert.certSerial());
        }

        // 落库：两 PENDING 行，materialModel/版本/operationId 齐备，无敏感材料
        assertEquals(2, keyStore.size(), "应为两用途各落一行目标版本");
        for (DeviceBusinessKey row : keyStore.values()) {
            assertEquals(BusinessKeyState.PENDING, row.getKeyState());
            assertEquals("DERIVED_ROOT", row.getMaterialModel());
            assertNotNull(row.getKeyId());
            assertEquals(3L, row.getRootKeyVersion());
            assertEquals("v1", row.getDerivationVersion());
            assertNotNull(row.getOperationId());
        }
        // item 证据：operationId/derivationVersion/收件指纹摘要（非证书本体）
        assertEquals(2, itemStore.size());
        for (BusinessKeyProvisioningItem item : itemStore) {
            assertNotNull(item.getOperationId());
            assertEquals("v1", item.getDerivationVersion());
            assertEquals(3L, item.getRootKeyVersion());
            assertNotNull(item.getRecipientCertFingerprintDigest());
            assertNull(item.getImportProofDigest(), "Request 阶段不得记导入证明");
        }
        assertEquals(50L, batchRef.getId());
        assertEquals(net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState.WAIT_DEVICE_ACK,
                batchRef.getStatus());
    }

    @Test
    @DisplayName("VIN 非法 → 在调用 framework 前 fail-closed，0 次派生且不落库")
    void provision_invalidVin_failsClosedBeforeFramework() {
        BoundDeviceIdentity badIdentity = new BoundDeviceIdentity("BAD-VIN", 10L, 20L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(badIdentity);

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_VIN_BINDING_INVALID, response.getError().getCode());
        assertFalse(response.getError().getMessage().contains("BAD-VIN"), "错误正文不得回显 VIN 原文");
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
        verify(deviceBusinessKeyRepository, never()).insert(any());
        verify(provisioningRepository, never()).insert(any());
    }

    @Test
    @DisplayName("历史 ACTIVE material_model IS NULL → 拒绝自动重建（禁猜测）")
    void provision_legacyActiveNullMaterialModel_rejected() {
        DeviceBusinessKey legacy = DeviceBusinessKey.builder()
                .id(7L)
                .deviceSn(DEVICE_SN)
                .businessDomain(DOMAIN)
                .purpose("UPLINK_AEAD")
                .businessKeyVersion(1L)
                .keyState(BusinessKeyState.ACTIVE)
                .materialModel(null)
                .build();
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        when(deviceBusinessKeyRepository.selectActiveByContext(eq(DEVICE_SN), eq(DOMAIN), any())).thenReturn(legacy);

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, response.getError().getCode());
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("材料模型不匹配（非 DERIVED_ROOT）→ 路由拒绝 806108 语义")
    void provision_materialModelMismatch_rejected() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        when(materialRouter.requireDerivedRoot(any()))
                .thenThrow(new BusinessKeyDerivationRejectedException("非根派生型"));

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_DERIVATION_UNSUPPORTED, response.getError().getCode());
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("根派生 Provider 未就绪 → 可重试 806107，批次保持 PREPARING（不终态化、不降级）")
    void provision_providerNotReady_retryable() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new BusinessKeyDerivedProviderUnavailableException("Transit root 未就绪"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, response.getResult());
        assertEquals(KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY, response.getError().getCode());
        assertTrue(response.getError().getRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", response.getError().getAction());
        // 关键：retryable=true 必须与批次状态自洽——批次不可终态化，否则同 requestId 重试被拒（806094）
        assertEquals(BusinessKeyProvisioningState.PREPARING, batchRef.getStatus());
        assertNotNull(batchRef.getFailReason(), "应记录依赖不可用原因供诊断/对账");
        assertEquals(1, itemStore.size(), "Provider 调用前已持久化 item 身份（CR-066 §6.2）");
        assertEquals(1, keyStore.size(), "应保留本次请求占位的目标版本");
        assertTrue(keyStore.values().stream().allMatch(k -> k.getKeyState() == BusinessKeyState.PENDING),
                "可重试依赖失败不得把目标版本置 FAILED（重试仍复用该版本）");
    }

    @Test
    @DisplayName("证书证据冲突 → 上抛交门面映射 806102（不再降级为 806108），批次终态化 FAILED")
    void provision_certificateEvidenceMismatch_propagates_andFailsBatch() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new CertificateEvidenceMismatchException(
                "Resolved certificate serial does not match the requested certSerial"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        // 关键：不得被 catch(CryptoException) 吞成 806108 协议响应，必须上抛由门面映射 806102/DO_NOT_RETRY
        assertThrows(CertificateEvidenceMismatchException.class, () -> service.provision(request(), ctx()));

        assertEquals(BusinessKeyProvisioningState.FAILED, batchRef.getStatus(),
                "不可重试的证书证据冲突应终态化批次");
        assertTrue(keyStore.values().stream().allMatch(k -> k.getKeyState() == BusinessKeyState.FAILED),
                "本批次占位 PENDING 目标版本应置 FAILED，避免遗留孤儿");
        assertEquals(1, itemStore.size(), "派生前已持久化 item 身份，失败后应标记 FAILED");
        assertEquals(ProvisioningDeviceResult.FAILED, itemStore.get(0).getDeviceResult());
    }

    @Test
    @DisplayName("证书材料暂不可取 → 上抛交门面映射 806101（不再降级为 806108），批次保持 PREPARING 可原 requestId 重试")
    void provision_certificateMaterialUnavailable_propagates_keepsBatchOpen() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new CertificateMaterialUnavailableException("pkiRequestId locator 缺失"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        assertThrows(CertificateMaterialUnavailableException.class, () -> service.provision(request(), ctx()));

        assertEquals(BusinessKeyProvisioningState.PREPARING, batchRef.getStatus(),
                "可重试的证书材料缺失不得终态化批次（否则同 requestId 重试被拒 806094）");
        assertTrue(keyStore.values().stream().allMatch(k -> k.getKeyState() == BusinessKeyState.PENDING),
                "可重试失败不得把目标版本置 FAILED（重试仍复用该版本）");
        assertEquals(1, itemStore.size(), "派生前已持久化 item 身份，可重试失败保留 PENDING");
    }

    @Test
    @DisplayName("依赖不可用后以原 requestId 重试：复用预留版本 + 同 operationId 重派生并推进 WAIT_DEVICE_ACK")
    void provision_dependencyUnavailable_thenRetrySameRequestId_succeeds() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(inv -> {
            if (attempts.getAndIncrement() == 0) {
                throw new CryptoDependencyUnavailableException(
                        "derive and wrap derived key unavailable (request not sent)");
            }
            KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey("KEY-" + purpose.name(), purpose,
                    context.businessKeyVersion(), 3L, "v1", inv.getArgument(5));
        }).when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        KeyProvisioningRequest request = request();
        KeyProvisioningResponse first = service.provision(request, ctx());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, first.getResult());
        assertEquals(BusinessKeyProvisioningState.PREPARING, batchRef.getStatus());

        KeyProvisioningResponse second = service.provision(request, ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, second.getResult());
        assertEquals(2, second.getItemsCount(), "重试应完成两用途下发");
        assertEquals(BusinessKeyProvisioningState.WAIT_DEVICE_ACK, batchRef.getStatus());
        assertNull(batchRef.getFailReason(), "推进成功后应清除可重试失败原因");
        assertEquals(2, keyStore.size(), "重试应复用预留版本，不新建版本、不遗留孤儿 PENDING");
        assertTrue(keyStore.values().stream()
                        .allMatch(k -> k.getKeyState() == BusinessKeyState.PENDING && k.getKeyId() != null),
                "重试补全预留版本材料并保持 PENDING");
        assertEquals(2, itemStore.size());

        // 同 requestId + 同版本 → operationId 必须稳定（Provider 幂等命中同一逻辑材料）
        ArgumentCaptor<KeyPurpose> purposeCaptor = ArgumentCaptor.forClass(KeyPurpose.class);
        ArgumentCaptor<String> opIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(derivedTemplate, times(3)).deriveAndWrap(any(), any(), purposeCaptor.capture(), any(), any(),
                opIdCaptor.capture(), any());
        Map<KeyPurpose, Set<String>> opIdsByPurpose = new HashMap<>();
        for (int i = 0; i < purposeCaptor.getAllValues().size(); i++) {
            opIdsByPurpose.computeIfAbsent(purposeCaptor.getAllValues().get(i), k -> new HashSet<>())
                    .add(opIdCaptor.getAllValues().get(i));
        }
        assertEquals(1, opIdsByPurpose.get(KeyPurpose.UPLINK_AEAD).size(),
                "同 requestId 同版本 operationId 必须稳定");
    }

    @Test
    @DisplayName("同设备业务域存在其他 requestId 未终结批次 → 拒绝新 requestId，不复用旧批次")
    void provision_newRequestIdWhileOtherOpenBatchExists_rejected() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new CryptoDependencyUnavailableException("derive and wrap derived key unavailable (request not sent)"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                service.provision(request(), ctx()).getResult(), "首次申请依赖不可用，批次保持未终结");
        BusinessKeyProvisioning open = batchRef;
        assertEquals(BusinessKeyProvisioningState.PREPARING, open.getStatus());

        // 新 requestId 撞唯一未终结批次约束 uk_device_domain_open：insert 冲突 + 查到旧未终结批次
        when(provisioningRepository.insert(any(BusinessKeyProvisioning.class)))
                .thenThrow(new DuplicateKeyException("uk_device_domain_open"));
        when(provisioningRepository.selectOpenByDeviceAndDomain(DEVICE_SN, DOMAIN)).thenReturn(open);

        KeyProvisioningResponse response = service.provision(request("V2C-REQ-0002"), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_STATE_NOT_ALLOWED, response.getError().getCode());
        assertFalse(response.getError().getRetryable());
        assertTrue(response.getError().getMessage().contains("原requestId"), "应引导以原 requestId 继续");
        assertEquals(1, keyStore.size(), "不得为新 requestId 落任何目录版本");
        assertEquals(1, itemStore.size(), "item 身份仅属于首个 requestId，不得聚合到新批次");
        verify(derivedTemplate, times(1)).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CR-067：framework 结果未知（批次已落库）→ ERR_RECONCILE_REQUIRED/QUERY_STATUS_LATER，批次 RECONCILE_REQUIRED")
    void provision_outcomeUnknown_reconcile() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new CryptoOperationOutcomeUnknownException("timeout"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, response.getResult());
        assertEquals(KeyprovConstants.ERR_RECONCILE_REQUIRED, response.getError().getCode());
        assertTrue(response.getError().getRetryable());
        assertEquals("QUERY_STATUS_LATER", response.getError().getAction(),
                "批次已落库只能 Status 对账，不得引导重发原 Request");
        assertEquals(BusinessKeyProvisioningState.RECONCILE_REQUIRED, batchRef.getStatus());
    }

    @Test
    @DisplayName("CR-067：批次前证书门禁可重试拒绝 → ERR_UNAVAILABLE/RETRY_SAME_REQUEST_ID，不落批次、不调用 Provider")
    void provision_gateRetryableRejection_preBatch() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        when(tboxEcdhCertificateEligibilityGate.evaluate(any()))
                .thenReturn(new TboxEcdhCertificateEligibilityGate.GateResult(false, true, "provider-not-ready"));

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, response.getResult());
        assertEquals(KeyprovConstants.ERR_UNAVAILABLE, response.getError().getCode());
        assertTrue(response.getError().getRetryable());
        assertEquals("RETRY_SAME_REQUEST_ID", response.getError().getAction(),
                "批次前暂不可用必须以原 requestId 重发 Request，不得转为 Status");
        // §8.2：gate DOWN 时 Repository createBatch 次数为 0、Provider 调用次数为 0
        assertNull(batchRef, "批次前失败不得创建批次");
        verify(provisioningRepository, never()).insert(any(BusinessKeyProvisioning.class));
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CR-067 §4.1c：ERR_RECONCILE_REQUIRED 若被错误配为 RETRY_SAME_REQUEST_ID 则四元组断言 fail-fast")
    void provision_reconcileRequiredWrongAction_failsFast() {
        // 防御性验证：validateRecoveryTuple 不允许 ERR_RECONCILE_REQUIRED 携带 RETRY_SAME_REQUEST_ID
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "responseWithError",
                        REQUEST_ID, KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                        KeyprovConstants.ERR_RECONCILE_REQUIRED, true, "RETRY_SAME_REQUEST_ID", "x"));
        // 反向组合（ERR_UNAVAILABLE 可重试却配 QUERY_STATUS_LATER）同样 fail-fast
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "responseWithError",
                        REQUEST_ID, KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE,
                        KeyprovConstants.ERR_UNAVAILABLE, true, "QUERY_STATUS_LATER", "x"));
    }

    // ================= CR-066 claim-or-reuse / 防死循环 / 版本代际 =================

    private DeviceBusinessKey completeMaterialRow(Long id, String purpose, Long version, String keyId,
                                                  BusinessKeyState state, String requestId) {
        return DeviceBusinessKey.builder()
                .id(id).deviceSn(DEVICE_SN).bindingId(10L).partId(20L).hsmUid(HSM_UID)
                .businessDomain(DOMAIN).purpose(purpose).materialModel("DERIVED_ROOT")
                .keyId(keyId).businessKeyVersion(version).kmsKeyRef("kms/ref/" + keyId)
                .rootKeyVersion(3L).derivationVersion("v1").algorithm("AES").keySpec("AES-256-GCM")
                .policyVersion("v1").keyState(state).requestId(requestId).rowVersion(1)
                .build();
    }

    private KeyProvisioningRequest requestWithVersions(long upVersion, long downVersion) {
        return KeyProvisioningRequest.newBuilder()
                .setRequestId(REQUEST_ID)
                .setIdempotencyKey("IK-" + REQUEST_ID)
                .setBusinessDomain(DOMAIN)
                .addPurposes(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addCurrentVersions(net.hwyz.iov.security.keyprov.v1.CurrentKeyVersion.newBuilder()
                        .setPurpose(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                        .setBusinessKeyVersion(upVersion))
                .addCurrentVersions(net.hwyz.iov.security.keyprov.v1.CurrentKeyVersion.newBuilder()
                        .setPurpose(net.hwyz.iov.security.keyprov.v1.KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                        .setBusinessKeyVersion(downVersion))
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .setTimestampMs(System.currentTimeMillis())
                .build();
    }

    @Test
    @DisplayName("同 requestId 撞 uk_key_id（既有 FAILED 同材料行）→ claim-or-reuse 恢复收敛 SUCCESS，不落 806100 死循环")
    void provision_duplicateKeyWithFailedExisting_converges() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        // 既有 FAILED 行（前一次失败批次，同逻辑材料证据完整，keyId 确定性相同）
        keyStore.put(100L, completeMaterialRow(100L, "UPLINK_AEAD", 1L, "KEY-UPLINK_AEAD",
                BusinessKeyState.FAILED, "PREV-REQ-1"));
        keyStore.put(200L, completeMaterialRow(200L, "DOWNLINK_AEAD", 1L, "KEY-DOWNLINK_AEAD",
                BusinessKeyState.FAILED, "PREV-REQ-2"));
        lenient().when(deviceBusinessKeyRepository.maxBusinessKeyVersion(any(), any(), any())).thenReturn(1L);
        when(deviceBusinessKeyRepository.attachDerivedMaterial(any()))
                .thenThrow(new DuplicateKeyException("uk_key_id"));
        when(deviceBusinessKeyRepository.markPendingRecovery(any(), any())).thenReturn(1);
        when(deviceBusinessKeyRepository.confirmRecovery(any(), any())).thenReturn(1);

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertEquals(2, response.getItemsCount());
        assertFalse(response.hasError());
        // 恢复并复用既有 FAILED 行，不再新建 canonical 行
        verify(deviceBusinessKeyRepository, atLeastOnce()).markPendingRecovery(any(), any());
        verify(deviceBusinessKeyRepository, atLeastOnce()).confirmRecovery(any(), any());
        verify(deviceBusinessKeyRepository, atLeastOnce()).markPlaceholderSuperseded(any(), any(), any());
        verify(metrics, atLeastOnce()).recordRetryLoopPrevented(eq("claim_converged"));
    }

    @Test
    @DisplayName("同 keyId 但不可变元数据冲突（rootKeyVersion 不一致）→ 806130 不可重试，批次 FAILED")
    void provision_duplicateKeyIdentityConflict_rejectedNonRetryable() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        // 既有 ACTIVE 行 rootKeyVersion=8 与本次结果(3)冲突
        DeviceBusinessKey existing = completeMaterialRow(100L, "UPLINK_AEAD", 1L, "KEY-UPLINK_AEAD",
                BusinessKeyState.ACTIVE, "PREV-REQ-1");
        existing.setRootKeyVersion(8L);
        keyStore.put(100L, existing);
        lenient().when(deviceBusinessKeyRepository.maxBusinessKeyVersion(any(), any(), any())).thenReturn(1L);
        when(deviceBusinessKeyRepository.attachDerivedMaterial(any()))
                .thenThrow(new DuplicateKeyException("uk_key_id"));

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_LOGICAL_KEY_IDENTITY_CONFLICT, response.getError().getCode());
        assertFalse(response.getError().getRetryable());
        assertEquals("DO_NOT_RETRY", response.getError().getAction());
        assertEquals(BusinessKeyProvisioningState.FAILED, batchRef.getStatus(), "不可重试冲突应终态化批次");
        verify(metrics).recordRetryLoopPrevented(eq("identity_conflict_rejected"));
    }

    @Test
    @DisplayName("声明版本与 ACTIVE 不一致但代际相同 → 806131 版本代际冲突，不创建新版本行")
    void provision_declaredVersionMismatch_sameGeneration_rejected() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        DeviceBusinessKey active = completeMaterialRow(1L, "UPLINK_AEAD", 1L, "KEY-UPLINK_AEAD",
                BusinessKeyState.ACTIVE, "PREV-REQ");
        DeviceBusinessKey activeDown = completeMaterialRow(2L, "DOWNLINK_AEAD", 1L, "KEY-DOWNLINK_AEAD",
                BusinessKeyState.ACTIVE, "PREV-REQ");
        keyStore.put(1L, active);
        keyStore.put(2L, activeDown);
        when(deviceBusinessKeyRepository.selectActiveByContext(eq(DEVICE_SN), eq(DOMAIN), any()))
                .thenAnswer(inv -> {
                    String purpose = inv.getArgument(2);
                    return "UPLINK_AEAD".equals(purpose) ? active : activeDown;
                });

        KeyProvisioningResponse response = service.provision(requestWithVersions(2L, 2L), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED, response.getResult());
        assertEquals(KeyprovConstants.ERR_VERSION_GENERATION_MISMATCH, response.getError().getCode());
        assertFalse(response.getError().getRetryable());
        verify(derivedTemplate, never()).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
        // 不创建任何新版本行
        assertEquals(2, keyStore.size());
    }

    @Test
    @DisplayName("换证/换件 REIMPORT：同代际复用 ACTIVE 行，不新建目录行，目录行数不变")
    void provision_reimport_reusesActive_noNewRow() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        DeviceBusinessKey active = completeMaterialRow(1L, "UPLINK_AEAD", 1L, "KEY-UPLINK_AEAD",
                BusinessKeyState.ACTIVE, "PREV-REQ");
        DeviceBusinessKey activeDown = completeMaterialRow(2L, "DOWNLINK_AEAD", 1L, "KEY-DOWNLINK_AEAD",
                BusinessKeyState.ACTIVE, "PREV-REQ");
        keyStore.put(1L, active);
        keyStore.put(2L, activeDown);
        when(deviceBusinessKeyRepository.selectActiveByContext(eq(DEVICE_SN), eq(DOMAIN), any()))
                .thenAnswer(inv -> {
                    String purpose = inv.getArgument(2);
                    return "UPLINK_AEAD".equals(purpose) ? active : activeDown;
                });

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED, response.getResult());
        assertEquals(2, response.getItemsCount());
        assertEquals(2, keyStore.size(), "REIMPORT 不得新建目录行（换证/换件同逻辑材料）");
        assertTrue(itemStore.stream().allMatch(i -> i.getDirectoryKeyId() != null && i.getDirectoryKeyId() <= 2L),
                "item 应复用既有 ACTIVE 行");
        verify(deviceBusinessKeyRepository, never()).markPlaceholderSuperseded(any(), any(), any());
    }
}
