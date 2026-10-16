package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoOperationOutcomeUnknownException;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAckResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
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
                                                  byte[] payloadBytes, String payloadDigest) {
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningRequest request;
        try {
            request = KeyProvisioningRequest.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("canonical KeyProvisioningRequest 解析失败");
        }
        if (request.getRequestId().isBlank()) {
            return protocolReject("requestId 缺失");
        }
        try {
            KeyProvisioningResponse response = provisioningBatchAppService.provision(request, ctx);
            if (response.getResult() == KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED) {
                return success(response.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_RESPONSE,
                        response.toByteArray(), response.getExpireAtMs() > 0 ? response.getExpireAtMs() : null);
            }
            return failure(response.getRequestId(), response.getResult(),
                    response.hasError() ? response.getError().getCode() : null,
                    response.hasError() ? response.getError().getMessage() : null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov request 结果未知: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
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
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_ACK, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningAck ack;
        try {
            ack = KeyProvisioningAck.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("canonical KeyProvisioningAck 解析失败");
        }
        if (ack.getRequestId().isBlank()) {
            return protocolReject("requestId 缺失");
        }
        try {
            KeyProvisioningAckResult result = provisioningAckAppService.confirmImport(ack, ctx);
            if (result.getResult() == KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED) {
                return success(result.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_ACK_RESULT,
                        result.toByteArray(), null);
            }
            return failure(result.getRequestId(), result.getResult(),
                    result.hasError() ? result.getError().getCode() : null,
                    result.hasError() ? result.getError().getMessage() : null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov ack 结果未知: requestId={}", ack.getRequestId());
            return error(ack.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
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
        KeyProvisioningOperationResult gate = gate(caller, ctx,
                KeyprovConstants.PAYLOAD_TYPE_STATUS_REQUEST, payloadType, schemaVersion, payloadBytes, payloadDigest);
        if (gate != null) {
            return gate;
        }
        KeyProvisioningStatusRequest request;
        try {
            request = KeyProvisioningStatusRequest.parseFrom(payloadBytes);
        } catch (InvalidProtocolBufferException e) {
            return protocolReject("canonical KeyProvisioningStatusRequest 解析失败");
        }
        if (request.getRequestId().isBlank()) {
            return protocolReject("requestId 缺失");
        }
        try {
            KeyProvisioningStatusResponse response = provisioningStatusAppService.queryStatus(request, ctx);
            if (response.hasError()) {
                return failure(response.getRequestId(), KeyProvisioningResult.KEY_PROVISIONING_RESULT_REJECTED,
                        response.getError().getCode(), response.getError().getMessage());
            }
            return success(response.getRequestId(), KeyprovConstants.PAYLOAD_TYPE_STATUS_RESPONSE,
                    response.toByteArray(), null);
        } catch (CryptoOperationOutcomeUnknownException e) {
            log.warn("keyprov status 结果未知: requestId={}", request.getRequestId());
            return error(request.getRequestId(), VmdErrorCode.KEYPROV_OUTCOME_UNKNOWN,
                    VmdKeyProvisioningOutcome.OUTCOME_UNKNOWN, true, "framework结果未知，请以原requestId对账");
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
            return error(null, VmdErrorCode.KEYPROV_DEVICE_CONTEXT_MISMATCH,
                    VmdKeyProvisioningOutcome.REJECTED, false, "缺少可信设备上下文");
        }
        if (expectedPayloadType == null || !expectedPayloadType.equals(payloadType)) {
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "端点与payloadType不匹配");
        }
        if (expectedSchemaVersion == null || !expectedSchemaVersion.equals(schemaVersion)) {
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "schemaVersion不受支持");
        }
        if (payloadBytes == null || payloadBytes.length == 0 || payloadBytes.length > maxPayloadBytes) {
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "payload为空或超限");
        }
        if (payloadDigest == null || payloadDigest.isBlank()
                || !payloadDigest.equalsIgnoreCase(sha256Hex(payloadBytes))) {
            return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                    VmdKeyProvisioningOutcome.REJECTED, false, "payload摘要缺失或不匹配");
        }
        return null;
    }

    private KeyProvisioningOperationResult protocolReject(String message) {
        return error(null, VmdErrorCode.KEYPROV_REQUEST_INVALID,
                VmdKeyProvisioningOutcome.REJECTED, false, message);
    }

    /**
     * 将业务应用服务返回的协议结果映射为 806xxx 稳定错误与结果类别。
     */
    private KeyProvisioningOperationResult failure(String requestId, KeyProvisioningResult result,
                                                   String protocolCode, String message) {
        VmdErrorCode errorCode = mapProtocolCode(protocolCode);
        VmdKeyProvisioningOutcome outcome;
        boolean retryable;
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
        return error(requestId, errorCode, outcome, retryable, message);
    }

    private VmdErrorCode mapProtocolCode(String protocolCode) {
        if (protocolCode == null) {
            return VmdErrorCode.KEYPROV_INTERNAL_UNAVAILABLE;
        }
        return switch (protocolCode) {
            case KeyprovConstants.ERR_NOT_FOUND -> VmdErrorCode.KEYPROV_BATCH_NOT_FOUND;
            case KeyprovConstants.ERR_IDEMPOTENCY_CONFLICT, KeyprovConstants.ERR_ACK_CONFLICT ->
                    VmdErrorCode.KEYPROV_IDEMPOTENCY_CONFLICT;
            case KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE -> VmdErrorCode.KEYPROV_BINDING_INVALID;
            case KeyprovConstants.ERR_CERTIFICATE_INVALID -> VmdErrorCode.KEYPROV_CERTIFICATE_INVALID;
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
