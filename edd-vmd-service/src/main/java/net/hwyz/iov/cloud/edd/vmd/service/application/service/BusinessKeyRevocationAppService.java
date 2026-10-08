package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.BusinessKeyChangedPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyOutcomeUnknownException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyRevocationFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyCacheInvalidator;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.BusinessKeyNotFoundException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyRef;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.RevocationRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.LocalDateTime;

/**
 * 业务密钥吊销应用服务（CR-055 F20 §6.5）
 * <p>
 * 先转 REVOKING 立即阻断业务使用（新加解密拒绝），再按显式 keyRef 调 framework revoke；
 * 成功转 REVOKED 并发布缓存失效事件；结果未知转 RECONCILE_REQUIRED 继续阻断并以原幂等键对账；
 * 不得因超时恢复为 ACTIVE。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyRevocationAppService {

    private static final String STATE_REVOKING = "REVOKING";
    private static final String STATE_REVOKED = "REVOKED";

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final ObjectProvider<BusinessKeyMaterialTemplate> businessKeyMaterialTemplateProvider;
    private final BusinessKeyChangedPublisher businessKeyChangedPublisher;
    private final ObjectProvider<BusinessKeyCacheInvalidator> cacheInvalidatorProvider;
    private final PlatformTransactionManager transactionManager;

    private volatile TransactionTemplate transactionTemplate;

    /**
     * 获取 BusinessKeyMaterialTemplate（framework 业务密钥未装配时 fail-closed）
     */
    private BusinessKeyMaterialTemplate materialTemplate() {
        BusinessKeyMaterialTemplate template = businessKeyMaterialTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new BusinessKeyKmsUnavailableException("framework业务密钥模板未装配（crypto.business-key.enabled=false）");
        }
        return template;
    }

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
     * 吊销业务密钥
     *
     * @param cmd 吊销命令（按 keyId）
     * @return 吊销结果
     */
    public BusinessKeyRevokeResult revoke(BusinessKeyRevokeCmd cmd) {
        log.info("业务密钥吊销: requestId={}, keyId={}, reason={}", cmd.getRequestId(), cmd.getKeyId(), cmd.getReason());
        validateCmd(cmd);

        DeviceBusinessKey key = deviceBusinessKeyRepository.selectByKeyId(cmd.getKeyId());
        if (key == null) {
            throw new BusinessKeyNotExistException("业务密钥不存在: keyId=" + cmd.getKeyId());
        }
        authorizationPolicy.authorize(BusinessKeyAction.REVOKE, null,
                key.getBusinessDomain(), key.getPurpose());

        // 1. 转 REVOKING（立即阻断新加解密），幂等：已 REVOKING/REVOKED 继续
        DeviceBusinessKey revoked = markRevokingOrReuse(cmd, key);
        if (revoked.getKeyState() == BusinessKeyState.REVOKED) {
            return buildResult(cmd, revoked, STATE_REVOKED);
        }

        // 2. 按显式 keyRef 调 framework revoke（事务外网络 I/O）
        try {
            materialTemplate().revoke(
                    new BusinessKeyRef(revoked.getKeyId(), revoked.getKmsKeyRef()),
                    new RevocationRequest(cmd.getReason(), cmd.getRequestId()));
        } catch (BusinessKeyNotFoundException e) {
            // KMS 已吊销/不存在 → 幂等成功
            log.info("业务密钥在KMS不存在或已吊销，幂等成功: keyId={}", cmd.getKeyId());
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.error("业务密钥吊销结果未知: keyId={}", cmd.getKeyId(), e);
            markReconcileRequired(cmd.getKeyId(), "吊销结果未知，待对账");
            invalidateCache(cmd.getKeyId());
            throw new BusinessKeyOutcomeUnknownException("吊销结果未知，待对账: keyId=" + cmd.getKeyId());
        } catch (CryptoDependencyUnavailableException e) {
            log.error("KMS/HSM或framework安全服务不可用，吊销中断（REVOKING保持阻断）: keyId={}", cmd.getKeyId(), e);
            throw new BusinessKeyKmsUnavailableException("KMS/HSM或framework安全服务不可用: keyId=" + cmd.getKeyId());
        } catch (CryptoException e) {
            log.error("业务密钥吊销失败（REVOKING保持阻断，可重试）: keyId={}", cmd.getKeyId(), e);
            throw new BusinessKeyRevocationFailedException("业务密钥吊销失败: keyId=" + cmd.getKeyId());
        }

        // 3. 成功转 REVOKED + 缓存失效事件
        markRevoked(cmd.getKeyId());
        invalidateCache(cmd.getKeyId());
        return buildResult(cmd, deviceBusinessKeyRepository.selectByKeyId(cmd.getKeyId()), STATE_REVOKED);
    }

    /**
     * 转 REVOKING 或复用已有吊销行（幂等）
     */
    private DeviceBusinessKey markRevokingOrReuse(BusinessKeyRevokeCmd cmd, DeviceBusinessKey key) {
        return tx().execute(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectByKeyId(cmd.getKeyId());
            if (current == null) {
                throw new BusinessKeyNotExistException("业务密钥不存在: keyId=" + cmd.getKeyId());
            }
            if (current.getKeyState() == BusinessKeyState.REVOKED) {
                return current;
            }
            if (current.getKeyState() == BusinessKeyState.REVOKING) {
                return current;
            }
            if (current.getKeyState() != BusinessKeyState.ACTIVE
                    && current.getKeyState() != BusinessKeyState.DEPRECATED
                    && current.getKeyState() != BusinessKeyState.EXPIRED) {
                throw new BusinessKeyStateNotAllowedException(
                        "业务密钥状态不允许吊销: keyId=" + cmd.getKeyId() + ", state=" + current.getKeyState().getValue());
            }
            current.markRevoking("吊销原因: " + cmd.getReason());
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
            businessKeyChangedPublisher.publish(current, STATE_REVOKING);
            return current;
        });
    }

    /**
     * 吊销成功转 REVOKED + REVOKED 事件
     */
    private void markRevoked(String keyId) {
        tx().executeWithoutResult(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectByKeyId(keyId);
            if (current == null) {
                return;
            }
            current.markRevoked();
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
            businessKeyChangedPublisher.publish(current, STATE_REVOKED);
        });
    }

    /**
     * 结果未知转 RECONCILE_REQUIRED（继续阻断，以原幂等键对账）
     */
    private void markReconcileRequired(String keyId, String reason) {
        tx().executeWithoutResult(status -> {
            DeviceBusinessKey current = deviceBusinessKeyRepository.selectByKeyId(keyId);
            if (current == null) {
                return;
            }
            current.markReconcileRequired(reason);
            current.setModifyTime(LocalDateTime.now());
            deviceBusinessKeyRepository.update(current);
        });
    }

    /**
     * 缓存失效（KeyCache/BusinessKeyCacheInvalidator）
     */
    private void invalidateCache(String keyId) {
        try {
            BusinessKeyCacheInvalidator invalidator = cacheInvalidatorProvider.getIfAvailable();
            if (invalidator != null) {
                invalidator.invalidateKey(keyId);
            }
        } catch (Exception e) {
            log.warn("业务密钥缓存失效异常（不影响吊销结果）: keyId={}", keyId, e);
        }
    }

    private BusinessKeyRevokeResult buildResult(BusinessKeyRevokeCmd cmd, DeviceBusinessKey key, String state) {
        return BusinessKeyRevokeResult.builder()
                .requestId(cmd.getRequestId())
                .keyId(cmd.getKeyId())
                .businessKeyVersion(key != null ? key.getBusinessKeyVersion() : null)
                .state(state)
                .build();
    }

    private void validateCmd(BusinessKeyRevokeCmd cmd) {
        if (cmd == null || StrUtil.isBlank(cmd.getRequestId()) || StrUtil.isBlank(cmd.getKeyId())) {
            throw new IllegalArgumentException("业务密钥吊销参数不完整: requestId/keyId 必填");
        }
        if (StrUtil.isBlank(cmd.getReason())) {
            throw new IllegalArgumentException("业务密钥吊销原因必填");
        }
    }
}
