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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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
     * <p>
     * 本方法不开启数据库事务：先落 REQUESTED 占位行（幂等/防并发重复签发，承 F15），
     * 再在事务外同步调用 PKI 签发——避免数据库事务横跨慢网络 I/O，且 PKI 已出证后
     * 本地提交失败也不至于回滚丢行（孤儿证书最小化），失败留痕可查、同 requestId 重试幂等。
     *
     * @param cmd 申请命令
     * @return 申请结果
     */
    public CertificateApplyResult applyDeviceCertificate(CertificateApplyCmd cmd) {
        log.info("申请设备证书: requestId={}, vin={}, deviceSn={}, profile={}", 
                cmd.getRequestId(), cmd.getVin(), cmd.getDeviceSn(), cmd.getCertificateProfile());

        // 1. 幂等检查：按request_id建立幂等占位
        VehicleCertificate existingCert = vehicleCertificateRepository.selectByRequestId(cmd.getRequestId());
        if (existingCert != null) {
            log.info("证书申请已存在，返回现有状态: requestId={}, status={}", 
                    cmd.getRequestId(), existingCert.getCertStatus());
            return buildApplyResult(existingCert, null);
        }

        // 同步签发成功时持有的已签发证书（用于填充响应中的证书本体，不落库）
        IssuedCertificate issuedCert = null;

        // 2. 校验VIN和车辆状态
        validateVin(cmd.getVin());

        // 3. 校验设备实例及active vehicle_part绑定
        VehiclePart activeBinding = validateActiveBinding(cmd.getVin(), cmd.getDeviceSn(), cmd.getDeviceCategory());

        // 4. 解析CSR，校验CN=device_sn、签名有效性（PoP）及Profile白名单
        validateCsr(cmd.getCsrDerBase64(), cmd.getDeviceSn(), cmd.getCertificateProfile());

        // 5. 计算CSR指纹
        String csrFingerprint = CsrUtils.calculateFingerprint(cmd.getCsrDerBase64());

        // 5.1 幂等复用：不同 request_id 但相同 (device_sn, profile, csr_fingerprint) 命中有效结果则复用（F15）
        VehicleCertificate reused = vehicleCertificateRepository
                .selectByDeviceSnAndProfileAndCsrFingerprint(cmd.getDeviceSn(), cmd.getCertificateProfile(), csrFingerprint);
        if (reused != null && isReusableStatus(reused.getCertStatus())) {
            log.info("相同CSR的证书申请已存在，幂等复用: requestId={}, reusedRequestId={}, status={}",
                    cmd.getRequestId(), reused.getRequestId(), reused.getCertStatus());
            return buildApplyResult(reused, null);
        }

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
        try {
            vehicleCertificateRepository.insert(certificate);
        } catch (DuplicateKeyException e) {
            // 并发同 request_id 已插入占位行（uk_request_id 兜底），回读返回既有记录，防重复签发
            VehicleCertificate raced = vehicleCertificateRepository.selectByRequestId(cmd.getRequestId());
            if (raced != null) {
                log.info("并发重复申请命中占位行，返回既有记录: requestId={}", cmd.getRequestId());
                return buildApplyResult(raced, null);
            }
            throw e;
        }

        // 7. 调用framework-security CertificateEnrollmentTemplate.apply()提交申请
        try {
            // 密钥算法从 CSR 实际公钥推导（TBOX 为 ECDSA P-256），不硬编码 RSA
            String keyAlgorithm = CsrUtils.extractPublicKeyAlgorithm(cmd.getCsrDerBase64());
            CertApplyRequest frameworkRequest = new CertApplyRequest(
                    new CertificateProfile(cmd.getCertificateProfile(), cmd.getCertificateProfile(), CertificateProfile.SubjectType.DEVICE_IDENTITY, keyAlgorithm, "DIGITAL_SIGNATURE"),
                    // 与校验处共用同一解码入口，兼容 URL-safe 与标准 Base64
                    CsrUtils.decodeBase64(cmd.getCsrDerBase64()),
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
                // 立即获取证书（step-ca 同步签发常态路径），证书本体用于响应回传、不落库
                issuedCert = queryAndProcessCertificate(certificate);
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

        return buildApplyResult(certificate, issuedCert);
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

        // 同步签发成功时持有的已签发证书（用于填充响应中的证书本体，不落库）
        IssuedCertificate issuedCert = null;

        // 如果状态是ISSUING，尝试查询PKI状态并对账推进
        if (CertificateStatus.ISSUING.equals(certificate.getCertStatus()) && certificate.getPkiRequestId() != null) {
            try {
                net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult status = 
                        getCertEnrollmentTemplate().getStatus(certificate.getPkiRequestId());
                if (status.state() == EnrollmentState.ISSUED) {
                    issuedCert = getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
                    updateCertificateFromIssued(certificate, issuedCert);
                }
            } catch (Exception e) {
                log.warn("查询PKI状态失败: requestId={}", requestId, e);
            }
        }

        // 已签发但响应未携带证书本体（apply后重查/对账路径），经pki_request_id重取回填
        if (issuedCert == null && certificate.getPkiRequestId() != null && certificate.getCertSn() != null) {
            issuedCert = tryReFetchCertificate(certificate);
        }

        return buildStatusResult(certificate, issuedCert);
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
            // 落实 §3.1「同一 device_sn+certificate_profile 最多一条 ACTIVE」：
            // 先作废同设备同 Profile 的既有 ACTIVE，再激活新证书（同一事务，避免并行有效证书）
            vehicleCertificateRepository.supersedeActiveByDeviceSnAndProfile(
                    certificate.getDeviceSn(), certificate.getCertificateProfile(), certificate.getRequestId());
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
     * 判断证书状态是否可作为幂等复用的有效结果（F15：命中有效结果则复用）
     * <p>
     * 终态失败（FAILED / INSTALL_FAILED / SUPERSEDED / REVOKED / EXPIRED）不参与复用，
     * 允许以新 request_id 重新签发。
     */
    private boolean isReusableStatus(CertificateStatus status) {
        return status == CertificateStatus.REQUESTED
                || status == CertificateStatus.ISSUING
                || status == CertificateStatus.PENDING_RECONCILE
                || status == CertificateStatus.ISSUED_NOT_CONFIRMED
                || status == CertificateStatus.ACTIVE;
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
     * 解析CSR，校验CN=device_sn、签名有效性（PoP）及Profile白名单。
     * <p>证书/CSR 设计上不含 VIN（TBOX-SEC Identity Contract），故不再用 containsVin 作门禁；
     * VIN ↔ 设备绑定已由 {@link #validateActiveBinding} 完成（TBOX-SEC-DSN-CR-015 §1.1/§9.1）。
     */
    private void validateCsr(String csrDerBase64, String deviceSn, String certificateProfile) {
        // CN 一致性：CSR Subject CN 必须与设备身份一致（真实解析，不再返回 MOCK 值）
        String cn = CsrUtils.parseCommonName(csrDerBase64);
        if (!deviceSn.equals(cn)) {
            throw new IllegalStateException("CSR Subject CN与device_sn不一致: CN=" + cn + ", deviceSn=" + deviceSn);
        }

        // 持有性证明 PoP：验证 PKCS#10 自签名（使用 CSR 内嵌公钥），失败拒签
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
     * <p>
     * 证书本体不落库（设计约定 VMD 不作证书仓库），优先使用同步签发时内存中的
     * {@code IssuedCertificate} 填充响应；缺失时经 {@code pki_request_id} 向 PKI 重取回填。
     */
    private CertificateApplyResult buildApplyResult(VehicleCertificate certificate, IssuedCertificate issuedCert) {
        CertificateApplyResult.CertificateApplyResultBuilder builder = CertificateApplyResult.builder()
                .requestId(certificate.getRequestId())
                .status(certificate.getCertStatus().name())
                .certSn(certificate.getCertSn())
                .pkiRequestId(certificate.getPkiRequestId())
                .issuer(certificate.getIssuer())
                .fingerprint(certificate.getCertificateFingerprint())
                .notBefore(certificate.getNotBefore() != null ? certificate.getNotBefore().toString() : null)
                .notAfter(certificate.getNotAfter() != null ? certificate.getNotAfter().toString() : null)
                .failReason(certificate.getFailReason());

        IssuedCertificate resolved = issuedCert;
        if (resolved == null && certificate.getPkiRequestId() != null && certificate.getCertSn() != null) {
            resolved = tryReFetchCertificate(certificate);
        }
        if (resolved != null) {
            builder.certificateDerBase64(Base64.getEncoder().encodeToString(resolved.leafCertificate()));
            builder.chainDerBase64(toBase64Array(resolved.certificateChain()));
        }
        return builder.build();
    }

    /**
     * 构建状态结果
     * <p>
     * 证书本体不落库（设计约定 VMD 不作证书仓库），已签发时经 {@code pki_request_id} 重取回填。
     */
    private CertificateStatusResult buildStatusResult(VehicleCertificate certificate, IssuedCertificate issuedCert) {
        CertificateStatusResult.CertificateStatusResultBuilder builder = CertificateStatusResult.builder()
                .requestId(certificate.getRequestId())
                .status(certificate.getCertStatus().name())
                .certSn(certificate.getCertSn())
                .certificateFingerprint(certificate.getCertificateFingerprint())
                .issuer(certificate.getIssuer())
                .notBefore(certificate.getNotBefore())
                .notAfter(certificate.getNotAfter())
                .issuedAt(certificate.getIssuedAt())
                .confirmedAt(certificate.getConfirmedAt())
                .failReason(certificate.getFailReason());

        IssuedCertificate resolved = issuedCert;
        if (resolved == null && certificate.getPkiRequestId() != null && certificate.getCertSn() != null) {
            resolved = tryReFetchCertificate(certificate);
        }
        if (resolved != null) {
            builder.certificateDerBase64(Base64.getEncoder().encodeToString(resolved.leafCertificate()));
            builder.chainDerBase64(toBase64Array(resolved.certificateChain()));
        }
        return builder.build();
    }

    /**
     * 经 PKI 重取已签发证书，失败仅告警并返回 null（响应回退为仅含元数据）
     */
    private IssuedCertificate tryReFetchCertificate(VehicleCertificate certificate) {
        try {
            return getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
        } catch (Exception e) {
            log.warn("重取证书失败，仅返回元数据: requestId={}, pkiRequestId={}",
                    certificate.getRequestId(), certificate.getPkiRequestId(), e);
            return null;
        }
    }

    /**
     * DER 证书链转 Base64 字符串数组
     */
    private String[] toBase64Array(List<byte[]> derList) {
        if (derList == null || derList.isEmpty()) {
            return null;
        }
        return derList.stream()
                .map(der -> Base64.getEncoder().encodeToString(der))
                .toArray(String[]::new);
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
        // 从叶子证书 DER 解析 X.509 subject/issuer（设计要求登记），失败留空
        String[] issuerSubject = parseIssuerSubject(issuedCert.leafCertificate());
        certificate.setSubject(issuerSubject[0]);
        certificate.setIssuer(issuerSubject[1]);
        vehicleCertificateRepository.update(certificate);
    }

    /**
     * 解析叶子证书 DER 的 X.509 subject/issuer（RFC2253 形式），解析失败返回 null
     */
    private String[] parseIssuerSubject(byte[] leafDer) {
        String subject = null;
        String issuer = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(leafDer));
            subject = x509.getSubjectX500Principal().getName();
            issuer = x509.getIssuerX500Principal().getName();
        } catch (Exception e) {
            log.warn("解析证书X.509信息失败，subject/issuer留空", e);
        }
        return new String[]{subject, issuer};
    }

    /**
     * 查询并处理证书
     *
     * @return 已签发证书（获取失败返回 null），用于填充响应中的证书本体
     */
    private IssuedCertificate queryAndProcessCertificate(VehicleCertificate certificate) {
        try {
            IssuedCertificate issuedCert = getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
            updateCertificateFromIssued(certificate, issuedCert);
            return issuedCert;
        } catch (Exception e) {
            log.error("获取证书失败: requestId={}", certificate.getRequestId(), e);
            return null;
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
