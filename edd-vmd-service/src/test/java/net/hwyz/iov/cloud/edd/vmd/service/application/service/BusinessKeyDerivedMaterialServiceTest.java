package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyLogicalIdentityConflict;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BusinessKeyDerivedMaterialService 单元测试（VMD-DSN-CR-066 §6.3/§11.1）。
 * <p>
 * 覆盖：UPDATE 回填成功、DuplicateKey + 完全一致既有行 → reuse、任一不可变事实冲突 → 不可重试、
 * 并发收敛、FAILED 恢复同一行 / 不可恢复拒绝、占位 SUPERSEDED 证据保留。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyDerivedMaterialServiceTest {

    private static final String KEY_ID = "openiov|V2C_COMM|UPLINK_AEAD|v1|rootV7|sha256:abcdef";
    private static final String KMS_REF = "kms/ref/" + KEY_ID;
    private static final String DEVICE_SN = "SN-1";
    private static final String DOMAIN = "V2C_COMM";
    private static final String PURPOSE = "UPLINK_AEAD";
    private static final long ROOT_VERSION = 7L;
    private static final String DERIVATION_VERSION = "v1";

    @Mock
    private DeviceBusinessKeyRepository repository;
    @Mock
    private BusinessKeyProvisioningItemRepository itemRepository;
    @Mock
    private BusinessKeyProvisioningMetrics metrics;

    private BusinessKeyDerivedMaterialService service;

    @BeforeEach
    void setUp() {
        service = new BusinessKeyDerivedMaterialService(repository, itemRepository,
                new LogicalKeyGenerationPolicy(), metrics);
    }

    private DeviceBusinessKey pendingRow() {
        return DeviceBusinessKey.builder()
                .id(1L).rowVersion(1).deviceSn(DEVICE_SN).businessDomain(DOMAIN).purpose(PURPOSE)
                .materialModel("DERIVED_ROOT").businessKeyVersion(3L).keyState(BusinessKeyState.PENDING)
                .requestId("REQ-1").build();
    }

    private DeviceBusinessKey existingRow(BusinessKeyState state, Long rootVersion, String kmsRef) {
        return DeviceBusinessKey.builder()
                .id(5L).rowVersion(3).deviceSn(DEVICE_SN).businessDomain(DOMAIN).purpose(PURPOSE)
                .materialModel("DERIVED_ROOT").keyId(KEY_ID).kmsKeyRef(kmsRef == null ? KMS_REF : kmsRef)
                .rootKeyVersion(rootVersion).derivationVersion(DERIVATION_VERSION)
                .algorithm("AES").keySpec("AES-256-GCM").policyVersion("v1")
                .businessKeyVersion(1L).keyState(state).build();
    }

    private DerivedWrappedKey result() {
        return KeyprovEnvelopeTestFixtures.derivedWrappedKey(KEY_ID, KeyPurpose.UPLINK_AEAD,
                3L, ROOT_VERSION, DERIVATION_VERSION, "OP-1");
    }

    private BusinessKeyPolicyEntry policy() {
        return new BusinessKeyPolicyEntry(DOMAIN, PURPOSE, Set.of("TBOX"),
                Set.of(BusinessKeyAction.values()), "AES", "AES-256-GCM",
                Duration.ofDays(365), Duration.ofDays(7), "v1");
    }

    private BusinessKeyDerivedMaterialService.ClaimContext ctx() {
        return new BusinessKeyDerivedMaterialService.ClaimContext(
                DEVICE_SN, DOMAIN, PURPOSE, "V2C_COMM_ROOT", "HWYZTEST000000001", "REQ-1");
    }

    @Test
    @DisplayName("UPDATE 回填成功：PENDING 占位行 attachDerivedMaterial，返回本行 id")
    void attach_freshPlaceholder_success() {
        when(repository.attachDerivedMaterial(any(DeviceBusinessKey.class))).thenReturn(1);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(1L, id);
        verify(repository, never()).selectByKeyIdForUpdate(any());
        verify(repository, never()).markPlaceholderSuperseded(any(), any(), any());
        verify(repository).attachDerivedMaterial(argThat(a ->
                KEY_ID.equals(a.getKeyId()) && KMS_REF.equals(a.getKmsKeyRef())
                        && a.getRootKeyVersion() == ROOT_VERSION
                        && a.getLogicalIdentityDigest() != null && a.getLogicalIdentityDigest().length == 32
                        && a.getValidFrom() != null && a.getValidTo() != null
                        && a.getValidTo().isAfter(a.getValidFrom())));
    }

    @Test
    @DisplayName("claim-or-reuse 复用行缺有效期（历史/缺陷期间 FAILED 行）→ 按策略兜底回填 validFrom/validTo")
    void claimOrReuse_missingValidity_backfills() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF); // 无 validFrom/validTo
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(null);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(5L, id);
        verify(repository).update(argThat(a ->
                a.getValidFrom() != null && a.getValidTo() != null
                        && a.getValidTo().isAfter(a.getValidFrom())));
        assertNotNull(existing.getValidFrom());
        assertNotNull(existing.getValidTo());
    }

    @Test
    @DisplayName("复用行已有有效期 → 不重复回填（不触发 update）")
    void claimOrReuse_validityPresent_noBackfill() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF);
        existing.setValidFrom(LocalDateTime.now());
        existing.setValidTo(LocalDateTime.now().plus(Duration.ofDays(365)));
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(null);

        service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        verify(repository, never()).update(any());
    }

    @Test
    @DisplayName("复用行有效期已过期（CR-068：多次复用可恢复行后 valid_to < now）→ 按策略续签，防止车端 SEC 以 WRAPPED_KEY_INVALID 拒绝导入")
    void claimOrReuse_expiredValidity_reissues() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF);
        // 旧行有效期已过期（首次 attach 距今 > policy.validity()）
        existing.setValidFrom(LocalDateTime.now().minus(Duration.ofDays(400)));
        existing.setValidTo(LocalDateTime.now().minus(Duration.ofDays(35)));
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(null);

        service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        verify(repository).update(argThat(a ->
                a.getValidFrom() != null && a.getValidTo() != null
                        && a.getValidTo().isAfter(LocalDateTime.now())
                        && a.getValidTo().isAfter(a.getValidFrom())));
    }

    @Test
    @DisplayName("DuplicateKey + 完全一致既有 ACTIVE 行 → claim-or-reuse：item 重挂 canonical + 占位 SUPERSEDED")
    void duplicateKey_identicalExisting_reuse() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF);
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(null);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(5L, id);
        verify(itemRepository).rebindDirectoryKey(100L, 5L, KEY_ID, 1L);
        verify(repository).markPlaceholderSuperseded(1L, 5L, "LOGICAL_KEY_ALREADY_CLAIMED");
        verify(metrics).recordRetryLoopPrevented(eq("claim_converged"));
    }

    @Test
    @DisplayName("DuplicateKey + rootKeyVersion 冲突 → 不可重试 BusinessKeyLogicalIdentityConflict")
    void duplicateKey_rootMismatch_identityConflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, 8L, KMS_REF); // root 不同
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(repository, never()).markPlaceholderSuperseded(any(), any(), any());
        verify(metrics).recordIdentityConflict(eq("root_mismatch"));
    }

    @Test
    @DisplayName("DuplicateKey + kmsKeyRef 冲突 → 不可重试")
    void duplicateKey_kmsRefMismatch_identityConflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, "kms/ref/other");
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(metrics).recordIdentityConflict(eq("kms_ref_mismatch"));
    }

    @Test
    @DisplayName("DuplicateKey + 上下文（deviceSn）冲突 → 不可重试")
    void duplicateKey_contextMismatch_identityConflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF);
        existing.setDeviceSn("SN-OTHER");
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(metrics).recordIdentityConflict(eq("context_mismatch"));
    }

    @Test
    @DisplayName("DuplicateKey + KCV 证据冲突 → 不可重试")
    void duplicateKey_kcvMismatch_identityConflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.ACTIVE, ROOT_VERSION, KMS_REF);
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        BusinessKeyProvisioningItem item = BusinessKeyProvisioningItem.builder()
                .keyCheckValue(new byte[]{0x01, 0x02, 0x03}).build();
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(item);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(metrics).recordIdentityConflict(eq("kcv_mismatch"));
    }

    @Test
    @DisplayName("并发收敛：attach 返回 0 且占位行已被并发回填 → 校验一致后复用本行")
    void concurrentAttach_alreadyBackfilled_reuseSameRow() {
        when(repository.attachDerivedMaterial(any())).thenReturn(0);
        DeviceBusinessKey current = pendingRow();
        current.setKeyId(KEY_ID);
        current.setKmsKeyRef(KMS_REF);
        current.setRootKeyVersion(ROOT_VERSION);
        current.setDerivationVersion(DERIVATION_VERSION);
        current.setAlgorithm("AES");
        current.setKeySpec("AES-256-GCM");
        when(repository.selectById(1L)).thenReturn(current);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(1L, id);
        verify(repository, never()).markPlaceholderSuperseded(any(), any(), any());
    }

    @Test
    @DisplayName("并发收敛：占位行已被并发 claim（SUPERSEDED）→ 跟随 canonical")
    void concurrentAttach_superseded_followCanonical() {
        when(repository.attachDerivedMaterial(any())).thenReturn(0);
        DeviceBusinessKey current = pendingRow();
        current.setKeyState(BusinessKeyState.SUPERSEDED);
        current.setSupersededById(5L);
        when(repository.selectById(1L)).thenReturn(current);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(5L, id);
    }

    @Test
    @DisplayName("DuplicateKey + FAILED 有 keyId 证据 → FAILED→PENDING_RECOVERY→PENDING 恢复同一行并复用")
    void duplicateKey_failedWithEvidence_recoverSameRow() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.FAILED, ROOT_VERSION, KMS_REF);
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);
        when(itemRepository.selectByDirectoryKeyId(5L)).thenReturn(null);
        when(repository.markPendingRecovery(5L, "CR066_LOGICAL_KEY_RECOVERY")).thenReturn(1);
        when(repository.confirmRecovery(5L, "REQ-1")).thenReturn(1);

        Long id = service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx());

        assertEquals(5L, id);
        verify(repository).markPendingRecovery(5L, "CR066_LOGICAL_KEY_RECOVERY");
        verify(repository).confirmRecovery(5L, "REQ-1");
        verify(itemRepository).rebindDirectoryKey(100L, 5L, KEY_ID, 1L);
        verify(metrics).recordClaim(eq("reuse"), eq("recovered"), eq("failed_evidence_recovered"));
    }

    @Test
    @DisplayName("DuplicateKey + FAILED 行 rootKeyVersion 缺失（身份无法校验）→ 不可恢复，拒绝")
    void duplicateKey_failedRootMissing_identityConflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.FAILED, null, KMS_REF); // root 缺失
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(repository, never()).markPendingRecovery(any(), any());
        verify(metrics).recordIdentityConflict(eq("root_mismatch"));
    }

    @Test
    @DisplayName("DuplicateKey + FAILED 行 keyId 证据缺失 → 不可恢复（evidence_incomplete），拒绝")
    void duplicateKey_failedKeyIdMissing_conflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.FAILED, ROOT_VERSION, KMS_REF);
        existing.setKeyId(null); // keyId 证据缺失（防御路径）
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(metrics).recordIdentityConflict(eq("evidence_incomplete"));
    }

    @Test
    @DisplayName("DuplicateKey + 状态不可复用（DEPRECATED）→ 拒绝")
    void duplicateKey_deprecatedNotReusable_conflict() {
        when(repository.attachDerivedMaterial(any())).thenThrow(new DuplicateKeyException("uk_key_id"));
        DeviceBusinessKey existing = existingRow(BusinessKeyState.DEPRECATED, ROOT_VERSION, KMS_REF);
        when(repository.selectByKeyIdForUpdate(KEY_ID)).thenReturn(existing);

        assertThrows(BusinessKeyLogicalIdentityConflict.class,
                () -> service.persistOrClaimDerivedMaterial(pendingRow(), result(), policy(), "OP-1", 100L, ctx()));
        verify(metrics).recordIdentityConflict(eq("state_not_reusable"));
    }

    @Test
    @DisplayName("重试路径：占位行已带 keyId → 校验身份一致后直接复用本行（不新建、不 claim）")
    void existingKeyIdPath_identityMatch_reuseSameRow() {
        DeviceBusinessKey backfilled = pendingRow();
        backfilled.setKeyId(KEY_ID);
        backfilled.setKmsKeyRef(KMS_REF);
        backfilled.setRootKeyVersion(ROOT_VERSION);
        backfilled.setDerivationVersion(DERIVATION_VERSION);
        backfilled.setAlgorithm("AES");
        backfilled.setKeySpec("AES-256-GCM");
        backfilled.setPolicyVersion("v1");

        Long id = service.persistOrClaimDerivedMaterial(backfilled, result(), policy(), "OP-1", 100L, ctx());

        assertEquals(1L, id);
        verify(repository, never()).attachDerivedMaterial(any());
        verify(repository, never()).markPlaceholderSuperseded(any(), any(), any());
    }
}
