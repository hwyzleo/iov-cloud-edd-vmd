package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务密钥维护应用服务（CR-055 F20 §6.4 步骤7）
 * <p>
 * 超过 decrypt_until 的 DEPRECATED 行转 EXPIRED，并发布 EXPIRED 事件。
 * 首期仅做状态迁移与事件通知，KMS 材料吊销按策略后续另立 CR（不自动吊销）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyMaintenanceAppService {

    private static final String STATE_EXPIRED = "EXPIRED";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyProvisioningRepository businessKeyProvisioningRepository;
    private final BusinessKeyProvisioningItemRepository businessKeyProvisioningItemRepository;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final PlatformTransactionManager transactionManager;

    private volatile TransactionTemplate transactionTemplate;

    /**
     * 惰性初始化 TransactionTemplate（保证单元测试可注入）
     */
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
     * 扫批转 EXPIRED：超过 decrypt_until 的 DEPRECATED 行
     *
     * @param limit 批量上限
     * @return 处理行数
     */
    public int expireDeprecatedKeys(int limit) {
        List<DeviceBusinessKey> keys = deviceBusinessKeyRepository.selectDeprecatedExpired(LocalDateTime.now(), limit);
        if (keys.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (DeviceBusinessKey key : keys) {
            try {
                expireOne(key);
                processed++;
            } catch (Exception e) {
                log.warn("业务密钥转EXPIRED失败: keyId={}", key.getKeyId(), e);
            }
        }
        log.info("业务密钥转EXPIRED扫批完成: processed={}, total={}", processed, keys.size());
        return processed;
    }

    private void expireOne(DeviceBusinessKey key) {
        tx().executeWithoutResult(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectById(key.getId());
            if (current == null || current.getKeyState() != net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState.DEPRECATED) {
                return;
            }
            current.markExpired();
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
            businessKeyChangedPublisher.publish(current, STATE_EXPIRED);
        });
    }

    /**
     * WAIT_DEVICE_ACK 超时转 EXPIRED（CR-058 §8）。
     * <p>不生成伪成功事件；本批次新建的 PENDING 目标版本置 FAILED（fail-closed），
     * 复用 ACTIVE 不受影响。
     *
     * @param cutoff 超时截止时间（modified 早于该时间视为超时）
     * @param limit  批量上限
     * @return 处理批次数
     */
    public int expireStaleAckBatches(LocalDateTime cutoff, int limit) {
        List<BusinessKeyProvisioning> batches = businessKeyProvisioningRepository.selectListByStatusBefore(
                BusinessKeyProvisioningState.WAIT_DEVICE_ACK, cutoff, limit);
        if (batches.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (BusinessKeyProvisioning batch : batches) {
            try {
                expireAckBatch(batch.getRequestId());
                processed++;
            } catch (Exception e) {
                log.warn("业务密钥开通批次转EXPIRED失败: requestId={}", batch.getRequestId(), e);
            }
        }
        log.info("业务密钥开通批次转EXPIRED扫批完成: processed={}, total={}", processed, batches.size());
        return processed;
    }

    private void expireAckBatch(String requestId) {
        tx().executeWithoutResult(status -> {
            BusinessKeyProvisioning current = businessKeyProvisioningRepository.selectByRequestIdForUpdate(requestId);
            if (current == null || current.getStatus() != BusinessKeyProvisioningState.WAIT_DEVICE_ACK) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            for (BusinessKeyProvisioningItem item : businessKeyProvisioningItemRepository.selectByProvisioningId(current.getId())) {
                DeviceBusinessKey directory = deviceBusinessKeyRepository.selectById(item.getDirectoryKeyId());
                if (directory != null && directory.getKeyState() == BusinessKeyState.PENDING) {
                    directory.markFailed("开通批次ACK超时过期");
                    directory.setModifyTime(now);
                    deviceBusinessKeyRepository.update(directory);
                }
            }
            current.markExpired("设备导入ACK超时");
            current.setModifyTime(now);
            businessKeyProvisioningRepository.update(current);
            log.error("业务密钥开通批次ACK超时转EXPIRED: requestId={}, deviceSn={}", requestId, current.getDeviceSn());
        });
    }

    /**
     * RECONCILE_REQUIRED 超阈值告警（CR-058 §8）。
     * <p>对账状态不与普通 ACK 超时共用短 TTL，仅输出结构化告警，不直接过期。
     *
     * @param cutoff 告警阈值
     * @param limit  批量上限
     * @return 告警批次数
     */
    public int alertStaleReconcileBatches(LocalDateTime cutoff, int limit) {
        List<BusinessKeyProvisioning> batches = businessKeyProvisioningRepository.selectListByStatusBefore(
                BusinessKeyProvisioningState.RECONCILE_REQUIRED, cutoff, limit);
        for (BusinessKeyProvisioning batch : batches) {
            log.error("业务密钥开通批次待对账超阈值告警: requestId={}, deviceSn={}, domain={}, modifyTime={}",
                    batch.getRequestId(), batch.getDeviceSn(), batch.getBusinessDomain(), batch.getModifyTime());
        }
        return batches.size();
    }
}
