package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
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
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private BusinessKeyProvisioningBatchAppService service;

    private final Map<Long, DeviceBusinessKey> keyStore = new HashMap<>();
    private final List<BusinessKeyProvisioningItem> itemStore = new ArrayList<>();
    private final AtomicLong keyIdSeq = new AtomicLong(1);
    private BusinessKeyProvisioning batchRef;

    private BoundDeviceIdentity identity;
    private BusinessKeyPolicyEntry policyEntry;

    @BeforeEach
    void setUp() {
        service = new BusinessKeyProvisioningBatchAppService(deviceBusinessKeyRepository, provisioningRepository,
                provisioningItemRepository, boundDeviceIdentityResolver, activeDeviceCertificateResolver,
                authorizationPolicy, policyRegistry, bizTypeMapper, materialRouter,
                new BusinessKeyProvisioningDomainService(), transactionManager);
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
        lenient().when(provisioningItemRepository.selectByProvisioningIdAndPurpose(any(), any())).thenReturn(null);

        lenient().when(policyRegistry.resolvePurposes(DOMAIN)).thenReturn(java.util.Optional.of(KeyprovConstants.V2C_PURPOSE_SET));
        lenient().when(bizTypeMapper.resolveBizType(DOMAIN)).thenReturn(BizType.V2C_COMM_ROOT);
        lenient().when(materialRouter.requireDerivedRoot(any())).thenReturn(derivedTemplate);

        identity = new BoundDeviceIdentity(VIN, 10L, 20L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        policyEntry = new BusinessKeyPolicyEntry(DOMAIN, "UPLINK_AEAD", Set.of("TBOX"),
                Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                Duration.ofDays(365), Duration.ofDays(7), "v1");
        lenient().when(authorizationPolicy.authorize(eq(BusinessKeyAction.PROVISION), any(), eq(DOMAIN), any()))
                .thenReturn(policyEntry);
        lenient().when(activeDeviceCertificateResolver.resolveActive(any()))
                .thenReturn(new ActiveDeviceCertificate(CERT_SN, "PKI-REQ-1", CERT_FINGERPRINT));

        lenient().when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    KeyPurpose purpose = inv.getArgument(2);
                    String name = purpose == null ? "NULL" : purpose.name();
                    return new DerivedWrappedKey("KEY-" + name, "kms/ref/" + name, 3, "v1",
                            new byte[]{1, 2, 3}, "RSA-OAEP-SHA256", new byte[]{9}, inv.getArgument(5));
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
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_HPKE_BASE_X25519_SHA256_AES128GCM)
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
        assertEquals(0, itemStore.size(), "派生前失败不落 item");
        assertEquals(1, keyStore.size(), "应保留本次请求占位的目标版本");
        assertTrue(keyStore.values().stream().allMatch(k -> k.getKeyState() == BusinessKeyState.PENDING),
                "可重试依赖失败不得把目标版本置 FAILED（重试仍复用该版本）");
    }

    @Test
    @DisplayName("依赖不可用后以原 requestId 重试：复用预留版本 + 同 operationId 重派生并推进 WAIT_DEVICE_ACK")
    void provision_dependencyUnavailable_thenRetrySameRequestId_succeeds() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        AtomicInteger attempts = new AtomicInteger();
        when(derivedTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            if (attempts.getAndIncrement() == 0) {
                throw new CryptoDependencyUnavailableException(
                        "derive and wrap derived key unavailable (request not sent)");
            }
            KeyPurpose purpose = inv.getArgument(2);
            return new DerivedWrappedKey("KEY-" + purpose.name(), "kms/ref/" + purpose.name(), 3, "v1",
                    new byte[]{1, 2, 3}, "RSA-OAEP-SHA256", new byte[]{9}, inv.getArgument(5));
        });

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
        assertEquals(0, itemStore.size(), "不得把新 requestId 的 item 聚合到旧批次");
        verify(derivedTemplate, times(1)).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("framework 结果未知 → 可重试对账，批次 RECONCILE_REQUIRED")
    void provision_outcomeUnknown_reconcile() {
        when(boundDeviceIdentityResolver.resolveByHsmUid(HSM_UID, null)).thenReturn(identity);
        doThrow(new CryptoOperationOutcomeUnknownException("timeout"))
                .when(derivedTemplate).deriveAndWrap(any(), any(), any(), any(), any(), any(), any());

        KeyProvisioningResponse response = service.provision(request(), ctx());

        assertEquals(KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE, response.getResult());
        assertEquals(KeyprovConstants.ERR_UNAVAILABLE, response.getError().getCode());
        assertEquals(BusinessKeyProvisioningState.RECONCILE_REQUIRED, batchRef.getStatus());
    }
}
