package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * KL1 存量批次迁移服务（VMD-DSN-CR-069 §5 / VMD-KL-REQ-004）。
 * <p>
 * 仅由可信首次持久化/事务审计恢复 F：{@code response_persisted_at}（CR-068 首次发送前持久化时间，
 * 非 modify_time/最后重放时间/升级时间）；B=F+300s。规则：
 * <ul>
 *   <li>已 KL1 → 跳过（重跑不重置 F/B/lease_seq）；</li>
 *   <li>原 payload/digest/codec 完整可验且 F 可信 → 幂等回填 KL1；B 未到保留（可续签），B 已到
 *       CAS FAILED(ACK_TIMEOUT) 并释放本批次 pending claim；</li>
 *   <li>F 缺失/矛盾、digest 损坏、未知 codec、Provider outcome 未决 → quarantine（停止新 bundle、
 *       保留只读证据与告警；不重 derive 救场、不猜 NOT_FOUND）。</li>
 * </ul>
 * 先 dry-run 计数与影子读取差异，再受控批次切换；现场 kp-f55df0… 按同一规则处理，不给特例续命。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Kl1LegacyMigrationService {

    private static final String KNOWN_CODEC = ParProtoWireCanonical.AAD_PROFILE;

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final KeyProvisioningResponseStore responseStore;
    private final Kl1BatchTerminator kl1BatchTerminator;
    private final BusinessKeyProvisioningMetrics metrics;
    private final PlatformTransactionManager transactionManager;

    private volatile TransactionTemplate transactionTemplate;

    private TransactionTemplate tx() {
        TransactionTemplate tpl = this.transactionTemplate;
        if (tpl == null) {
            synchronized (this) {
                if (this.transactionTemplate == null) {
                    this.transactionTemplate = new TransactionTemplate(transactionManager);
                }
                tpl = this.transactionTemplate;
            }
        }
        return tpl;
    }

    /**
     * 迁移结果报告（每批次一个分类）。
     */
    public static final class MigrationReport {
        private final List<String> backfilled = new ArrayList<>();
        private final List<String> quarantined = new ArrayList<>();
        private final List<String> ackTimeout = new ArrayList<>();
        private final List<String> alreadyKl1 = new ArrayList<>();
        private final List<String> skipped = new ArrayList<>();

        public int backfilled() {
            return backfilled.size();
        }

        public int quarantined() {
            return quarantined.size();
        }

        public int ackTimeout() {
            return ackTimeout.size();
        }

        public int alreadyKl1() {
            return alreadyKl1.size();
        }

        public int skipped() {
            return skipped.size();
        }

        public int total() {
            return backfilled.size() + quarantined.size() + ackTimeout.size()
                    + alreadyKl1.size() + skipped.size();
        }

        public void summarize() {
            log.info("KL1 存量迁移汇总: backfilled={}, quarantined={}, ackTimeout={}, alreadyKl1={}, skipped={}",
                    backfilled.size(), quarantined.size(), ackTimeout.size(), alreadyKl1.size(), skipped.size());
        }
    }

    /**
     * Dry-run：只计数与影子读取差异，不落库（VMD-DSN-CR-069 §5.2）。
     */
    public MigrationReport dryRun(int limit) {
        MigrationReport report = new MigrationReport();
        List<BusinessKeyProvisioning> candidates = provisioningRepository.selectKl1MigrationCandidates(limit);
        for (BusinessKeyProvisioning batch : candidates) {
            classify(batch, report, true);
        }
        report.summarize();
        return report;
    }

    /**
     * 正式迁移：幂等回填/受控终结/quarantine，受控批次切换（重跑不重置 F/B/lease_seq）。
     */
    public MigrationReport migrate(int limit) {
        MigrationReport report = new MigrationReport();
        List<BusinessKeyProvisioning> candidates = provisioningRepository.selectKl1MigrationCandidates(limit);
        for (BusinessKeyProvisioning batch : candidates) {
            try {
                tx().executeWithoutResult(status -> migrateOne(batch.getRequestId(), report));
            } catch (Exception e) {
                log.warn("KL1 存量迁移单批失败（隔离，不中断）: requestId={}", batch.getRequestId(), e);
                report.skipped.add(batch.getRequestId());
            }
        }
        report.summarize();
        return report;
    }

    private void migrateOne(String requestId, MigrationReport report) {
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestIdForUpdate(requestId);
        if (batch == null) {
            report.skipped.add(requestId);
            return;
        }
        classify(batch, report, false);
    }

    /**
     * 单批分类与迁移（dryRun=true 只读影子判定；false 落库）。
     */
    private void classify(BusinessKeyProvisioning batch, MigrationReport report, boolean dryRun) {
        if (batch.isKl1()) {
            report.alreadyKl1.add(batch.getRequestId());
            metrics.recordKl1Migration("skipped", "already_kl1");
            return;
        }
        if (batch.getStatus() == BusinessKeyProvisioningState.COMPLETED) {
            // 已完成成功事实：无需迁移（ACTIVE 不重发；保留历史证据）
            report.skipped.add(batch.getRequestId());
            metrics.recordKl1Migration("skipped", "completed");
            return;
        }
        // 证据校验：payload 完整、digest 匹配、codec 已知、F 可信（response_persisted_at 晚于 started_at）
        LocalDateTime f = batch.getResponsePersistedAt();
        boolean evidenceOk = batch.hasPersistedResponse()
                && f != null
                && (batch.getStartedAt() == null || !f.isBefore(batch.getStartedAt()))
                && KNOWN_CODEC.equals(batch.getResponseCodecVersion());
        if (!evidenceOk) {
            if (!dryRun) {
                // quarantine：保留只读证据与告警，停止新 bundle（不重 derive 救场）
                batch.markMigrationQuarantine(LocalDateTime.now());
                provisioningRepository.update(batch);
            }
            report.quarantined.add(batch.getRequestId());
            metrics.recordKl1Migration(dryRun ? "skipped" : "quarantine",
                    dryRun ? "dry_run" : "evidence_invalid");
            return;
        }
        // digest 完整性（影子读取差异：replay store 校验）
        byte[] original;
        try {
            original = responseStore.load(batch.getRequestId());
        } catch (Exception e) {
            if (!dryRun) {
                batch.markMigrationQuarantine(LocalDateTime.now());
                provisioningRepository.update(batch);
            }
            report.quarantined.add(batch.getRequestId());
            metrics.recordKl1Migration(dryRun ? "skipped" : "quarantine",
                    dryRun ? "dry_run" : "digest_corrupted");
            return;
        }
        if (original == null) {
            if (!dryRun) {
                batch.markMigrationQuarantine(LocalDateTime.now());
                provisioningRepository.update(batch);
            }
            report.quarantined.add(batch.getRequestId());
            metrics.recordKl1Migration(dryRun ? "skipped" : "quarantine", "payload_missing");
            return;
        }
        if (dryRun) {
            report.backfilled.add(batch.getRequestId());
            metrics.recordKl1Migration("skipped", "dry_run");
            return;
        }
        // 幂等回填 KL1：F=response_persisted_at，B=F+300s，content digest 从已验证原 bytes 计算
        batch.setLifecycleProfile(Kl1Constants.PROFILE);
        batch.setFirstIssuedAt(f);
        batch.setAckDeadlineAt(f.plus(Duration.ofMillis(Kl1Constants.BATCH_MS)));
        batch.setImmutableContentDigest(hex(Kl1WireCodec.immutableContentDigest(original)));
        batch.setModifyTime(LocalDateTime.now());
        provisioningRepository.update(batch);
        metrics.recordKl1Migration("backfill", "backfilled");

        // B 已到 → CAS FAILED(ACK_TIMEOUT) + 释放 claims；B 未到保留（可续签）
        boolean terminated = kl1BatchTerminator.terminateIfDeadlinePassed(
                provisioningRepository.selectByRequestIdForUpdate(batch.getRequestId()), LocalDateTime.now());
        if (terminated) {
            report.ackTimeout.add(batch.getRequestId());
            metrics.recordKl1Terminal("ack_timeout", "migration_deadline_passed");
        } else {
            report.backfilled.add(batch.getRequestId());
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
