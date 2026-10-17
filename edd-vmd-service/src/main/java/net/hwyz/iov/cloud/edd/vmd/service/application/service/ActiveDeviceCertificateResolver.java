package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDeviceCertNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.CertificateSerialUtils;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 当前有效设备证书解析器（CR-055 §4，CR-060 §4.2）
 * <p>
 * 业务密钥设备证书公钥 Wrap 的收件方证书：解析设备当前有效（ACTIVE）设备身份证书。
 * 首期设备身份证书 Profile 固定为 TBOX_TSP_CLIENT（对齐 D25 设备证书签发域）。
 * 解析失败 fail-closed，不得退回或使用其它证书。
 * <p>
 * CR-060 §4.2：除 serial 外，同时返回 {@code pkiRequestId}（材料 locator）与 SHA-256 指纹
 * （expected evidence），使调用方可构造含直接证据的 RecipientRef，减少一次目录查询。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveDeviceCertificateResolver {

    /**
     * 设备身份证书 Profile（与 CertificateIdentityValidator.ALLOWED_PROFILE 对齐）
     */
    private static final String DEVICE_IDENTITY_PROFILE = "TBOX_TSP_CLIENT";

    private final VehicleCertificateRepository vehicleCertificateRepository;

    /**
     * 解析当前有效设备证书序列号
     *
     * @param identity active 绑定设备身份
     * @return 设备证书 canonical 序列号
     * @throws BusinessKeyDeviceCertNotFoundException 无有效设备证书
     */
    public String resolveActiveCertSn(BoundDeviceIdentity identity) {
        return resolveActive(identity).certSn();
    }

    /**
     * 解析当前有效设备证书（serial + pkiRequestId + fingerprint，CR-060 §4.2）
     *
     * @param identity active 绑定设备身份
     * @return 设备证书证据集合（序列号 canonical 化）
     * @throws BusinessKeyDeviceCertNotFoundException 无有效设备证书
     */
    public ActiveDeviceCertificate resolveActive(BoundDeviceIdentity identity) {
        VehicleCertificate cert = vehicleCertificateRepository
                .selectActiveByDeviceSnAndProfile(identity.deviceSn(), DEVICE_IDENTITY_PROFILE);
        if (cert == null) {
            log.warn("设备[{}]无有效设备证书（profile={}），拒绝业务密钥设备下发", identity.deviceSn(), DEVICE_IDENTITY_PROFILE);
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]无有效设备证书(profile=" + DEVICE_IDENTITY_PROFILE + ")");
        }
        if (cert.getNotAfter() != null && cert.getNotAfter().isBefore(LocalDateTime.now())) {
            log.warn("设备[{}]设备证书已过期 certSn={}, notAfter={}，拒绝业务密钥设备下发",
                    identity.deviceSn(), cert.getCertSn(), cert.getNotAfter());
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]设备证书已过期: certSn=" + cert.getCertSn());
        }
        // canonical 化（CR-060 §8.2）：Recipient/expectedSerial 口径统一，禁止 VMD 自定义规则
        String certSn = CertificateSerialUtils.canonical(cert.getCertSn());
        return new ActiveDeviceCertificate(certSn, cert.getPkiRequestId(), cert.getCertificateFingerprint());
    }
}
