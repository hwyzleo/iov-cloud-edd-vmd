package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateApplicationRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateMaterialUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateNotReadyException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusConfigurationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusProtocolException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusTrustValidationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.ImportProofRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.ImportProofVerificationUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.KeyRevokedException;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Keyprov Service 应用门面（VMD-DSN-CR-059 §5）。
 * <p>
 * 承接 Adapter 映射后的可信上下文与 canonical payload，完成协议门禁（端点/payloadType/
 * schemaVersion/digest/大小）、canonical 解析、身份与调用方校验，并按端点路由至既有
 * 业务密钥开通/ACK/状态查询应用服务；同时将领域协议错误映射为登记后的 806xxx 稳定错误。
 * <p>
 * 本门面不持有 Wrapped Key 之外的敏感材料，禁止对 payload bytes 执行 toString/日志化。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Slf4j
@Service
public class KeyProvisioningApplicationFacade {

    private final BusinessKeyProvisioningBatchAppService provisioningBatchAppService;
    private final BusinessKeyProvisioningAckAppService provisioningAckAppService;
    private final BusinessKeyProvisioningStatusAppService provisioningStatusAppService;

    /**
     * keyprov capability 功能开关（默认关闭 fail-closed）
     */
    @Value("${vmd.business-key.provisioning.keyprov.enabled:false}")
    private boolean keyprovEnabled;

    /**
     * 受信调用方 allowlist（逗号分隔，默认仅 VAGW）
     */
    @Value("${vmd.business-key.provisioning.keyprov.allowed-callers:VAGW}")
    private String allowedCallers;

    /**
     * canonical 协议版本 allowlist 值
     */
    @Value("${vmd.business-key.provisioning.keyprov.schema-version:keyprov-v1}")
    private String expectedSchemaVersion;

    /**
     * canonical payload 大小上限（bytes）
     */
    @Value("${vmd.business-key.provisioning.keyprov.max-payload-bytes:16384}")
    private int maxPayloadBytes;

    /**
     * VMD 固定的 PAR-PROTO release（精确 allowlist，缺失时能力 NOT_READY，CR-062 §4.1）
     */
    @Value("${vmd.business-key.provisioning.keyprov.protocol-release:}")
    private String pinnedProtocolRelease;

    /**
     * VMD 固定的 PAR-PROTO DescriptorSet hash（精确 allowlist，缺失时能力 NOT_READY，CR-062 §4.1）
     */
    @Value("${vmd.business-key.provisioning.keyprov.descriptor-set-hash:}")
    private String pinnedDescriptorSetHash;

    public KeyProvisioningApplicationFacade(BusinessKeyProvisioningBatchAppService provisioningBatchAppService,
                                            BusinessKeyProvisioningAckAppService provisioningAckAppService,
                                            BusinessKeyProvisioningStatusAppService provisioningStatusAppService) {
        this.provisioningBatchAppService = provisioningBatchAppService;
        this.provisioningAckAppService = provisioningAckAppService;
        this.provisioningStatusAppService = provisioningStatusAppService;
    }

    /**
     * 处理官方 /request 端点。
     */
    public KeyProvisioningOperationResult request(String caller, KeyprovAuthenticatedContext ctx,
                                                  String payloadType, String schemaVersion,
                                                  byte[] payloadBytes, String payloadDigest,
                                                  String protocolRelease, String descriptorSetHash) {
        return withCanonicalErrorPayload(KeyprovConstants.PAYLOAD_TYPE_REQUEST, payloadBytes,
                doRequest(caller, ctx, payloadType, schemaVersion, payloadBytes, payloadDigest,
                        protocolRelease, descriptorSetHash));
    }

    /**
     * 处理官方 /request 端点（未声明 release/hash，fail-closed 不下发 envelope v1）。
     */
    public KeyProvisioningOperationResult request(String caller, KeyprovAuthenticatedContext ctx,
                                                  String payloadType, String schemaVersion,
                                                  byte[] payloadBytes, String payloadDigest) {
        return request(caller, ctx, payloadType, schemaVersion, payloadBytes, payloadDigest, null, null);
    }

    private KeyProvisioningOperationResult doRequest(String caller, KeyprovAuthenticatedContext ctx,
                                                     String payloadType, String schemaVersion,
                                                     byte[] payloadBytes, String payloadDigest,
                                                     String protocolRelease, String descriptorSetHash) {
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningOperationResult releaseGate = releaseGate(protocolRelease, descriptorSetHash);
        if (releaseGate != null) {
            return releaseGate;
        }
        KeyProvisioningRequest request;
        try {
            request = KeyProvisioningRequest.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("request", "canonical KeyProvisioningRequest 解析失败");
        }
        if (request.getRequestId().isBlank()) {
            return protocolReject("request", "requestId 缺失");
        }
        try {
            KeyProvisioningResponse response = provisioningBatchAppService.provision(request, ctx);
            if (response.getResult() == KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED) {
                return success(response.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_RESPONSE,
                        response.toByteArray(), response.getExpireAtMs() > 0 ? response.getExpireAtMs() : null);
            }
            return failure(KeyprovConstants.PAYLOAD_TYPE_REQUEST, response.getRequestId(), response.getResult(),
                    response.hasError() ? response.getError().getCode() : null,
                    response.hasError() ? response.getError().getMessage() : null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov request 结果未知: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
        } catch (CertificateMaterialUnavailableException | CertificateEvidenceMismatchException
                 | CertificateReferenceAmbiguousException | CertificateNotReadyException
                 | CertificateProfileNotAllowedException | KeyRevokedException
                 | CertificateStatusTrustValidationException | CertificateApplicationRejectedException
                 | CertificateStatusProtocolException | CertificateStatusDependencyUnavailableException
                 | CertificateStatusConfigurationException | CertificateStatusOutcomeUnknownException e) {
            log.warn("keyprov request 证书解析/策略异常: requestId={}, type={}", request.getRequestId(), e.getClass().getSimpleName());
            return certificateFailure(request.getRequestId(), e);
        } catch (BusinessKeyKmsUnavailableException e) {
            log.warn("keyprov request Provider 未就绪: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_PROVIDER_NOT_READY,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "business-key Provider能力未就绪");
        } catch (CryptoDependencyUnavailableException e) {
            log.warn("keyprov request 依赖不可用: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "业务密钥目录或framework能力不可用");
        } catch (CryptoException e) {
            log.warn("keyprov request 封装失败: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_WRAP_FAILED,
                    VmdKeyProvisioningOutcome.REJECTED, false, "业务密钥封装失败");
        } catch (RuntimeException e) {
            log.error("keyprov request 内部异常: requestId={}", request.getRequestId(), e);
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "服务内部不可用");
        }
    }

    /**
     * 处理官方 /ack 端点。
     */
    public KeyProvisioningOperationResult ack(String caller, KeyprovAuthenticatedContext ctx,
                                              String payloadType, String schemaVersion,
                                              byte[] payloadBytes, String payloadDigest) {
        return withCanonicalErrorPayload(KeyprovConstants.PAYLOAD_TYPE_ACK, payloadBytes,
                doAck(caller, ctx, payloadType, schemaVersion, payloadBytes, payloadDigest));
    }

    private KeyProvisioningOperationResult doAck(String caller, KeyprovAuthenticatedContext ctx,
                                                 String payloadType, String schemaVersion,
                                                 byte[] payloadBytes, String payloadDigest) {
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_ACK, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningAck ack;
        try {
            ack = KeyProvisioningAck.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("ack", "canonical KeyProvisioningAck 解析失败");
        }
        if (ack.getRequestId().isBlank()) {
            return protocolReject("ack", "requestId 缺失");
        }
        try {
            KeyProvisioningAckResult result = provisioningAckAppService.confirmImport(ack, ctx);
            if (result.getResult() == KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED) {
                return success(result.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT,
                        result.toByteArray(), null);
            }
            return failure(KeyprovConstants.PAYLOAD_TYPE_ACK, result.getRequestId(), result.getResult(),
                    result.hasError() ? result.getError().getCode() : null,
                    result.hasError() ? result.getError().getMessage() : null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov ack 结果未知: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
        } catch (CertificateMaterialUnavailableException | CertificateEvidenceMismatchException
                 | CertificateReferenceAmbiguousException | CertificateNotReadyException
                 | CertificateProfileNotAllowedException | KeyRevokedException
                 | CertificateStatusTrustValidationException | CertificateApplicationRejectedException
                 | CertificateStatusProtocolException | CertificateStatusDependencyUnavailableException
                 | CertificateStatusConfigurationException | CertificateStatusOutcomeUnknownException
                 | CertificateEvidenceRejectedException e) {
            log.warn("keyprov ack 证书解析/策略异常: requestId={}, type={}", ack.getRequestId(), e.getClass().getSimpleName());
            return certificateFailure(ack.getRequestId(), e);
        } catch (ImportProofRejectedException e) {
            log.warn("keyprov ack 导入证明验签拒绝: requestId={}, reason={}", ack.getRequestId(), e.rejectReason());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_IMPORT_PROOF_REJECTED,
                    VmdKeyProvisioningOutcome.REJECTED, false, "导入证明验签拒绝，安全拒绝且不激活");
        } catch (ImportProofVerificationUnavailableException e) {
            log.warn("keyprov ack 导入证明验签 capability 未就绪: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_PROVIDER_NOT_READY,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "导入证明验签能力未就绪，不激活");
        } catch (BusinessKeyKmsUnavailableException e) {
            log.warn("keyprov ack Provider 未就绪: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_PROVIDER_NOT_READY,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "business-key Provider能力未就绪");
        } catch (CryptoDependencyUnavailableException e) {
            log.warn("keyprov ack 依赖不可用: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "业务密钥目录或framework能力不可用");
        } catch (CryptoException e) {
            log.warn("keyprov ack 密钥操作失败: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_WRAP_FAILED,
                    VmdKeyProvisioningOutcome.REJECTED, false, "业务密钥操作失败");
        } catch (RuntimeException e) {
            log.error("keyprov ack 内部异常: requestId={}", ack.getRequestId(), e);
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "服务内部不可用");
        }
    }

    /**
     * 处理官方 /status 端点（只读，不创建版本、不重放请求）。
     */
    public KeyProvisioningOperationResult status(String caller, KeyprovAuthenticatedContext ctx,
                                                 String payloadType, String schemaVersion,
                                                 byte[] payloadBytes, String payloadDigest) {
        return withCanonicalErrorPayload(KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, payloadBytes,
                doStatus(caller, ctx, payloadType, schemaVersion, payloadBytes, payloadDigest));
    }

    private KeyProvisioningOperationResult doStatus(String caller, KeyprovAuthenticatedContext ctx,
                                                    String payloadType, String schemaVersion,
                                                    byte[] payloadBytes, String payloadDigest) {
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningStatusRequest request;
        try {
            request = KeyProvisioningStatusRequest.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("status", "canonical KeyProvisioningStatusRequest 解析失败");
        }
        if (request.getRequestId().isBlank()) {
            return protocolReject("status", "requestId 缺失");
        }
        try {
            KeyProvisioningStatusResponse response = provisioningStatusAppService.queryStatus(request, ctx);
            if (response.hasError()) {
                return failure(KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, response.getRequestId(),
                        KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        response.getError().getCode(), response.getError().getMessage());
            }
            return success(response.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_STATUS_RESPONSE,
                    response.toByteArray(), null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov status 结果未知: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
        } catch (CertificateMaterialUnavailableException | CertificateEvidenceMismatchException
                 | CertificateReferenceAmbiguousException | CertificateNotReadyException
                 | CertificateProfileNotAllowedException | KeyRevokedException
                 | CertificateStatusTrustValidationException | CertificateApplicationRejectedException
                 | CertificateStatusProtocolException | CertificateStatusDependencyUnavailableException
                 | CertificateStatusConfigurationException | CertificateStatusOutcomeUnknownException e) {
            log.warn("keyprov status 证书解析/策略异常: requestId={}, type={}", request.getRequestId(), e.getClass().getSimpleName());
            return certificateFailure(request.getRequestId(), e);
        } catch (BusinessKeyKmsUnavailableException e) {
            log.warn("keyprov status Provider 未就绪: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_PROVIDER_NOT_READY,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "business-key Provider能力未就绪");
        } catch (CryptoDependencyUnavailableException e) {
            log.warn("keyprov status 依赖不可用: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "业务密钥目录或framework能力不可用");
        } catch (RuntimeException e) {
            log.error("keyprov status 内部异常: requestId={}", request.getRequestId(), e);
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "服务内部不可用");
        }
    }

    /**
     * 调用方身份授权（受信 VAGW workload identity）
     */
    public boolean isCallerAuthorized(String caller) {
        if (caller == null || caller.isBlank()) {
            return false;
        }
        return allowedCallerSet().contains(caller.trim().toUpperCase(Locale.ROOT));
    }

    private Set<String> allowedCallerSet() {
        Set<String> allowed = new LinkedHashSet<>();
        if (allowedCallers != null) {
            Arrays.stream(allowedCallers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toUpperCase(Locale.ROOT))
                    .forEach(allowed::add);
        }
        return allowed;
    }

    /**
     * 协议与身份门禁；通过返回 null，否则返回错误结果。
     */
    private KeyProvisioningOperationResult gate(String caller, KeyprovAuthenticatedContext ctx,
                                                String expectedPayloadType, String payloadType,
                                                String schemaVersion, byte[] payloadBytes, String payloadDigest) {
        if (!keyprovEnabled) {
            log.warn("keyprov capability 未启用，fail-closed");
            return error(null, VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "keyprov capability 未启用");
        }
        if (!isCallerAuthorized(caller)) {
            log.warn("keyprov 调用方未授权: caller={}", caller);
            return error(null, VmdErrorCode.KEYPROV_CALLER_UNAUTHORIZED,
                    VmdKeyProvisioningOutcome.REJECTED, false, "调用方身份未授权");
        }
        if (ctx == null || !ctx.hasIdentity() || ctx.hsmUid() == null || ctx.hsmUid().isBlank()) {
            // 诊断：只输出布尔与调用方，不输出设备身份明文
            log.warn("keyprov 请求被拒(806091 设备上下文): caller={}, hasContext={}, hasIdentity={}, hsmUidPresent={}",
                    caller, ctx != null, ctx != null && ctx.hasIdentity(),
                    ctx != null && ctx.hsmUid() != null && !ctx.hsmUid().isBlank());
            return error(null, VmdErrorCode.KEYPROV_DEVICE_CONTEXT_MISMATCH,
                    VmdKeyProvisioningOutcome.REJECTED, false, "缺少可信设备上下文");
        }
        if (expectedPayloadType == null || !expectedPayloadType.equals(payloadType)) {
            log.warn("keyprov 请求被拒(806094 payloadType不匹配): caller={}, expected={}, actual={}",
                    caller, expectedPayloadType, payloadType);
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "端点与payloadType不匹配");
        }
        if (expectedSchemaVersion == null || !expectedSchemaVersion.equals(schemaVersion)) {
            log.warn("keyprov 请求被拒(806094 schemaVersion不受支持): caller={}, expected={}, actual={}",
                    caller, expectedSchemaVersion, schemaVersion);
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "schemaVersion不受支持");
        }
        if (payloadBytes == null || payloadBytes.length == 0 || payloadBytes.length > maxPayloadBytes) {
            log.warn("keyprov 请求被拒(806094 payload为空或超限): caller={}, payloadBytes={}, maxPayloadBytes={}",
                    caller, payloadBytes == null ? -1 : payloadBytes.length, maxPayloadBytes);
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "payload为空或超限");
        }
        if (payloadDigest == null || payloadDigest.isBlank()
                || !payloadDigest.equalsIgnoreCase(sha256Hex(payloadBytes))) {
            // 诊断：只输出不可逆摘要指纹（前16位）与长度，绝不输出 payload 本体
            log.warn("keyprov 请求被拒(806094 payload摘要不匹配): caller={}, payloadBytes={}, receivedDigest={}, computedDigest={}",
                    caller, payloadBytes.length, digestFingerprint(payloadDigest),
                    digestFingerprint(sha256Hex(payloadBytes)));
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "payload摘要缺失或不匹配");
        }
        return null;
    }

    /**
     * 摘要指纹：仅保留前 16 位便于与端到端比对，属不可逆哈希、非敏感材料本身。
     */
    private static String digestFingerprint(String digest) {
        if (digest == null || digest.isBlank()) {
            return "<missing>";
        }
        String value = digest.trim();
        return value.length() <= 16 ? value : value.substring(0, 16) + "…(len=" + value.length() + ")";
    }

    private KeyProvisioningOperationResult protocolReject(String endpoint, String message) {
        log.warn("keyprov 请求被拒(806094 协议/内容非法): endpoint={}, detail={}", endpoint, message);
        return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                VmdKeyProvisioningOutcome.REJECTED, false, message);
    }

    /**
     * PAR-PROTO release / DescriptorSet hash 精确 allowlist 门禁（CR-062 §4.1/§9）。
     * <p>
     * VMD 只接受与自身固定值完全相等（区分大小写、不 trim）的请求；VMD 固定值缺失或请求
     * 声明缺失/不匹配一律 fail-closed，且不根据请求值动态选择 schema 或算法。
     */
    private KeyProvisioningOperationResult releaseGate(String protocolRelease, String descriptorSetHash) {
        if (isBlank(pinnedProtocolRelease) || isBlank(pinnedDescriptorSetHash)) {
            log.warn("keyprov 请求被拒(806094 release/hash未固定): pinnedReleasePresent={}, pinnedHashPresent={}",
                    !isBlank(pinnedProtocolRelease), !isBlank(pinnedDescriptorSetHash));
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "keyprov release/hash能力未就绪");
        }
        if (!pinnedProtocolRelease.equals(protocolRelease)
                || !pinnedDescriptorSetHash.equals(descriptorSetHash)) {
            log.warn("keyprov 请求被拒(806094 release/hash不匹配)");
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "release/DescriptorSet hash不匹配");
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 将业务应用服务返回的协议结果映射为 806xxx 稳定错误与结果类别。
     */
    private KeyProvisioningOperationResult failure(String uplinkPayloadType, String requestId,
                                                   KeyProvisioningResult result,
                                                   String protocolCode, String message) {
        VmdErrorCode errorCode = mapProtocolCode(protocolCode);
        VmdKeyProvisioningOutcome outcome;
        boolean retryable;
        // 根派生/Transit 根未就绪为确定性依赖不可用（同 requestId 重试），非结果未知
        if (KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY.equals(protocolCode)
                || KeyprovConstants.ERR_IMPORT_PROOF_NOT_READY.equals(protocolCode)) {
            outcome = VmdKeyProvisioningOutcome.UNAVAILABLE;
            retryable = true;
        } else {
            switch (result) {
                case KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE -> {
                    outcome = VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN;
                    retryable = true;
                }
                case KEY_PROVISIONING_RESULT_EXPIRED, KEY_PROVISIONING_RESULT_CONFLICT,
                     KEY_PROVISIONING_RESULT_REJECTED -> {
                    outcome = VmdKeyProvisioningOutcome.REJECTED;
                    retryable = false;
                }
                default -> {
                    outcome = VmdKeyProvisioningOutcome.REJECTED;
                    retryable = false;
                }
            }
        }
        KeyProvisioningOperationResult operationResult = error(requestId, errorCode, outcome, retryable, message);
        // 业务层已给出 canonical 协议结果：下行 payload 直接透传其精确 result 与协议错误码
        attachCanonicalErrorPayload(operationResult, uplinkPayloadType, requestId, result,
                protocolCode == null || protocolCode.isBlank() ? mapErrorCodeToProtocolCode(errorCode) : protocolCode,
                message);
        return operationResult;
    }

    /**
     * 保证「非成功」结果同样返回 canonical 下行 payload（VMD-DSN-CR-059 §5/§6）。
     * <p>
     * 门禁/协议/依赖类失败此前只返回 out-of-band 806xxx，VAGW 因「无 canonical payload」不回下行，
     * 车端只能按超时反复重投同一 requestId。此处统一补一条 canonical 错误响应：车端据
     * {@code error.retryable} 决定 RETRY_WAIT 或 BLOCKED，可按 requestId 关联并终止盲重试。
     */
    private KeyProvisioningOperationResult withCanonicalErrorPayload(String uplinkPayloadType,
                                                                    byte[] payloadBytes,
                                                                    KeyProvisioningOperationResult result) {
        if (result == null || result.getErrorCode() == null || result.getDownlinkPayload() != null) {
            return result;
        }
        String requestId = result.getRequestId() != null && !result.getRequestId().isBlank()
                ? result.getRequestId() : bestEffortRequestId(payloadBytes);
        attachCanonicalErrorPayload(result, uplinkPayloadType, requestId,
                toProtocolResult(result.getOutcome()),
                mapErrorCodeToProtocolCode(result.getErrorCode()), result.getErrorMessage());
        return result;
    }

    /**
     * 将 canonical 错误 payload 挂到操作结果上；不改变 VAGW 侧 806xxx 稳定码语义。
     */
    private void attachCanonicalErrorPayload(KeyProvisioningOperationResult target, String uplinkPayloadType,
                                             String requestId, KeyProvisioningResult result,
                                             String protocolCode, String message) {
        if (target == null || downlinkPayloadTypeFor(uplinkPayloadType) == null) {
            return;
        }
        byte[] payload = buildCanonicalErrorPayload(uplinkPayloadType, requestId, result, protocolCode,
                target.isRetryable(), target.getAction(), message);
        if (payload == null) {
            return;
        }
        target.setDownlinkPayloadType(downlinkPayloadTypeFor(uplinkPayloadType));
        target.setDownlinkPayload(payload);
        target.setDownlinkPayloadDigest(sha256Hex(payload));
    }

    private byte[] buildCanonicalErrorPayload(String uplinkPayloadType, String requestId,
                                              KeyProvisioningResult result, String protocolCode,
                                              boolean retryable, String action, String message) {
        ProtocolError error = ProtocolError.newBuilder()
                .setCode(protocolCode == null || protocolCode.isBlank()
                        ? KeyprovConstants.ERR_PROTOCOL_INVALID : protocolCode)
                .setRetryable(retryable)
                .setAction(action == null ? "" : action)
                .setMessage(message == null ? "" : truncate(message))
                .build();
        String requestIdValue = boundedRequestId(requestId);
        KeyProvisioningResult resultValue = result == null
                ? KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED : result;
        return switch (uplinkPayloadType) {
            case KeyprovConstants.PAYLOAD_TYPE_REQUEST -> KeyProvisioningResponse.newBuilder()
                    .setRequestId(requestIdValue)
                    .setResult(resultValue)
                    .setError(error)
                    .build().toByteArray();
            case KeyprovConstants.PAYLOAD_TYPE_ACK -> KeyProvisioningAckResult.newBuilder()
                    .setRequestId(requestIdValue)
                    .setResult(resultValue)
                    .setError(error)
                    .build().toByteArray();
            // StatusResponse 无 result 字段：以 error 表达拒绝，status 保持 UNSPECIFIED
            case KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST -> KeyProvisioningStatusResponse.newBuilder()
                    .setRequestId(requestIdValue)
                    .setError(error)
                    .build().toByteArray();
            default -> null;
        };
    }

    private static String downlinkPayloadTypeFor(String uplinkPayloadType) {
        if (uplinkPayloadType == null) {
            return null;
        }
        return switch (uplinkPayloadType) {
            case KeyprovConstants.PAYLOAD_TYPE_REQUEST -> KeyprovConstants.PAYLOAD_TYPE_RESPONSE;
            case KeyprovConstants.PAYLOAD_TYPE_ACK -> KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT;
            case KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST -> KeyprovConstants.PAYLOAD_TYPE_STATUS_RESPONSE;
            default -> null;
        };
    }

    private static KeyProvisioningResult toProtocolResult(VmdKeyProvisioningOutcome outcome) {
        if (outcome == null) {
            return KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED;
        }
        return switch (outcome) {
            case SUCCESS -> KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED;
            case REJECTED -> KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED;
            case OUTCOME_UNKNOWN, UNAVAILABLE -> KeyProvisioningResult.KEY_PROVISIONING_RESULT_RETRYABLE_FAILURE;
        };
    }

    /**
     * VMD 稳定 806xxx → canonical ProtocolError.code（VMD-DSN-CR-059 §6）。
     * <p>
     * 与 {@link #mapProtocolCode(String)} 互为反向；仅用于门禁/依赖类失败时补齐车端可消费的错误码。
     */
    private static String mapErrorCodeToProtocolCode(VmdErrorCode errorCode) {
        if (errorCode == null) {
            return KeyprovConstants.ERR_PROTOCOL_INVALID;
        }
        return switch (errorCode) {
            case KEYPROV_REQUEST_INVALID -> KeyprovConstants.ERR_PROTOCOL_INVALID;
            case KEYPROV_IDEMPOTENCY_CONFLICT -> KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT;
            case KEYPROV_BATCH_NOT_FOUND -> KeyprovConstants.ERR_NOT_FOUND;
            case KEYPROV_BINDING_INVALID, KEYPROV_DEVICE_CONTEXT_MISMATCH,
                 KEYPROV_CALLER_UNAUTHORIZED -> KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE;
            case KEYPROV_CERTIFICATE_INVALID, KEYPROV_CERTIFICATE_EVIDENCE_MISMATCH,
                 KEYPROV_CERTIFICATE_REFERENCE_AMBIGUOUS,
                 KEYPROV_CERTIFICATE_POLICY_REJECTED -> KeyprovConstants.ERR_CERTIFICATE_INVALID;
            case KEYPROV_OUTCOME_UNKNOWN -> KeyprovConstants.ERR_RECONCILE_REQUIRED;
            case KEYPROV_VIN_BINDING_INVALID -> KeyprovConstants.ERR_VIN_BINDING_INVALID;
            case KEYPROV_DERIVED_PROVIDER_NOT_READY -> KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY;
            case KEYPROV_DERIVATION_UNSUPPORTED -> KeyprovConstants.ERR_DERIVATION_UNSUPPORTED;
            case KEYPROV_ENVELOPE_UNSUPPORTED -> KeyprovConstants.ERR_ENVELOPE_UNSUPPORTED;
            case KEYPROV_ENVELOPE_INTEGRITY_REJECTED -> KeyprovConstants.ERR_ENVELOPE_INTEGRITY;
            case KEYPROV_IMPORT_PROOF_REJECTED -> KeyprovConstants.ERR_IMPORT_PROOF_INVALID;
            default -> KeyprovConstants.ERR_UNAVAILABLE;
        };
    }

    /**
     * 仅用于关联的 requestId 最佳努力提取。
     * <p>
     * canonical 上行三类消息的 field 1 均为 {@code request_id}。门禁/解析失败时无权威 requestId，
     * 此值只让车端能把下行错误关联回流程（否则会因 request_id 不匹配而忽略，继续盲重试）；
     * 不参与任何校验、授权或幂等决策。
     */
    private static String bestEffortRequestId(byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            return "";
        }
        try {
            return boundedRequestId(KeyProvisioningRequest.parseFrom(payloadBytes).getRequestId());
        } catch (InvalidProtocolBufferException e) {
            return "";
        }
    }

    /**
     * request_id 规范化：canonical 约束为 ≤128 UTF-8 字节且无控制字符。
     * <p>
     * 不满足时以空串关联（envelope 仍带 correlation），绝不把非法值写入 canonical 下行 payload。
     */
    private static String boundedRequestId(String requestId) {
        if (requestId == null) {
            return "";
        }
        String candidate = requestId.trim();
        if (candidate.isEmpty()) {
            return "";
        }
        if (candidate.getBytes(StandardCharsets.UTF_8).length > 128 || containsControlChar(candidate)) {
            return "";
        }
        return candidate;
    }

    private static boolean containsControlChar(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * framework 证书解析/状态异常 → VMD 稳定错误码与结果类别（CR-060 §6.2）。
     * <ul>
     *   <li>材料不可取/locator 缺失 → 806101，可重试（原 requestId）</li>
     *   <li>证据冲突 → 806102，不可重试</li>
     *   <li>目录多命中 → 806103，不可重试（数据治理）</li>
     *   <li>信任/有效期/KU-EKU/Profile/吊销/应用拒绝/状态协议 → 806104，不可重试</li>
     *   <li>状态依赖不可用/配置错误/状态结果未知 → 806105，依赖恢复后可重试</li>
     * </ul>
     */
    private KeyProvisioningOperationResult certificateFailure(String requestId, CryptoException e) {
        if (e instanceof CertificateMaterialUnavailableException) {
            return error(requestId, VmdErrorCode.KEYPROV_CERTIFICATE_MATERIAL_UNAVAILABLE,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "证书材料暂不可取或locator缺失，请以原requestId重试");
        }
        if (e instanceof CertificateEvidenceMismatchException) {
            return error(requestId, VmdErrorCode.KEYPROV_CERTIFICATE_EVIDENCE_MISMATCH,
                    VmdKeyProvisioningOutcome.REJECTED, false, "证书serial/fingerprint/requestId证据冲突，安全拒绝");
        }
        if (e instanceof CertificateReferenceAmbiguousException) {
            return error(requestId, VmdErrorCode.KEYPROV_CERTIFICATE_REFERENCE_AMBIGUOUS,
                    VmdKeyProvisioningOutcome.REJECTED, false, "证书目录命中多条记录，需数据治理");
        }
        if (e instanceof CertificateStatusDependencyUnavailableException
                || e instanceof CertificateStatusConfigurationException
                || e instanceof CertificateStatusOutcomeUnknownException) {
            return error(requestId, VmdErrorCode.KEYPROV_PROVIDER_NOT_READY,
                    VmdKeyProvisioningOutcome.UNAVAILABLE, true, "证书状态能力未就绪，依赖不可用");
        }
        return error(requestId, VmdErrorCode.KEYPROV_CERTIFICATE_POLICY_REJECTED,
                VmdKeyProvisioningOutcome.REJECTED, false, "设备证书不满足受信链/有效期/KU-EKU策略，拒绝封装");
    }

    private VmdErrorCode mapProtocolCode(String protocolCode) {        if (protocolCode == null) {
            return VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE;
        }
        return switch (protocolCode) {
            case KeyprovConstants.ERR_NOT_FOUND -> VmdErrorCode.KEYPROV_BATCH_NOT_FOUND;
            case KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT, KeyprovConstants.ERR_ACK_CONFLICT ->
                    VmdErrorCode.KEYPROV_IDEMPOTENCY_CONFLICT;
            case KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE -> VmdErrorCode.KEYPROV_BINDING_INVALID;
            case KeyprovConstants.ERR_CERTIFICATE_INVALID -> VmdErrorCode.KEYPROV_CERTIFICATE_INVALID;
            case KeyprovConstants.ERR_VIN_BINDING_INVALID -> VmdErrorCode.KEYPROV_VIN_BINDING_INVALID;
            case KeyprovConstants.ERR_DERIVED_PROVIDER_NOT_READY -> VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY;
            case KeyprovConstants.ERR_DERIVATION_UNSUPPORTED -> VmdErrorCode.KEYPROV_DERIVATION_UNSUPPORTED;
            case KeyprovConstants.ERR_ENVELOPE_UNSUPPORTED -> VmdErrorCode.KEYPROV_ENVELOPE_UNSUPPORTED;
            case KeyprovConstants.ERR_ENVELOPE_INTEGRITY -> VmdErrorCode.KEYPROV_ENVELOPE_INTEGRITY_REJECTED;
            case KeyprovConstants.ERR_IMPORT_PROOF_INVALID -> VmdErrorCode.KEYPROV_IMPORT_PROOF_REJECTED;
            case KeyprovConstants.ERR_IMPORT_PROOF_NOT_READY -> VmdErrorCode.KEYPROV_PROVIDER_NOT_READY;
            case KeyprovConstants.ERR_PROTOCOL_INVALID, KeyprovConstants.ERR_PURPOSE_INCOMPLETE,
                 KeyprovConstants.ERR_STATE_NOT_ALLOWED, KeyprovConstants.ERR_ITEM_MISMATCH,
                 KeyprovConstants.ERR_EXPIRED, KeyprovConstants.ERR_IMPORT_FAILED ->
                    VmdErrorCode.KEYPROV_REQUEST_INVALID;
            case KeyprovConstants.ERR_UNAVAILABLE -> VmdErrorCode.KEYPROV_DIRECTORY_UNAVAILABLE;
            case KeyprovConstants.ERR_RECONCILE_REQUIRED -> VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN;
            default -> VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE;
        };
    }

    private KeyProvisioningOperationResult success(String requestId, String downlinkPayloadType,
                                                   byte[] downlinkPayload, Long expireAtMs) {
        return KeyProvisioningOperationResult.builder()
                .requestId(requestId)
                .downlinkPayloadType(downlinkPayloadType)
                .downlinkPayload(downlinkPayload)
                .downlinkPayloadDigest(sha256Hex(downlinkPayload))
                .expireAtMs(expireAtMs)
                .outcome(VmdKeyProvisioningOutcome.SUCCESS)
                .build();
    }

    private KeyProvisioningOperationResult error(String requestId, VmdErrorCode errorCode,
                                                 VmdKeyProvisioningOutcome outcome,
                                                 boolean retryable, String message) {
        String action = switch (outcome) {
            case OUTCOME_UNKNOWN -> "QUERY_STATUS_LATER";
            case UNAVAILABLE -> "RETRY_SAME_REQUEST_ID";
            default -> "DO_NOT_RETRY";
        };
        return KeyProvisioningOperationResult.builder()
                .requestId(requestId)
                .outcome(outcome)
                .errorCode(errorCode)
                .errorMessage(truncate(message))
                .retryable(retryable)
                .action(action)
                .build();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 256 ? message.substring(0, 256) : message;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
