package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehicleDeviceCertificatePublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 证书签发编排应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateProvisioningAppService {

    private final VehicleCertificateRepository vehicleCertificateRepository;
    private final VehiclePartRepository vehiclePartRepository;
    private final PartInfoRepository partInfoRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleDeviceCertificatePublisher vehicleDeviceCertificatePublisher;
    private final ObjectProvider<CertEnrollmentTemplate> certEnrollmentTemplateProvider;

    /**
     * 申请设备证书
     *
     * @param cmd 申请命令
     * @return 申请结果
     */
    @Transactional(rollbackFor = Exception.class)
    public CertificateApplyResult applyDeviceCertificate(CertificateApplyCmd cmd) {
        log.info("申请设备证书: requestId={}, vin={}, deviceSn={}, profile={}", 
                cmd.getRequestId(), cmd.getVin(), cmd.getDeviceSn(), cmd.getCertificateProfile());

        // 1. 幂等检查：按request_id建立幂等占位
        VehicleCertificate existingCert = vehicleCertificateRepository.selectByRequestId(cmd.getRequestId());
        if (existingCert != null) {
            log.info("证书申请已存在，返回现有状态: requestId={}, status={}", 
                    cmd.getRequestId(), existingCert.getCertStatus());
            return buildApplyResult(existingCert);
        }

        // 2. 校验VIN和车辆状态
        validateVin(cmd.getVin());

        // 3. 校验设备实例及active vehicle_part绑定
        VehiclePart activeBinding = validateActiveBinding(cmd.getVin(), cmd.getDeviceSn(), cmd.getDeviceCategory());

        // 4. 解析CSR，校验CN=device_sn、签名有效性、Profile白名单及CSR不含VIN
        validateCsr(cmd.getCsrDerBase64(), cmd.getDeviceSn(), cmd.getVin(), cmd.getCertificateProfile());

        // 5. 计算CSR指纹
        String csrFingerprint = CsrUtils.calculateFingerprint(cmd.getCsrDerBase64());

        // 6. 创建证书记录（REQUESTED状态）
        VehicleCertificate certificate = VehicleCertificate.builder()
                .requestId(cmd.getRequestId())
                .vin(cmd.getVin())
                .bindingId(activeBinding.getId())
                .partId(activeBinding.getPartId())
                .deviceCategory(cmd.getDeviceCategory())
                .deviceSn(cmd.getDeviceSn())
                .certificateProfile(cmd.getCertificateProfile())
                .csrFingerprint(csrFingerprint)
                .certStatus(CertificateStatus.REQUESTED)
                .sourceSystem(cmd.getSourceSystem())
                .facilityNo(cmd.getFacilityNo())
                .lineCode(cmd.getLineCode())
                .build();
        vehicleCertificateRepository.insert(certificate);

        // 7. 调用framework-security CertificateEnrollmentTemplate.apply()提交申请
        try {
            CertApplyRequest frameworkRequest = new CertApplyRequest(
                    new CertificateProfile(cmd.getCertificateProfile(), cmd.getCertificateProfile(), CertificateProfile.SubjectType.DEVICE_IDENTITY, "RSA", "DIGITAL_SIGNATURE"),
                    Base64.getUrlDecoder().decode(cmd.getCsrDerBase64()),
                    new SubjectRef(SubjectRef.SubjectType.DEVICE_SN, cmd.getDeviceSn()),
                    cmd.getRequestId(),
                    null
            );

            net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult frameworkResult = 
                    getCertEnrollmentTemplate().apply(frameworkRequest);

            // 8. 保存pki_request_id并映射状态
            certificate.setPkiRequestId(frameworkResult.requestId());
            if (frameworkResult.state() == EnrollmentState.ISSUED) {
                certificate.setCertStatus(CertificateStatus.ISSUING);
                // 立即获取证书
                queryAndProcessCertificate(certificate);
            } else if (frameworkResult.state() == EnrollmentState.REJECTED || frameworkResult.state() == EnrollmentState.FAILED) {
                certificate.setCertStatus(CertificateStatus.FAILED);
                certificate.setFailReason("PKI拒绝或失败: " + frameworkResult.state());
            } else {
                certificate.setCertStatus(CertificateStatus.ISSUING);
            }
            vehicleCertificateRepository.update(certificate);

            log.info("证书申请已提交: requestId={}, pkiRequestId={}, state={}", 
                    cmd.getRequestId(), certificate.getPkiRequestId(), frameworkResult.state());

        } catch (Exception e) {
            log.error("证书申请失败: requestId={}", cmd.getRequestId(), e);
            certificate.setCertStatus(CertificateStatus.FAILED);
            certificate.setFailReason(e.getMessage());
            vehicleCertificateRepository.update(certificate);
            throw e;
        }

        return buildApplyResult(certificate);
    }

    /**
     * 查询证书申请状态
     *
     * @param requestId 业务请求ID
     * @return 状态结果
     */
    public CertificateStatusResult queryCertificateStatus(String requestId) {
        log.info("查询证书状态: requestId={}", requestId);

        VehicleCertificate certificate = vehicleCertificateRepository.selectByRequestId(requestId);
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + requestId);
        }

        // 如果状态是ISSUING，尝试查询PKI状态
        if (CertificateStatus.ISSUING.equals(certificate.getCertStatus()) && certificate.getPkiRequestId() != null) {
            try {
                net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult status = 
                        getCertEnrollmentTemplate().getStatus(certificate.getPkiRequestId());
                if (status.state() == EnrollmentState.ISSUED) {
                    IssuedCertificate issuedCert = getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
                    updateCertificateFromIssued(certificate, issuedCert);
                }
            } catch (Exception e) {
                log.warn("查询PKI状态失败: requestId={}", requestId, e);
            }
        }

        return buildStatusResult(certificate);
    }

    /**
     * 确认证书安装
     *
     * @param cmd 确认命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmCertificateInstalled(CertificateConfirmCmd cmd) {
        log.info("确认证书安装: requestId={}, result={}", cmd.getRequestId(), cmd.getResult());

        VehicleCertificate certificate = vehicleCertificateRepository.selectByRequestId(cmd.getRequestId());
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + cmd.getRequestId());
        }

        // 校验状态：只有ISSUED_NOT_CONFIRMED状态才能确认
        if (!CertificateStatus.ISSUED_NOT_CONFIRMED.equals(certificate.getCertStatus())) {
            throw new IllegalStateException("证书状态不允许确认安装: " + certificate.getCertStatus());
        }

        // 校验安装对象是否匹配
        validateInstallConfirmation(certificate, cmd);

        // 更新状态
        if ("SUCCESS".equals(cmd.getResult())) {
            certificate.setCertStatus(CertificateStatus.ACTIVE);
            certificate.setConfirmedAt(LocalDateTime.now());
        } else {
            certificate.setCertStatus(CertificateStatus.INSTALL_FAILED);
            certificate.setFailReason(cmd.getFailReason());
        }

        vehicleCertificateRepository.update(certificate);

        // 发布证书绑定变化事件
        vehicleDeviceCertificatePublisher.publishCertificateChanged(certificate);

        log.info("证书安装确认完成: requestId={}, status={}", cmd.getRequestId(), certificate.getCertStatus());
    }

    /**
     * 根据VIN和设备类别获取活跃证书绑定
     *
     * @param vin            车架号
     * @param deviceCategory 设备类别
     * @return 证书信息
     */
    public VehicleCertificate getActiveCertificateBinding(String vin, String deviceCategory) {
        return vehicleCertificateRepository.selectActiveByVinAndDeviceCategory(vin, deviceCategory);
    }

    /**
     * 根据设备SN获取证书列表
     *
     * @param deviceSn 设备SN
     * @return 证书列表
     */
    public List<VehicleCertificate> getCertificatesByDevice(String deviceSn) {
        return vehicleCertificateRepository.selectByDeviceSn(deviceSn);
    }

    /**
     * 根据证书序列号获取证书
     *
     * @param certSn 证书序列号
     * @return 证书信息
     */
    public VehicleCertificate getCertificateBySerial(String certSn) {
        return vehicleCertificateRepository.selectByCertSn(certSn);
    }

    /**
     * 查询更新时间大于指定时间的证书列表（用于对账）
     *
     * @param updatedAfter 更新时间
     * @param limit        限制数量
     * @return 证书列表
     */
    public List<VehicleCertificate> listCertificateBindings(Instant updatedAfter, int limit) {
        return vehicleCertificateRepository.selectUpdatedAfter(updatedAfter, limit);
    }

    /**
     * 校验VIN和车辆状态
     */
    private void validateVin(String vin) {
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo = vehBasicInfoRepository.selectByVin(vin);
        if (basicInfo == null) {
            throw new IllegalArgumentException("VIN不存在: " + vin);
        }
        if (basicInfo.getEolTime() == null) {
            throw new IllegalStateException("车辆未下线，不允许申请证书: " + vin);
        }
    }

    /**
     * 校验设备实例及active vehicle_part绑定
     */
    private VehiclePart validateActiveBinding(String vin, String deviceSn, String deviceCategory) {
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo = partInfoRepository.selectBySn(deviceSn);
        if (partInfo == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceSn);
        }

        VehiclePart activeBinding = vehiclePartRepository.selectActiveByVinAndPartId(vin, partInfo.getId());
        if (activeBinding == null) {
            throw new IllegalStateException("设备与车辆未建立active绑定: vin=" + vin + ", deviceSn=" + deviceSn);
        }

        return activeBinding;
    }

    /**
     * 解析CSR，校验CN=device_sn、签名有效性、Profile白名单及CSR不含VIN
     */
    private void validateCsr(String csrDerBase64, String deviceSn, String vin, String certificateProfile) {
        String cn = CsrUtils.parseCommonName(csrDerBase64);
        if (!deviceSn.equals(cn)) {
            throw new IllegalStateException("CSR Subject CN与device_sn不一致: CN=" + cn + ", deviceSn=" + deviceSn);
        }

        if (CsrUtils.containsVin(csrDerBase64, vin)) {
            throw new IllegalStateException("CSR不应包含VIN");
        }

        if (!CsrUtils.verifySignature(csrDerBase64)) {
            throw new IllegalStateException("CSR签名无效");
        }

        validateCertificateProfile(certificateProfile);
    }

    /**
     * 校验证书Profile白名单
     */
    private void validateCertificateProfile(String certificateProfile) {
        if (!"TBOX_TSP_CLIENT".equals(certificateProfile)) {
            throw new IllegalStateException("证书Profile不允许: " + certificateProfile);
        }
    }

    /**
     * 校验安装确认对象是否匹配
     */
    private void validateInstallConfirmation(VehicleCertificate certificate, CertificateConfirmCmd cmd) {
        if (cmd.getVin() != null && !cmd.getVin().equals(certificate.getVin())) {
            throw new IllegalStateException("安装确认VIN不匹配");
        }
        if (cmd.getDeviceSn() != null && !cmd.getDeviceSn().equals(certificate.getDeviceSn())) {
            throw new IllegalStateException("安装确认设备SN不匹配");
        }
    }

    /**
     * 构建申请结果
     */
    private CertificateApplyResult buildApplyResult(VehicleCertificate certificate) {
        return CertificateApplyResult.builder()
                .requestId(certificate.getRequestId())
                .status(certificate.getCertStatus().name())
                .certSn(certificate.getCertSn())
                .pkiRequestId(certificate.getPkiRequestId())
                .failReason(certificate.getFailReason())
                .build();
    }

    /**
     * 构建状态结果
     */
    private CertificateStatusResult buildStatusResult(VehicleCertificate certificate) {
        return CertificateStatusResult.builder()
                .requestId(certificate.getRequestId())
                .status(certificate.getCertStatus().name())
                .certSn(certificate.getCertSn())
                .certificateFingerprint(certificate.getCertificateFingerprint())
                .notBefore(certificate.getNotBefore())
                .notAfter(certificate.getNotAfter())
                .issuedAt(certificate.getIssuedAt())
                .confirmedAt(certificate.getConfirmedAt())
                .failReason(certificate.getFailReason())
                .build();
    }

    /**
     * 从签发结果更新证书信息
     */
    private void updateCertificateFromIssued(VehicleCertificate certificate, IssuedCertificate issuedCert) {
        certificate.setCertSn(issuedCert.serialNumber());
        certificate.setCertificateFingerprint(issuedCert.sha256Fingerprint());
        certificate.setNotBefore(issuedCert.notBefore().atZone(ZoneId.systemDefault()).toLocalDateTime());
        certificate.setNotAfter(issuedCert.notAfter().atZone(ZoneId.systemDefault()).toLocalDateTime());
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        vehicleCertificateRepository.update(certificate);
    }

    /**
     * 查询并处理证书
     */
    private void queryAndProcessCertificate(VehicleCertificate certificate) {
        try {
            IssuedCertificate issuedCert = getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
            updateCertificateFromIssued(certificate, issuedCert);
        } catch (Exception e) {
            log.error("获取证书失败: requestId={}", certificate.getRequestId(), e);
        }
    }

    /**
     * 获取证书注册模板（PKI服务必须可用）
     */
    private CertEnrollmentTemplate getCertEnrollmentTemplate() {
        return Optional.ofNullable(certEnrollmentTemplateProvider.getIfAvailable())
                .orElseThrow(() -> new IllegalStateException("PKI服务未配置，请检查 crypto.pki.endpoint 配置"));
    }
}
