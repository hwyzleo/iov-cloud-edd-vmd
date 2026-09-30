package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CompensateCertificateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleCertificateQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateCompensateResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateDetailResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateListResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationReasonRequiredException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateIssuanceConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateRequestIdempotencyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificateOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 证书人工补偿应用服务（MPT，CR-053 / US-060）
 * <p>
 * 仅编排后台用例、权限上下文、人工原因与审计；实际签发、状态推进统一委托
 * {@link CertificateProvisioningAppService}，不复制 PKI 调用逻辑。
 * <p>
 * 分流：①已有 REQUESTED/ISSUING/PENDING_RECONCILE 记录按原 request_id/idempotencyKey 对账；
 * ②MES 请求未达 VMD 时，基于 TBOX 已生成 CSR 创建 source_system=MPT_COMPENSATION 的人工申请。
 * 后台不生成设备密钥或 CSR；所有写动作复用 active 绑定、CSR/CN、Profile、幂等与 PKI 策略校验。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateCompensationAppService {

    /** 人工补偿来源系统标识 */
    public static final String SOURCE_MPT_COMPENSATION = "MPT_COMPENSATION";

    /** MPT 服务端生成请求号前缀 */
    private static final String MPT_REQUEST_PREFIX = "MPT-CERT-";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CertificateProvisioningAppService certificateProvisioningAppService;
    private final VehicleCertificateRepository vehicleCertificateRepository;
    private final VehicleCertificateOperationRepository vehicleCertificateOperationRepository;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final CertificateIdentityValidator certificateIdentityValidator;

    /**
     * 人工补申请（MES 请求未达 VMD）
     * <p>
     * 规则（设计 §2.2）：
     * 1. requestId 已存在 → 比对 VIN/设备/Profile/CSR 指纹，一致幂等返回（或携 CSR 继续签发），不一致 806061；
     * 2. requestId 不存在但 originalMesRequestId 命中 → 返回已有记录，引导 reconcile；
     * 3. 均不存在 → 服务端生成 MPT-CERT-{yyyyMMdd}-{唯一}，写 source_system=MPT_COMPENSATION 后调用既有申请内核；
     * 4. 按 device_sn + profile 查有效/处理中记录，CSR 摘要不同拒绝（806062），相同幂等返回；
     *    FAILED 终态失败记录命中同摘要时按原 requestId 重新签发（失败重试，BUG 修复），不幂等返回。
     *
     * @param cmd 补偿命令
     * @return 补偿结果（含 sourceSystem / reusedExisting / nextAction）
     */
    public CertificateCompensateResult compensate(CompensateCertificateCmd cmd) {
        log.info("人工补偿证书申请: vin={}, deviceSn={}, profile={}, operatorId={}",
                cmd.getVin(), cmd.getDeviceSn(), cmd.getCertificateProfile(), cmd.getOperatorId());

        // 0. 必填人工原因
        if (StrUtil.isBlank(cmd.getReason())) {
            throw new CertificateCompensationReasonRequiredException();
        }

        // 1. requestId 已存在 → 幂等比对
        if (StrUtil.isNotBlank(cmd.getRequestId())) {
            VehicleCertificate existing = vehicleCertificateRepository.selectByRequestId(cmd.getRequestId());
            if (existing != null) {
                String csrFingerprint = CsrUtils.calculateFingerprint(cmd.getCsrDerBase64());
                if (!sameSummary(existing, cmd, csrFingerprint)) {
                    auditFailed(cmd, "COMPENSATE", existing.getCertStatus(),
                            VmdErrorCode.CERTIFICATE_REQUEST_IDEMPOTENCY_CONFLICT.getCode());
                    throw new CertificateRequestIdempotencyConflictException(cmd.getRequestId());
                }
                // 同摘要：已取得 pki 或已达可确认/终态 → 幂等返回；REQUESTED/PENDING_RECONCILE 且无 pki → 携 CSR 按原键继续签发
                return handleExistingSameSummary(existing, cmd);
            }
        }

        // 2. originalMesRequestId 已存在 → 拒绝创建新申请，返回已有记录引导 reconcile
        if (StrUtil.isNotBlank(cmd.getOriginalMesRequestId())) {
            VehicleCertificate byOriginal = vehicleCertificateRepository.selectByOriginalRequestId(cmd.getOriginalMesRequestId());
            if (byOriginal != null) {
                CertificateCompensateResult result = buildCompensateResult(byOriginal, true, "RECONCILE");
                appendAudit(cmd, "COMPENSATE", byOriginal.getCertStatus(), byOriginal.getCertStatus(),
                        "IDEMPOTENT_HIT", null, null, result.getRequestId());
                log.info("原请求号[{}]已存在，引导对账: requestId={}", cmd.getOriginalMesRequestId(), byOriginal.getRequestId());
                return result;
            }
        }

        // 3. 业务去重/防重：同 (vin, hsm_uid, public_key_sha256, profile) 有效或处理中（CR-054/CR-015 §5）
        //    先解析 active 绑定权威 UID 并通过统一身份校验，避免补偿绕过 CN/绑定门禁
        BoundDeviceIdentity identity = boundDeviceIdentityResolver.resolve(
                cmd.getVin(), cmd.getDeviceSn(), cmd.getDeviceCategory());
        ParsedCsr parsedCsr = certificateIdentityValidator.validate(
                cmd.getCsrDerBase64(), identity, null, cmd.getCertificateProfile());

        VehicleCertificate inProgress = vehicleCertificateRepository
                .selectByVinAndUidAndSpkiAndProfile(cmd.getVin(), identity.hsmUid(), parsedCsr.spkiSha256(), cmd.getCertificateProfile());
        if (inProgress != null) {
            if (sameSummary(inProgress, cmd, parsedCsr.csrFingerprint())) {
                // 相同业务键且摘要一致 → 幂等返回（或携 CSR 继续签发）
                return handleExistingSameSummary(inProgress, cmd);
            }
            auditFailed(cmd, "COMPENSATE", inProgress.getCertStatus(),
                    VmdErrorCode.CERTIFICATE_ISSUANCE_CONFLICT.getCode());
            throw new CertificateIssuanceConflictException(cmd.getDeviceSn(), cmd.getCertificateProfile());
        }

        // 3.1 换钥冲突：同 (vin, hsm_uid, profile) 已绑定其他公钥，普通补偿拒绝，须授权换钥（CR-054 RD-054-5）
        VehicleCertificate keyConflict = vehicleCertificateRepository
                .selectKeyConflictByVinAndUidAndProfile(cmd.getVin(), identity.hsmUid(), parsedCsr.spkiSha256(), cmd.getCertificateProfile());
        if (keyConflict != null) {
            auditFailed(cmd, "COMPENSATE", keyConflict.getCertStatus(),
                    VmdErrorCode.CERTIFICATE_KEY_CONFLICT.getCode());
            throw new CertificateKeyConflictException(cmd.getVin(), identity.hsmUid(), cmd.getCertificateProfile());
        }

        // 4. 确定 requestId：调用方提供且不存在时原样使用（原 MES 请求号或人工补申请号，承设计 §2.2）；
        //    未提供时由服务端生成 MPT-CERT-{yyyyMMdd}-{唯一}，写 source_system=MPT_COMPENSATION 后调用既有申请内核
        String requestId = StrUtil.isNotBlank(cmd.getRequestId()) ? cmd.getRequestId() : generateMptRequestId();
        CertificateApplyResult applyResult;
        try {
            CertificateApplyCmd applyCmd = buildApplyCmd(cmd, requestId);
            applyResult = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);
        } catch (DuplicateKeyException e) {
            // 并发双击：同业务键（vin+uid+spki+profile）已插入，回读既有记录幂等返回
            VehicleCertificate raced = vehicleCertificateRepository
                    .selectByVinAndUidAndSpkiAndProfile(cmd.getVin(), identity.hsmUid(), parsedCsr.spkiSha256(), cmd.getCertificateProfile());
            if (raced != null) {
                log.info("并发双击命中既有记录，幂等返回: requestId={}", raced.getRequestId());
                return buildCompensateResult(raced, true, nextActionFor(raced));
            }
            throw e;
        }

        VehicleCertificate saved = vehicleCertificateRepository.selectByRequestId(requestId);
        CertificateCompensateResult result = buildCompensateResult(saved, false, nextActionFor(saved));
        fillCertificateBody(result, applyResult);
        appendAudit(cmd, "COMPENSATE", null, saved.getCertStatus(), "SUCCESS", null, requestId, requestId);
        log.info("人工补申请完成: requestId={}, status={}", requestId, saved.getCertStatus());
        return result;
    }

    /**
     * 分页查询证书申请记录（MPT，CR-053）
     *
     * @param query 查询条件
     * @return 证书列表结果
     */
    public List<CertificateListResult> search(VehicleCertificateQuery query) {
        Map<String, Object> map = new HashMap<>();
        if (query.getRequestId() != null && !query.getRequestId().isEmpty()) {
            map.put("requestId", query.getRequestId());
        }
        if (query.getVin() != null && !query.getVin().isEmpty()) {
            map.put("vin", query.getVin());
        }
        if (query.getDeviceSn() != null && !query.getDeviceSn().isEmpty()) {
            map.put("deviceSn", query.getDeviceSn());
        }
        if (query.getCertSn() != null && !query.getCertSn().isEmpty()) {
            map.put("certSn", query.getCertSn());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            map.put("certStatus", query.getStatus());
        }
        if (query.getSource() != null && !query.getSource().isEmpty()) {
            map.put("sourceSystem", query.getSource());
        }
        if (query.getBeginTime() != null) {
            map.put("beginTime", query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            map.put("endTime", query.getEndTime());
        }
        List<VehicleCertificate> certificates = vehicleCertificateRepository.selectByMap(map);
        List<CertificateListResult> results = new ArrayList<>();
        for (VehicleCertificate cert : certificates) {
            results.add(toListResult(cert));
        }
        return results;
    }

    /**
     * 查询证书申请详情与脱敏操作审计时间线（MPT，CR-053）
     *
     * @param id 证书记录主键
     * @return 证书详情结果
     */
    public CertificateDetailResult getDetail(Long id) {
        VehicleCertificate cert = vehicleCertificateRepository.selectById(id);
        if (cert == null) {
            throw new IllegalArgumentException("证书申请不存在: " + id);
        }
        List<CertificateOperationResult> operations = new ArrayList<>();
        for (VehicleCertificateOperation op : vehicleCertificateOperationRepository.selectByRequestId(cert.getRequestId())) {
            operations.add(CertificateOperationResult.builder()
                    .action(op.getAction())
                    .operatorName(op.getOperatorName())
                    .reason(op.getReason())
                    .ticketNo(op.getTicketNo())
                    .beforeStatus(op.getBeforeStatus())
                    .afterStatus(op.getAfterStatus())
                    .result(op.getResult())
                    .occurredAt(op.getOccurredAt())
                    .build());
        }
        return CertificateDetailResult.builder()
                .id(cert.getId())
                .requestId(cert.getRequestId())
                .originalRequestId(cert.getOriginalRequestId())
                .pkiRequestId(cert.getPkiRequestId())
                .vin(cert.getVin())
                .deviceCategory(cert.getDeviceCategory())
                .deviceSn(cert.getDeviceSn())
                .certificateProfile(cert.getCertificateProfile())
                .csrFingerprint(cert.getCsrFingerprint())
                .subject(cert.getSubject())
                .issuer(cert.getIssuer())
                .certSn(cert.getCertSn())
                .status(cert.getCertStatus() != null ? cert.getCertStatus().name() : null)
                .sourceSystem(cert.getSourceSystem())
                .certificateFingerprint(cert.getCertificateFingerprint())
                .notBefore(cert.getNotBefore())
                .notAfter(cert.getNotAfter())
                .issuedAt(cert.getIssuedAt())
                .confirmedAt(cert.getConfirmedAt())
                .compensationReason(cert.getCompensationReason())
                .ticketNo(cert.getTicketNo())
                .lastOperator(cert.getLastOperator())
                .lastOperationAt(cert.getLastOperationAt())
                .failReason(cert.getFailReason())
                .createTime(instantToLocalDateTime(cert.getCreateTime()))
                .modifyTime(instantToLocalDateTime(cert.getModifyTime()))
                .operations(operations)
                .build();
    }

    /**
     * 构建列表结果
     */
    private CertificateListResult toListResult(VehicleCertificate cert) {
        return CertificateListResult.builder()
                .id(cert.getId())
                .requestId(cert.getRequestId())
                .originalRequestId(cert.getOriginalRequestId())
                .vin(cert.getVin())
                .deviceCategory(cert.getDeviceCategory())
                .deviceSn(cert.getDeviceSn())
                .certificateProfile(cert.getCertificateProfile())
                .certSn(cert.getCertSn())
                .status(cert.getCertStatus() != null ? cert.getCertStatus().name() : null)
                .sourceSystem(cert.getSourceSystem())
                .certificateFingerprint(cert.getCertificateFingerprint())
                .notBefore(cert.getNotBefore())
                .notAfter(cert.getNotAfter())
                .issuedAt(cert.getIssuedAt())
                .confirmedAt(cert.getConfirmedAt())
                .lastOperator(cert.getLastOperator())
                .lastOperationAt(cert.getLastOperationAt())
                .failReason(cert.getFailReason())
                .modifyTime(instantToLocalDateTime(cert.getModifyTime()))
                .build();
    }

    /**
     * Instant 转 LocalDateTime（null 安全）
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    /**
     * 获取已签发证书本体（MPT，只读，按记录ID）
     * <p>
     * 证书本体（DER/证书链）VMD 不落库，仅在 framework 结果存储 TTL 内可经 {@code pki_request_id} 重取。
     * 本方法只读：仅 ISSUED_NOT_CONFIRMED / ACTIVE 可获取，复用
     * {@link CertificateProvisioningAppService#queryCertificateStatus} 重取本体，不做任何状态变更。
     * 供产线/售后在签发后再次获取证书本体手动注入设备（设计 nextAction=QUERY）。
     *
     * @param id           证书记录主键
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @param sourceIp     来源IP
     * @param userAgent    终端User-Agent
     * @return 补偿结果（含证书本体 DER/链）
     */
    public CertificateCompensateResult queryCertificate(Long id, String operatorId, String operatorName,
                                                        String sourceIp, String userAgent) {
        VehicleCertificate certificate = vehicleCertificateRepository.selectById(id);
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + id);
        }
        // 只读门禁：仅已签发未确认/已激活可获取证书本体，其余状态无本体可取
        if (certificate.getCertStatus() != CertificateStatus.ISSUED_NOT_CONFIRMED
                && certificate.getCertStatus() != CertificateStatus.ACTIVE) {
            throw new IllegalStateException("证书状态不支持获取证书本体: "
                    + (certificate.getCertStatus() != null ? certificate.getCertStatus().name() : null));
        }

        CertificateStatusResult status = certificateProvisioningAppService.queryCertificateStatus(certificate.getRequestId());
        CertificateCompensateResult result = buildCompensateResult(certificate, true, nextActionFor(certificate));
        if (status != null) {
            result.setCertificateDerBase64(status.getCertificateDerBase64());
            result.setChainDerBase64(status.getChainDerBase64());
        }
        appendAudit(cmdOf(null, operatorId, operatorName, sourceIp, userAgent, certificate, null),
                "QUERY", certificate.getCertStatus(), certificate.getCertStatus(),
                "SUCCESS", null, certificate.getOriginalRequestId(), certificate.getRequestId());
        boolean hasBody = status != null && status.getCertificateDerBase64() != null;
        log.info("获取证书本体: requestId={}, status={}, hasBody={}",
                certificate.getRequestId(), certificate.getCertStatus(), hasBody);
        return result;
    }

    /**
     * 继续/对账已有申请（MPT，按记录ID）
     *
     * @param id           证书记录主键
     * @param reason       人工原因（可空，进入审计）
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @param sourceIp     来源IP
     * @param userAgent    终端User-Agent
     * @return 补偿结果
     */
    public CertificateCompensateResult reconcile(Long id, String reason, String operatorId,
                                                 String operatorName, String sourceIp, String userAgent) {
        VehicleCertificate certificate = vehicleCertificateRepository.selectById(id);
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + id);
        }
        CertificateStatus before = certificate.getCertStatus();
        CertificateApplyResult applyResult = certificateProvisioningAppService.reconcile(
                certificate.getRequestId(), null, operatorId, operatorName);
        VehicleCertificate updated = vehicleCertificateRepository.selectByRequestId(certificate.getRequestId());

        String resultCode = updated != null && updated.getCertStatus() != null
                && updated.getCertStatus() == before ? "PENDING" : "SUCCESS";
        CertificateCompensateResult result = buildCompensateResult(updated, false, nextActionFor(updated));
        fillCertificateBody(result, applyResult);
        appendAudit(cmdOf(reason, operatorId, operatorName, sourceIp, userAgent, certificate, null),
                "RECONCILE", before, updated != null ? updated.getCertStatus() : null, resultCode, null,
                certificate.getOriginalRequestId(), certificate.getRequestId());
        log.info("证书申请对账完成: requestId={}, from={}, to={}", certificate.getRequestId(), before, updated != null ? updated.getCertStatus() : null);
        return result;
    }

    /**
     * 安装结果补录（MPT，按记录ID）
     * <p>
     * 必填 reason + ticketNo；校验 requestId + certSn + deviceSn 后复用共享确认内核；
     * 仅 ISSUED_NOT_CONFIRMED / INSTALL_FAILED 可进入，终态禁止回退。
     *
     * @param id           证书记录主键
     * @param certSn       证书序列号（对象校验）
     * @param deviceSn     设备SN（对象校验）
     * @param result       安装结果 SUCCESS/FAILED
     * @param failReason   失败原因
     * @param reason       人工原因（必填）
     * @param ticketNo     工单号（必填）
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @param sourceIp     来源IP
     * @param userAgent    终端User-Agent
     * @return 补偿结果
     */
    public CertificateCompensateResult confirmInstalled(Long id, String certSn, String deviceSn, String result,
                                                        String failReason, String reason, String ticketNo,
                                                        String operatorId, String operatorName, String sourceIp, String userAgent) {
        if (StrUtil.isBlank(reason) || StrUtil.isBlank(ticketNo)) {
            throw new CertificateCompensationReasonRequiredException();
        }
        VehicleCertificate certificate = vehicleCertificateRepository.selectById(id);
        if (certificate == null) {
            throw new IllegalArgumentException("证书申请不存在: " + id);
        }
        CertificateStatus before = certificate.getCertStatus();

        CertificateConfirmCmd cmd = CertificateConfirmCmd.builder()
                .requestId(certificate.getRequestId())
                .result(result)
                .failReason(failReason)
                .certSn(certSn)
                .deviceSn(deviceSn)
                .reason(reason)
                .ticketNo(ticketNo)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build();
        certificateProvisioningAppService.confirmCertificateInstalled(cmd);

        VehicleCertificate updated = vehicleCertificateRepository.selectByRequestId(certificate.getRequestId());
        CertificateCompensateResult compensateResult = buildCompensateResult(updated, false, "NONE");
        appendAudit(cmdOf(reason, operatorId, operatorName, sourceIp, userAgent, certificate, ticketNo),
                "CONFIRM_INSTALLED", before, updated != null ? updated.getCertStatus() : null,
                "SUCCESS", null, certificate.getOriginalRequestId(), certificate.getRequestId());
        log.info("证书安装结果补录完成: requestId={}, from={}, to={}", certificate.getRequestId(), before, updated != null ? updated.getCertStatus() : null);
        return compensateResult;
    }

    /**
     * 命中同摘要既有申请的处理：
     * - 已取得 pki_request_id 或已达 ISSUED_NOT_CONFIRMED/ACTIVE：幂等返回既有状态；
     * - REQUESTED/PENDING_RECONCILE 且无 pki_request_id（操作员携带 CSR）：以原 request_id/idempotencyKey 继续签发；
     * - FAILED（终态失败，操作员携带 CSR）：同样以原 request_id/idempotencyKey 重新签发（失败重试，BUG 修复），
     *   而非幂等返回 FAILED——避免失败后无法用同一 CSR 重试的死路。
     */
    private CertificateCompensateResult handleExistingSameSummary(VehicleCertificate existing, CompensateCertificateCmd cmd) {
        boolean canContinue = (existing.getCertStatus() == CertificateStatus.REQUESTED
                || existing.getCertStatus() == CertificateStatus.PENDING_RECONCILE)
                && StrUtil.isBlank(existing.getPkiRequestId())
                || existing.getCertStatus() == CertificateStatus.FAILED;
        if (canContinue) {
            CertificateApplyResult applyResult = certificateProvisioningAppService.reconcile(
                    existing.getRequestId(), cmd.getCsrDerBase64(), cmd.getOperatorId(), cmd.getOperatorName());
            VehicleCertificate updated = vehicleCertificateRepository.selectByRequestId(existing.getRequestId());
            CertificateCompensateResult result = buildCompensateResult(updated, false, nextActionFor(updated));
            fillCertificateBody(result, applyResult);
            appendAudit(cmd, "COMPENSATE", existing.getCertStatus(), updated != null ? updated.getCertStatus() : null,
                    "SUCCESS", null, updated != null ? updated.getRequestId() : existing.getRequestId(), updated != null ? updated.getRequestId() : existing.getRequestId());
            log.info("同摘要既有处理中申请按原键继续签发: requestId={}, to={}", existing.getRequestId(), updated != null ? updated.getCertStatus() : null);
            return result;
        }
        CertificateCompensateResult result = buildCompensateResult(existing, true, nextActionFor(existing));
        appendAudit(cmd, "COMPENSATE", existing.getCertStatus(), existing.getCertStatus(),
                "IDEMPOTENT_HIT", null, existing.getOriginalRequestId(), existing.getRequestId());
        log.info("同摘要既有申请幂等返回: requestId={}, status={}", existing.getRequestId(), existing.getCertStatus());
        return result;
    }

    /**
     * 构建申请内核命令
     */
    private CertificateApplyCmd buildApplyCmd(CompensateCertificateCmd cmd, String requestId) {
        return CertificateApplyCmd.builder()
                .requestId(requestId)
                .vin(cmd.getVin())
                .deviceCategory(cmd.getDeviceCategory())
                .deviceSn(cmd.getDeviceSn())
                .certificateProfile(cmd.getCertificateProfile())
                .csrDerBase64(cmd.getCsrDerBase64())
                .sourceSystem(SOURCE_MPT_COMPENSATION)
                .originalRequestId(cmd.getOriginalMesRequestId())
                .compensationReason(cmd.getReason())
                .ticketNo(cmd.getTicketNo())
                .operatorId(cmd.getOperatorId())
                .operatorName(cmd.getOperatorName())
                .facilityNo(cmd.getFacilityNo())
                .lineCode(cmd.getLineCode())
                .build();
    }

    /**
     * 请求摘要比对：VIN、设备类别、设备SN、Profile、CSR指纹 全部一致
     */
    private boolean sameSummary(VehicleCertificate cert, CompensateCertificateCmd cmd, String csrFingerprint) {
        return StrUtil.equals(cert.getVin(), cmd.getVin())
                && StrUtil.equals(cert.getDeviceCategory(), cmd.getDeviceCategory())
                && StrUtil.equals(cert.getDeviceSn(), cmd.getDeviceSn())
                && StrUtil.equals(cert.getCertificateProfile(), cmd.getCertificateProfile())
                && StrUtil.equals(cert.getCsrFingerprint(), csrFingerprint);
    }

    /**
     * 服务端生成 MPT 请求号（幂等键）：MPT-CERT-{yyyyMMdd}-{8位唯一}。
     * <p>
     * 设计原文为 MPT-CERT-{yyyyMMdd}-{id}，因 request_id 需在落库/调 PKI 前确定而自增 id 未知，
     * 故以 UUID 前 8 位作为唯一后缀，保证唯一且稳定。
     */
    private String generateMptRequestId() {
        return MPT_REQUEST_PREFIX + LocalDateTime.now().format(DATE_FORMAT) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 根据证书状态推导下一步动作
     */
    private String nextActionFor(VehicleCertificate cert) {
        if (cert == null || cert.getCertStatus() == null) {
            return "NONE";
        }
        if (cert.getCertStatus() == CertificateStatus.REQUESTED
                || cert.getCertStatus() == CertificateStatus.ISSUING
                || cert.getCertStatus() == CertificateStatus.PENDING_RECONCILE) {
            return "RECONCILE";
        }
        if (cert.getCertStatus() == CertificateStatus.ISSUED_NOT_CONFIRMED
                || cert.getCertStatus() == CertificateStatus.ACTIVE) {
            return "QUERY";
        }
        return "NONE";
    }

    /**
     * 将已签发证书本体（DER/证书链）从申请结果回填到补偿结果，供产线/售后手动注入设备。
     * <p>
     * 证书本体为公开信息（不含私钥），VMD 不长期持久化，仅在签发/对账取得时透传。
     *
     * @param result      补偿结果
     * @param applyResult 申请内核结果（可能为空）
     */
    private void fillCertificateBody(CertificateCompensateResult result, CertificateApplyResult applyResult) {
        if (result != null && applyResult != null) {
            result.setCertificateDerBase64(applyResult.getCertificateDerBase64());
            result.setChainDerBase64(applyResult.getChainDerBase64());
        }
    }

    /**
     * 从证书记录构建补偿结果
     */
    private CertificateCompensateResult buildCompensateResult(VehicleCertificate cert, boolean reusedExisting, String nextAction) {
        if (cert == null) {
            return CertificateCompensateResult.builder().reusedExisting(reusedExisting).nextAction(nextAction).build();
        }
        return CertificateCompensateResult.builder()
                .requestId(cert.getRequestId())
                .status(cert.getCertStatus() != null ? cert.getCertStatus().name() : null)
                .certSn(cert.getCertSn())
                .pkiRequestId(cert.getPkiRequestId())
                .failReason(cert.getFailReason())
                .issuer(cert.getIssuer())
                .fingerprint(cert.getCertificateFingerprint())
                .notBefore(cert.getNotBefore() != null ? cert.getNotBefore().toString() : null)
                .notAfter(cert.getNotAfter() != null ? cert.getNotAfter().toString() : null)
                .sourceSystem(cert.getSourceSystem())
                .reusedExisting(reusedExisting)
                .nextAction(nextAction)
                .build();
    }

    /**
     * 写人工操作审计（正常路径）
     */
    private void appendAudit(CompensateCertificateCmd cmd, String action, CertificateStatus before,
                             CertificateStatus after, String result, String errorCode, String originalRequestId, String requestId) {
        try {
            VehicleCertificateOperation operation = VehicleCertificateOperation.builder()
                    .operationId(UUID.randomUUID().toString())
                    .requestId(requestId)
                    .action(action)
                    .operatorId(cmd != null ? cmd.getOperatorId() : null)
                    .operatorName(cmd != null ? cmd.getOperatorName() : null)
                    .reason(cmd != null ? cmd.getReason() : null)
                    .ticketNo(cmd != null ? cmd.getTicketNo() : null)
                    .originalRequestId(originalRequestId)
                    .beforeStatus(before != null ? before.name() : null)
                    .afterStatus(after != null ? after.name() : null)
                    .requestDigest(cmd != null ? buildRequestDigest(cmd) : null)
                    .result(result)
                    .errorCode(errorCode)
                    .sourceIp(cmd != null ? cmd.getSourceIp() : null)
                    .userAgent(cmd != null ? cmd.getUserAgent() : null)
                    .occurredAt(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();
            vehicleCertificateOperationRepository.insert(operation);
        } catch (Exception e) {
            log.warn("证书人工操作审计写入失败: requestId={}, action={}", requestId, action, e);
        }
    }

    /**
     * 写人工操作审计（reconcile / confirm 路径）
     */
    private CompensateCertificateCmd cmdOf(String reason, String operatorId, String operatorName,
                                           String sourceIp, String userAgent, VehicleCertificate cert, String ticketNo) {
        return CompensateCertificateCmd.builder()
                .reason(reason)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .vin(cert.getVin())
                .deviceCategory(cert.getDeviceCategory())
                .deviceSn(cert.getDeviceSn())
                .certificateProfile(cert.getCertificateProfile())
                .ticketNo(ticketNo)
                .build();
    }

    /**
     * 失败路径审计（806061 / 806062 等），随后由调用方 rethrow
     */
    private void auditFailed(CompensateCertificateCmd cmd, String action, CertificateStatus before, String errorCode) {
        appendAudit(cmd, action, before, before, "FAILED", errorCode,
                cmd != null ? cmd.getOriginalMesRequestId() : null,
                cmd != null ? cmd.getRequestId() : null);
    }

    /**
     * 规范化请求摘要 + CSR 指纹（不含 CSR 全文、证书本体、私钥或凭据）
     */
    private String buildRequestDigest(CompensateCertificateCmd cmd) {
        String fingerprint = "";
        try {
            fingerprint = CsrUtils.calculateFingerprint(cmd.getCsrDerBase64());
        } catch (Exception e) {
            log.warn("计算CSR指纹失败，请求摘要不含指纹", e);
        }
        return "vin=" + cmd.getVin()
                + "|deviceCategory=" + cmd.getDeviceCategory()
                + "|deviceSn=" + cmd.getDeviceSn()
                + "|profile=" + cmd.getCertificateProfile()
                + "|csrFingerprint=" + fingerprint;
    }

}
