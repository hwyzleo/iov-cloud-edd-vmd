package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KL1 批次到期扫批应用服务（VMD-DSN-CR-069 §4，sweeper ≤5s 与入口惰性到期共用）。
 * <p>
 * 对 now≥B 的未终结 KL1 批次执行与入口惰性到期相同的锁内 CAS FAILED(ACK_TIMEOUT) + 释放 claims；
 * Provider 处理中超过 120s 且副作用 unknown 只隔离告警，不按可释放失败处理（本服务不观测 Provider
 * 在途，由入口侧告警承担）。终态 tombstone 不在此删除（保留审计）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Kl1BatchSweepAppService {

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final Kl1BatchTerminator kl1BatchTerminator;
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
     * 扫描并终结到期 KL1 批次（CAS 失败/状态已变自动跳过）。
     *
     * @param limit 批量上限
     * @return 终结批次数
     */
    public int sweep(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<BusinessKeyProvisioning> candidates = provisioningRepository
                .selectKl1PendingBeforeDeadline(now, limit);
        if (candidates.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (BusinessKeyProvisioning candidate : candidates) {
            try {
                boolean done = tx().execute(status -> {
                    BusinessKeyProvisioning current = provisioningRepository.selectByRequestIdForUpdate(candidate.getRequestId());
                    if (current == null || !current.isKl1() || !current.getStatus().isOpen()) {
                        return false;
                    }
                    if (current.getAckDeadlineAt() == null || now.isBefore(current.getAckDeadlineAt())) {
                        return false;
                    }
                    return kl1BatchTerminator.terminateIfDeadlinePassed(current, now);
                });
                if (done) {
                    processed++;
                }
            } catch (Exception e) {
                log.warn("KL1 sweeper 批次终结失败: requestId={}", candidate.getRequestId(), e);
            }
        }
        return processed;
    }
}
