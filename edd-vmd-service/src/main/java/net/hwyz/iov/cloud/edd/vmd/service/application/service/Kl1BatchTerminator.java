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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * KL1 批次到期终态器（VMD-DSN-CR-069 §2/§4）。
 * <p>
 * Request/ACK/Status 入口惰性到期与 ≤5s sweeper 共用同一规则：锁内判定 now≥B 且未 ACTIVE →
 * CAS FAILED(ACK_TIMEOUT) 并释放本批次 pending claim（不删除共享 material/原 proof、不假定 SEC
 * 无副作用）。FAILED 为终态，迟到 ACK 绝不复活；公开 Status 仍为既有 FAILED 枚举。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Kl1BatchTerminator {

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;

    /**
     * 锁内惰性到期判定与终结（调用方须已在事务内持有批次行锁）。
     *
     * @param batch 已加锁的批次
     * @param now   服务端 UTC 当前时间
     * @return true 表示本次调用完成了 FAILED(ACK_TIMEOUT) 终结
     */
    public boolean terminateIfDeadlinePassed(BusinessKeyProvisioning batch, LocalDateTime now) {
        if (batch == null || !batch.isKl1() || !batch.getStatus().isOpen()) {
            return false;
        }
        if (batch.getAckDeadlineAt() == null || now.isBefore(batch.getAckDeadlineAt())) {
            return false;
        }
        // now ≥ B 且未 ACTIVE → FAILED(ACK_TIMEOUT)；释放本批次 PENDING claim（不删共享 material/原 proof）
        LocalDateTime at = now;
        for (BusinessKeyProvisioningItem item : provisioningItemRepository.selectByProvisioningId(batch.getId())) {
            markPendingKeyFailed(deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId()), at);
        }
        batch.markAckTimeout(at);
        provisioningRepository.update(batch);
        log.warn("KL1 批次到期 FAILED(ACK_TIMEOUT): requestId={}, deviceSn={}, B={}, now={}",
                batch.getRequestId(), batch.getDeviceSn(), batch.getAckDeadlineAt(), now);
        return true;
    }

    /**
     * 释放本批次创建的 PENDING 目录行归属（FAILED 行保留 keyId 证据，供后续新 rid 按
     * CR-066 selectRecoverableByContext 恢复复用；不删除共享 key_material）。
     */
    private void markPendingKeyFailed(DeviceBusinessKey directory, LocalDateTime now) {
        if (directory != null && directory.getKeyState() == BusinessKeyState.PENDING) {
            directory.markFailed("KL1批次到期ACK_TIMEOUT，释放pending归属");
            directory.setModifyTime(now);
            deviceBusinessKeyRepository.update(directory);
        }
    }
}
