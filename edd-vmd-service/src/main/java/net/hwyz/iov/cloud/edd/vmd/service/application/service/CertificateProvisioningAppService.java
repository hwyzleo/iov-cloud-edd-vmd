package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehicleDeviceCertificatePublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import cn.hutool.core.util.StrUtil;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.PkiOutcomeUnknownException;
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
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final CertificateIdentityValidator certificateIdentityValidator;
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

        // 3. 解析active TBOX绑定及权威HSM UID（VIN↔deviceSn active 绑定 + 芯片UID，CR-054）
        BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolve(
                cmd.getVin(), cmd.getDeviceSn(), cmd.getDeviceCategory());

        // 4. 统一身份校验：CSR解析/验签(PoP)、CN==hsm_uid、声明ecu_uid一致性、
        //    Subject/SAN 禁止项、Profile 白名单（CR-054，承 TBOX-SEC-DSN-CR-015 §3 固定顺序）
        ParsedCsr parsedCsr = certificateIdentityValidator.validate(
                cmd.getCsrDerBase64(), identity, cmd.getDeclaredEcuUid(), cmd.getCertificateProfile());

        // 5. 业务幂等复用：不同 request_id 但相同 (vin, hsm_uid, public_key_sha256, profile) 命中有效结果则复用（CR-015 §5）
        VehicleCertificate reused = vehicleCertificateRepository
                .selectByVinAndUidAndSpkiAndProfile(cmd.getVin(), identity.hsmUid(), parsedCsr.spkiSha256(), cmd.getCertificateProfile());
        if (reused != null && isReusableStatus(reused.getCertStatus())) {
            log.info("同身份同公钥的证书申请已存在，幂等复用: requestId={}, reusedRequestId={}, status={}",
                    cmd.getRequestId(), reused.getRequestId(), reused.getCertStatus());
            return buildApplyResult(reused, null);
        }

        // 5.1 换钥冲突：同 (vin, hsm_uid, profile) 已绑定其他公钥，须授权换钥，不得返回旧证书（CR-054 RD-054-5）
        VehicleCertificate keyConflict = vehicleCertificateRepository
                .selectKeyConflictByVinAndUidAndProfile(cmd.getVin(), identity.hsmUid(), parsedCsr.spkiSha256(), cmd.getCertificateProfile());
        if (keyConflict != null) {
            log.warn("同身份不同公钥，拒绝签发需授权换钥: requestId={}, vin={}, hsmUid={}, profile={}",
                    cmd.getRequestId(), cmd.getVin(), identity.hsmUid(), cmd.getCertificateProfile());
            throw new CertificateKeyConflictException(cmd.getVin(), identity.hsmUid(), cmd.getCertificateProfile());
        }

        // 6. 创建证书记录（REQUESTED状态）
        VehicleCertificate certificate = VehicleCertificate.builder()
                .requestId(cmd.getRequestId())
                .vin(cmd.getVin())
                .bindingId(identity.bindingId())
                .partId(identity.partId())
                .deviceCategory(cmd.getDeviceCategory())
                .deviceSn(cmd.getDeviceSn())
                .hsmUid(identity.hsmUid())
                .publicKeySha256(parsedCsr.spkiSha256())
                .certificateProfile(cmd.getCertificateProfile())
                .csrFingerprint(parsedCsr.csrFingerprint())
                .certStatus(CertificateStatus.REQUESTED)
                .sourceSystem(cmd.getSourceSystem())
                .originalRequestId(cmd.getOriginalRequestId())
                .compensationReason(cmd.getCompensationReason())
                .ticketNo(cmd.getTicketNo())
                .lastOperator(cmd.getOperatorId())
                .lastOperationAt(cmd.getOperatorId() != null ? LocalDateTime.now() : null)
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
            IssuedCertificate issued = applyWithUnknownHandling(certificate, cmd.getCsrDerBase64());
            if (issued != null) {
                issuedCert = issued;
            }
            log.info("证书申请已提交: requestId={}, pkiRequestId={}, status={}",
                    cmd.getRequestId(), certificate.getPkiRequestId(), certificate.getCertStatus());
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
     * 继续/对账已有证书申请（CR-053 / US-060）
     * <p>
     * 允许 REQUESTED / ISSUING / PENDING_RECONCILE / FAILED（FAILED 为失败重试）：
     * - FAILED 且提供 CSR（失败重试场景）：优先按原 request_id/idempotencyKey 重新 apply，
     *   旧 pki_request_id 已指向失败/拒绝的申请，重发覆盖之（不换键规避重复签发控制）；
     * - 已取得 pki_request_id（非 FAILED）：经 getStatus/getCertificate 对账推进（PKI 超时/结果未知场景）；
     * - 未取得 pki_request_id 且提供 CSR（人工补偿场景，操作员携带受信工位回读 CSR）：
     *   以原 request_id/idempotencyKey 重新 apply（不生成新键）；
     * - 无 CSR（CSR 全文不落库）：转 PENDING_RECONCILE，引导人工补申请。
     * <p>
     * 行锁（FOR UPDATE）互斥并发 reconcile；状态单调推进，终态禁止回退；重复操作幂等返回。
     *
     * @param requestId   业务请求ID（原幂等键）
     * @param csrDerBase64 CSR DER Base64（可选；无 pki_request_id 时用于按原键重发）
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @return 申请结果
     */
    @Transactional(rollbackFor = Exception.class)
    public CertificateApplyResult reconcile(String requestId, String csrDerBase64, String operatorId, String operatorName) {
        log.info("对账证书申请: requestId={}, operatorId={}", requestId, operatorId);

        VehicleCertificate origin = vehicleCertificateRepository.selectByRequestId(requestId);
        if (origin == null) {
            throw new IllegalArgumentException("证书申请不存在: " + requestId);
        }

        // 行锁并发互斥
        VehicleCertificate certificate = vehicleCertificateRepository.selectByIdForUpdate(origin.getId());
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + requestId);
        }

        // 状态门禁：REQUESTED / ISSUING / PENDING_RECONCILE 可对账，FAILED 可失败重试，其余终态禁止回退
        if (!isReconcileStatus(certificate.getCertStatus())) {
            throw new CertificateCompensationNotAllowedException(requestId, certificate.getCertStatus().name());
        }

        IssuedCertificate issuedCert = null;
        if (certificate.getCertStatus() == CertificateStatus.FAILED && StrUtil.isNotBlank(csrDerBase64)) {
            // FAILED 终态失败重试：操作员携带 CSR 时优先按原 requestId/idempotencyKey 重新 apply
            // （旧 pki_request_id 已指向失败/拒绝的申请，重发覆盖之，不换键规避重复签发控制）
            BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolve(
                    certificate.getVin(), certificate.getDeviceSn(), certificate.getDeviceCategory());
            ParsedCsr parsedCsr = certificateIdentityValidator.validate(
                    csrDerBase64, identity, null, certificate.getCertificateProfile());
            // 回填身份快照（存量行可能缺失）
            if (StrUtil.isBlank(certificate.getHsmUid())) {
                certificate.setHsmUid(identity.hsmUid());
            }
            if (StrUtil.isBlank(certificate.getPublicKeySha256())) {
                certificate.setPublicKeySha256(parsedCsr.spkiSha256());
            }
            issuedCert = applyWithUnknownHandling(certificate, csrDerBase64);
        } else if (StrUtil.isNotBlank(certificate.getPkiRequestId())) {
            // 有 pki_request_id：getStatus/getCertificate 对账推进
            try {
                net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult status =
                        getCertEnrollmentTemplate().getStatus(certificate.getPkiRequestId());
                if (status.state() == EnrollmentState.ISSUED) {
                    issuedCert = getCertEnrollmentTemplate().getCertificate(certificate.getPkiRequestId());
                    updateCertificateFromIssued(certificate, issuedCert);
                } else if (status.state() == EnrollmentState.REJECTED || status.state() == EnrollmentState.FAILED) {
                    certificate.setCertStatus(CertificateStatus.FAILED);
                    certificate.setFailReason("PKI拒绝或失败: " + status.state());
                    vehicleCertificateRepository.update(certificate);
                } else if (status.state() == EnrollmentState.UNKNOWN) {
                    // 结果未知（请求已发送、响应丢失）：禁止自动重签，转待对账（FW-SEC-DSN-CR-008 §6）
                    certificate.setCertStatus(CertificateStatus.PENDING_RECONCILE);
                    certificate.setFailReason("PKI结果未知: " + status.state());
                    vehicleCertificateRepository.update(certificate);
                } else {
                    certificate.setCertStatus(CertificateStatus.ISSUING);
                    vehicleCertificateRepository.update(certificate);
                }
            } catch (Exception e) {
                log.warn("对账查询PKI状态失败: requestId={}", requestId, e);
                certificate.setCertStatus(CertificateStatus.PENDING_RECONCILE);
                vehicleCertificateRepository.update(certificate);
            }
        } else if (StrUtil.isNotBlank(csrDerBase64)) {
            // 无 pki_request_id 且操作员提供 CSR：复用原 requestId/idempotencyKey 重新 apply
            BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolve(
                    certificate.getVin(), certificate.getDeviceSn(), certificate.getDeviceCategory());
            ParsedCsr parsedCsr = certificateIdentityValidator.validate(
                    csrDerBase64, identity, null, certificate.getCertificateProfile());
            // 回填身份快照（存量行可能缺失）
            if (StrUtil.isBlank(certificate.getHsmUid())) {
                certificate.setHsmUid(identity.hsmUid());
            }
            if (StrUtil.isBlank(certificate.getPublicKeySha256())) {
                certificate.setPublicKeySha256(parsedCsr.spkiSha256());
            }
            issuedCert = applyWithUnknownHandling(certificate, csrDerBase64);
        } else {
            // 无 pki_request_id 且无 CSR（CSR 全文不落库）：转待对账，引导人工补申请
            certificate.setCertStatus(CertificateStatus.PENDING_RECONCILE);
            vehicleCertificateRepository.update(certificate);
        }

        // 人工操作审计上下文（CR-053）
        certificate.setLastOperator(operatorId);
        certificate.setLastOperationAt(operatorId != null ? LocalDateTime.now() : null);
        vehicleCertificateRepository.update(certificate);

        log.info("证书申请对账完成: requestId={}, status={}", requestId, certificate.getCertStatus());
        return buildApplyResult(certificate, issuedCert);
    }

    /**
     * 判断证书状态是否允许对账/失败重试（CR-053：REQUESTED / ISSUING / PENDING_RECONCILE；BUG 修复：+FAILED）
     */
    private boolean isReconcileStatus(CertificateStatus status) {
        return status == CertificateStatus.REQUESTED
                || status == CertificateStatus.ISSUING
                || status == CertificateStatus.PENDING_RECONCILE
                || status == CertificateStatus.FAILED;
    }

    /**
     * 确认证书安装
     * <p>
     * CR-053（RD-053-6）扩展共享内核：OAPI 与 MPT 使用同一状态门禁和对象校验。
     * 允许 ISSUED_NOT_CONFIRMED / INSTALL_FAILED 进入确认；INSTALL_FAILED 仅允许在重新注入/校验后重试确认，
     * 终态不得回退。校验 requestId + certSn + deviceSn。
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

        // 校验状态：仅 ISSUED_NOT_CONFIRMED / INSTALL_FAILED 可进入补录（终态不得回退）
        if (!CertificateStatus.ISSUED_NOT_CONFIRMED.equals(certificate.getCertStatus())
                && !CertificateStatus.INSTALL_FAILED.equals(certificate.getCertStatus())) {
            throw new IllegalStateException("证书状态不允许确认安装: " + certificate.getCertStatus());
        }

        // 校验安装对象是否匹配（requestId + certSn + deviceSn，CR-053）
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

        // 人工补录审计上下文（MPT 写操作，CR-053）
        certificate.setCompensationReason(cmd.getReason());
        certificate.setTicketNo(cmd.getTicketNo());
        certificate.setLastOperator(cmd.getOperatorId());
        certificate.setLastOperationAt(cmd.getOperatorId() != null ? LocalDateTime.now() : null);

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
     * 校验安装确认对象是否匹配（CR-053：requestId + certSn + deviceSn）
     */
    private void validateInstallConfirmation(VehicleCertificate certificate, CertificateConfirmCmd cmd) {
        if (cmd.getVin() != null && !cmd.getVin().equals(certificate.getVin())) {
            throw new IllegalStateException("安装确认VIN不匹配");
        }
        if (cmd.getDeviceSn() != null && !cmd.getDeviceSn().equals(certificate.getDeviceSn())) {
            throw new IllegalStateException("安装确认设备SN不匹配");
        }
        if (cmd.getCertSn() != null && !cmd.getCertSn().equals(certificate.getCertSn())) {
            throw new IllegalStateException("安装确认证书序列号不匹配");
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
     * 调用framework CertificateEnrollmentTemplate.apply()并推进状态（CR-053 提取，供申请/对账复用）
     * <p>
     * 保存 pki_request_id 并映射状态；ISSUED（step-ca 同步签发常态路径）立即取证回传（不落库）。
     *
     * @param certificate  证书记录（须已落库）
     * @param csrDerBase64 CSR DER Base64
     * @return 已签发证书（获取失败返回 null），用于填充响应中的证书本体
     */
    private IssuedCertificate doFrameworkApply(VehicleCertificate certificate, String csrDerBase64) {
        // 密钥算法从 CSR 实际公钥推导（TBOX 为 ECDSA P-256），不硬编码 RSA
        String keyAlgorithm = CsrUtils.extractPublicKeyAlgorithm(csrDerBase64);
        CertApplyRequest frameworkRequest = new CertApplyRequest(
                new CertificateProfile(certificate.getCertificateProfile(), certificate.getCertificateProfile(),
                        CertificateProfile.SubjectType.DEVICE_IDENTITY, keyAlgorithm, "DIGITAL_SIGNATURE"),
                // 与校验处共用同一解码入口，兼容 URL-safe 与标准 Base64
                CsrUtils.decodeBase64(csrDerBase64),
                new SubjectRef(SubjectRef.SubjectType.DEVICE_UID, certificate.getHsmUid()),
                certificate.getRequestId(),
                null
        );

        net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult frameworkResult =
                getCertEnrollmentTemplate().apply(frameworkRequest);

        // 保存pki_request_id并映射状态
        certificate.setPkiRequestId(frameworkResult.requestId());
        IssuedCertificate issuedCert = null;
        if (frameworkResult.state() == EnrollmentState.ISSUED) {
            certificate.setCertStatus(CertificateStatus.ISSUING);
            // 立即获取证书（step-ca 同步签发常态路径），证书本体用于响应回传、不落库
            issuedCert = queryAndProcessCertificate(certificate);
        } else if (frameworkResult.state() == EnrollmentState.REJECTED || frameworkResult.state() == EnrollmentState.FAILED) {
            certificate.setCertStatus(CertificateStatus.FAILED);
            certificate.setFailReason("PKI拒绝或失败: " + frameworkResult.state());
        } else if (frameworkResult.state() == EnrollmentState.UNKNOWN) {
            // 结果未知（请求已发送、响应丢失）：禁止自动重签，转待对账（FW-SEC-DSN-CR-008 §6）
            certificate.setCertStatus(CertificateStatus.PENDING_RECONCILE);
            certificate.setFailReason("PKI结果未知: " + frameworkResult.state());
        } else {
            certificate.setCertStatus(CertificateStatus.ISSUING);
        }
        vehicleCertificateRepository.update(certificate);
        return issuedCert;
    }

    /**
     * 统一处理证书申请中的「结果未知」：framework 抛 {@link PkiOutcomeUnknownException}
     * 或 apply 返回 UNKNOWN 状态时（请求已发送但响应丢失，FW-SEC-DSN-CR-008 §6），
     * 禁止自动重签，转 PENDING_RECONCILE 引导运维受控恢复；不向上抛出（避免事务回滚丢状态）。
     *
     * @return 已签发证书（结果未知返回 null），用于填充响应中的证书本体
     */
    private IssuedCertificate applyWithUnknownHandling(VehicleCertificate certificate, String csrDerBase64) {
        try {
            return doFrameworkApply(certificate, csrDerBase64);
        } catch (PkiOutcomeUnknownException e) {
            log.warn("证书申请结果未知，转待对账: requestId={}", certificate.getRequestId(), e);
            certificate.setCertStatus(CertificateStatus.PENDING_RECONCILE);
            certificate.setFailReason(e.getMessage());
            vehicleCertificateRepository.update(certificate);
            return null;
        }
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
