package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link Kl1LegacyMigrationService} 单元测试（VMD-DSN-CR-069 §5）。
 * <p>
 * 覆盖：already_kl1 幂等跳过（重跑不重置）、证据完整回填（F 仅从 response_persisted_at 恢复、
 * B=F+300s、content digest 从已验证 bytes）、B 已到受控终结、证据缺失/损坏 quarantine（不重 derive）、
 * dry-run 只读不落库。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class Kl1LegacyMigrationServiceTest {

    private BusinessKeyProvisioningRepository provisioningRepository;
    private BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private KeyProvisioningResponseStore responseStore;
    private Kl1BatchTerminator kl1BatchTerminator;
    private BusinessKeyProvisioningMetrics metrics;
    private Kl1LegacyMigrationService service;
    private PlatformTransactionManager transactionManager;
    private org.springframework.transaction.TransactionStatus transactionStatus;

    @BeforeEach
    void setUp() {
        provisioningRepository = mock(BusinessKeyProvisioningRepository.class);
        provisioningItemRepository = mock(BusinessKeyProvisioningItemRepository.class);
        deviceBusinessKeyRepository = mock(DeviceBusinessKeyRepository.class);
        responseStore = mock(KeyProvisioningResponseStore.class);
        kl1BatchTerminator = mock(Kl1BatchTerminator.class);
        metrics = mock(BusinessKeyProvisioningMetrics.class);
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(org.springframework.transaction.TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        service = new Kl1LegacyMigrationService(provisioningRepository, provisioningItemRepository,
                deviceBusinessKeyRepository, responseStore, kl1BatchTerminator, metrics, transactionManager);
    }

    private BusinessKeyProvisioning legacyBatch(String requestId, boolean withEvidence, boolean kl1Already) {
        LocalDateTime started = LocalDateTime.now().minusMinutes(10);
        LocalDateTime persisted = withEvidence ? started.plusMinutes(2) : null;
        BusinessKeyProvisioning batch = BusinessKeyProvisioning.builder()
                .id(1L)
                .requestId(requestId)
                .status(BusinessKeyProvisioningState.WAIT_DEVICE_ACK)
                .startedAt(started)
                .responsePersistedAt(persisted)
                .responseCodecVersion(ParProtoWireCanonical.AAD_PROFILE)
                .build();
        if (withEvidence) {
            batch.setResponsePayload(new byte[]{1, 2, 3});
            batch.setResponseDigest("deadbeef");
        }
        if (kl1Already) {
            batch.setLifecycleProfile(Kl1Constants.PROFILE);
            batch.setFirstIssuedAt(persisted);
            batch.setAckDeadlineAt(persisted.plus(java.time.Duration.ofMillis(Kl1Constants.BATCH_MS)));
        }
        return batch;
    }

    @Test
    @DisplayName("证据完整 + F 可信 → 回填 KL1：F=response_persisted_at、B=F+300s、content digest 落库")
    void backfill_withTrustedEvidence() {
        BusinessKeyProvisioning batch = legacyBatch("kp-f55df0e59917e989042fdb0b9941f394", true, false);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(provisioningRepository.selectByRequestIdForUpdate(anyString())).thenReturn(batch);
        when(responseStore.load("kp-f55df0e59917e989042fdb0b9941f394"))
                .thenReturn(Kl1WireCodec.patchField4(responseWire(), 1_800_000_060_000L));
        when(kl1BatchTerminator.terminateIfDeadlinePassed(any(), any())).thenReturn(false);
        when(provisioningRepository.update(any())).thenReturn(1);

        Kl1LegacyMigrationService.MigrationReport report = service.migrate(10);
        assertEquals(1, report.backfilled(), "可信证据回填");
        assertEquals(0, report.quarantined());
        // F/B 恢复自 response_persisted_at（非升级时间/modify_time）
        assertEquals(batch.getResponsePersistedAt(), batch.getFirstIssuedAt());
        assertEquals(batch.getResponsePersistedAt().plus(java.time.Duration.ofMillis(Kl1Constants.BATCH_MS)),
                batch.getAckDeadlineAt(), "B = F + 300s");
        assertNotNull(batch.getImmutableContentDigest());
        assertEquals(Kl1Constants.PROFILE, batch.getLifecycleProfile());
    }

    @Test
    @DisplayName("B 已到 → 回填后受控终结 FAILED(ACK_TIMEOUT)，现场批次无特例")
    void ackTimeout_whenDeadlinePassed() {
        BusinessKeyProvisioning batch = legacyBatch("kp-f55df0e59917e989042fdb0b9941f394", true, false);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(provisioningRepository.selectByRequestIdForUpdate(anyString())).thenReturn(batch);
        when(responseStore.load(anyString())).thenReturn(Kl1WireCodec.patchField4(responseWire(), 1_800_000_060_000L));
        when(kl1BatchTerminator.terminateIfDeadlinePassed(any(), any())).thenReturn(true);
        when(provisioningRepository.update(any())).thenReturn(1);

        Kl1LegacyMigrationService.MigrationReport report = service.migrate(10);
        assertEquals(1, report.ackTimeout(), "B 已过 → 统一规则 ACK_TIMEOUT（不给现场ID特例续命）");
    }

    @Test
    @DisplayName("F 缺失（response_persisted_at NULL）→ quarantine，不重 derive 救场")
    void quarantine_whenEvidenceMissing() {
        BusinessKeyProvisioning batch = legacyBatch("kp-x", false, false);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(provisioningRepository.selectByRequestIdForUpdate(anyString())).thenReturn(batch);
        when(provisioningRepository.update(any())).thenReturn(1);

        Kl1LegacyMigrationService.MigrationReport report = service.migrate(10);
        assertEquals(1, report.quarantined());
        assertEquals(Kl1Constants.TERMINAL_REASON_MIGRATION_QUARANTINE, batch.getTerminalReason());
        assertFalse(batch.isKl1(), "quarantine 不回填 KL1");
    }

    @Test
    @DisplayName("digest 损坏（replay store 校验失败）→ quarantine，保留只读证据")
    void quarantine_whenDigestCorrupted() {
        BusinessKeyProvisioning batch = legacyBatch("kp-x", true, false);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(provisioningRepository.selectByRequestIdForUpdate(anyString())).thenReturn(batch);
        when(responseStore.load(anyString()))
                .thenThrow(new KeyprovEnvelopeIntegrityException("Response 重放字节完整性校验失败"));
        when(provisioningRepository.update(any())).thenReturn(1);

        Kl1LegacyMigrationService.MigrationReport report = service.migrate(10);
        assertEquals(1, report.quarantined());
        assertEquals(Kl1Constants.TERMINAL_REASON_MIGRATION_QUARANTINE, batch.getTerminalReason());
    }

    @Test
    @DisplayName("已 KL1 → 跳过（重跑不重置 F/B/lease_seq）")
    void alreadyKl1_skipped() {
        BusinessKeyProvisioning batch = legacyBatch("kp-y", true, true);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(provisioningRepository.selectByRequestIdForUpdate(anyString())).thenReturn(batch);
        LocalDateTime fBefore = batch.getFirstIssuedAt();
        Kl1LegacyMigrationService.MigrationReport report = service.migrate(10);
        assertEquals(1, report.alreadyKl1());
        assertEquals(fBefore, batch.getFirstIssuedAt(), "重跑不重置 F");
        verify(provisioningRepository, never()).update(batch);
    }

    @Test
    @DisplayName("dry-run：只读影子判定，不落库（回填/隔离均不执行）")
    void dryRun_noWrites() {
        BusinessKeyProvisioning batch = legacyBatch("kp-z", true, false);
        when(provisioningRepository.selectKl1MigrationCandidates(anyInt())).thenReturn(List.of(batch));
        when(responseStore.load(anyString())).thenReturn(Kl1WireCodec.patchField4(responseWire(), 1_800_000_060_000L));

        Kl1LegacyMigrationService.MigrationReport report = service.dryRun(10);
        assertEquals(1, report.backfilled());
        verify(provisioningRepository, never()).update(any());
        verify(kl1BatchTerminator, never()).terminateIfDeadlinePassed(any(), any());
    }

    private byte[] responseWire() {
        return net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse.newBuilder()
                .setRequestId("kp-legacy")
                .setResult(net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                .setExpireAtMs(1_800_000_000_000L)
                .build()
                .toByteArray();
    }
}
