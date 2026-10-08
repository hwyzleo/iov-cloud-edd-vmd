package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
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
}
