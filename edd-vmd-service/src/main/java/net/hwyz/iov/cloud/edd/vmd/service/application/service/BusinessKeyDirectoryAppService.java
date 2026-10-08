package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyMetadataResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyMultipleActiveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyStateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 业务密钥目录应用服务（CR-055 F20 §6.2/§6.3）
 * <p>
 * 正向目录解析（resolveActive）、反向目录解析（resolveByKeyId）与元数据查询（getMetadata）。
 * 调用方身份由接入层认证上下文提供，不接受请求体自报 caller；目录失败时禁止绕过 VMD 直连 KMS。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyDirectoryAppService {

    private final DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    private final BusinessKeyAuthorizationPolicy authorizationPolicy;
    private final BusinessKeyBizTypeMapper bizTypeMapper;

    /**
     * 正向目录解析：framework 运行时按 (deviceSn, bizType, purpose) 解析唯一 ACTIVE
     *
     * @param context framework DeviceKeyContext
     * @return 业务密钥描述符（不返回明文或设备 Wrapped Key）
     */
    public BusinessKeyDescriptor resolveActive(DeviceKeyContext context) {
        BusinessKeyDirectoryQuery query = bizTypeMapper.toDirectoryQuery(context);
        authorizationPolicy.authorize(BusinessKeyAction.RESOLVE_ACTIVE, null,
                query.businessDomain(), query.purpose());

        DeviceBusinessKey active = deviceBusinessKeyRepository.selectActiveByContext(
                query.deviceSn(), query.businessDomain(), query.purpose());
        if (active == null) {
            log.warn("业务密钥正向目录解析未命中: deviceSn={}, domain={}, purpose={}",
                    query.deviceSn(), query.businessDomain(), query.purpose());
            throw new BusinessKeyNotExistException(
                    "业务密钥不存在或无ACTIVE: deviceSn=" + query.deviceSn()
                            + ", domain=" + query.businessDomain() + ", purpose=" + query.purpose());
        }
        assertSingleActive(query);
        return toDescriptor(active);
    }

    /**
     * 正向目录解析：VMD 内部查询上下文（供 Service API 控制器使用）
     *
     * @param query VMD 目录查询键
     * @return 业务密钥描述符
     */
    public BusinessKeyDescriptor resolveActive(BusinessKeyDirectoryQuery query) {
        authorizationPolicy.authorize(BusinessKeyAction.RESOLVE_ACTIVE, null,
                query.businessDomain(), query.purpose());
        DeviceBusinessKey active = deviceBusinessKeyRepository.selectActiveByContext(
                query.deviceSn(), query.businessDomain(), query.purpose());
        if (active == null) {
            throw new BusinessKeyNotExistException(
                    "业务密钥不存在或无ACTIVE: deviceSn=" + query.deviceSn()
                            + ", domain=" + query.businessDomain() + ", purpose=" + query.purpose());
        }
        assertSingleActive(query);
        return toDescriptor(active);
    }

    /**
     * 反向目录解析：按 keyId 解析，核对信封 businessKeyVersion，受控放行 DEPRECATED 解密窗口
     *
     * @param keyId     framework/KMS 不透明标识
     * @param operation 操作（仅 DECRYPT 放行；ENCRYPT 应走正向解析）
     * @return 业务密钥描述符
     */
    public BusinessKeyDescriptor resolveByKeyId(String keyId, KeyOperation operation) {
        DeviceBusinessKey key = deviceBusinessKeyRepository.selectByKeyId(keyId);
        if (key == null) {
            throw new BusinessKeyNotExistException("业务密钥不存在: keyId=" + keyId);
        }
        authorizationPolicy.authorize(BusinessKeyAction.DECRYPT, null,
                key.getBusinessDomain(), key.getPurpose());
        ensureDecryptAllowed(key);
        return toDescriptor(key);
    }

    /**
     * 查询非敏感元数据（含内部 kmsKeyRef，仅受信服务间契约）
     *
     * @param keyId framework/KMS 不透明标识
     * @return 非敏感元数据
     */
    public BusinessKeyMetadataResult getMetadata(String keyId) {
        DeviceBusinessKey key = deviceBusinessKeyRepository.selectByKeyId(keyId);
        if (key == null) {
            throw new BusinessKeyNotExistException("业务密钥不存在: keyId=" + keyId);
        }
        return toMetadata(key);
    }

    /**
     * 管理查询：按设备（可限定域/用途）查询全部业务密钥版本元数据
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域（可空，空为全部域）
     * @param purpose        用途（可空，空为全部用途）
     * @return 元数据列表（按版本升序）
     */
    public List<BusinessKeyMetadataResult> queryByDevice(String deviceSn, String businessDomain, String purpose) {
        List<DeviceBusinessKey> keys = deviceBusinessKeyRepository.selectByDeviceSn(deviceSn);
        return keys.stream()
                .filter(k -> businessDomain == null || businessDomain.isBlank() || businessDomain.equalsIgnoreCase(k.getBusinessDomain()))
                .filter(k -> purpose == null || purpose.isBlank() || purpose.equalsIgnoreCase(k.getPurpose()))
                .map(this::toMetadata)
                .toList();
    }

    private BusinessKeyMetadataResult toMetadata(DeviceBusinessKey key) {
        return BusinessKeyMetadataResult.builder()
                .keyId(key.getKeyId())
                .businessKeyVersion(key.getBusinessKeyVersion())
                .kmsKeyRef(key.getKmsKeyRef())
                .kmsKeyVersion(key.getKmsKeyVersion())
                .kmsProvider(key.getKmsProvider())
                .algorithm(key.getAlgorithm())
                .keySpec(key.getKeySpec())
                .state(key.getKeyState().getValue())
                .validFrom(key.getValidFrom())
                .validTo(key.getValidTo())
                .decryptUntil(key.getDecryptUntil())
                .deviceSn(key.getDeviceSn())
                .businessDomain(key.getBusinessDomain())
                .purpose(key.getPurpose())
                .build();
    }



    /**
     * 多 ACTIVE 数据完整性判定（fail-closed）
     */
    private void assertSingleActive(BusinessKeyDirectoryQuery query) {
        int activeCount = deviceBusinessKeyRepository.countActiveByContext(
                query.deviceSn(), query.businessDomain(), query.purpose());
        if (activeCount > 1) {
            log.error("业务密钥目录存在多个ACTIVE: deviceSn={}, domain={}, purpose={}, count={}",
                    query.deviceSn(), query.businessDomain(), query.purpose(), activeCount);
            throw new BusinessKeyMultipleActiveException(
                    "业务密钥目录存在多个ACTIVE: deviceSn=" + query.deviceSn()
                            + ", domain=" + query.businessDomain() + ", purpose=" + query.purpose());
        }
    }

    /**
     * 解密状态门禁：ACTIVE 或仍在 decrypt_until 内的 DEPRECATED；其余拒绝
     */
    private void ensureDecryptAllowed(DeviceBusinessKey key) {
        LocalDateTime now = LocalDateTime.now();
        if (key.getKeyState() == BusinessKeyState.ACTIVE) {
            return;
        }
        if (key.getKeyState() == BusinessKeyState.DEPRECATED
                && key.getDecryptUntil() != null && key.getDecryptUntil().isAfter(now)) {
            return;
        }
        log.warn("业务密钥反向解析拒绝: keyId={}, state={}, decryptUntil={}", key.getKeyId(), key.getKeyState(), key.getDecryptUntil());
        throw new BusinessKeyStateNotAllowedException(
                "业务密钥状态不允许解密: keyId=" + key.getKeyId() + ", state=" + key.getKeyState().getValue());
    }

    private BusinessKeyDescriptor toDescriptor(DeviceBusinessKey key) {
        return new BusinessKeyDescriptor(
                key.getKeyId(),
                key.getBusinessKeyVersion(),
                key.getKmsKeyRef(),
                toCryptoState(key.getKeyState()),
                toInstant(key.getValidFrom()),
                toInstant(key.getValidTo()),
                toInstant(key.getDecryptUntil()));
    }

    private CryptoKeyState toCryptoState(BusinessKeyState state) {
        return switch (state) {
            case ACTIVE -> CryptoKeyState.ACTIVE;
            case DEPRECATED -> CryptoKeyState.DEPRECATED;
            case REVOKED -> CryptoKeyState.REVOKED;
            case EXPIRED -> CryptoKeyState.EXPIRED;
            default -> null;
        };
    }

    private Instant toInstant(LocalDateTime time) {
        return time != null ? time.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
