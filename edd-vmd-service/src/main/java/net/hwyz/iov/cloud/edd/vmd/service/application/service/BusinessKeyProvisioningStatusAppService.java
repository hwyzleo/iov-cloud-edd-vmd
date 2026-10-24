package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatus;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningStatusResponse;
import net.hwyz.iov.security.keyprov.v1.KeyPurposeStatus;
import net.hwyz.iov.security.keyprov.v1.ProtocolError;
import org.springframework.stereotype.Service;

/**
 * 业务密钥开通状态查询应用服务（CR-058 §5 Status 段）。
 * <p>
 * 以原 requestId 对账，仅返回非敏感状态与逐用途 keyId/version/import_result；
 * ACK 丢失、断网或重启时用原 requestId 收敛，不创建新业务版本。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessKeyProvisioningStatusAppService {

    private final BusinessKeyProvisioningRepository provisioningRepository;
    private final BusinessKeyProvisioningItemRepository provisioningItemRepository;
    private final BusinessKeyProvisioningDomainService domainService;

    /**
     * 处理 canonical KeyProvisioningStatusRequest。
     */
    public KeyProvisioningStatusResponse queryStatus(KeyProvisioningStatusRequest request,
                                                     KeyprovAuthenticatedContext ctx) {
        String requestId = request.getRequestId();
        if (ctx == null || !ctx.hasIdentity()) {
            return status(requestId, KeyProvisioningStatus.KEY_PROVISIONING_STATUS_UNKNOWN,
                    KeyprovConstants.ERR_PROTOCOL_INVALID, "缺少已认证会话身份");
        }
        BusinessKeyProvisioning batch = provisioningRepository.selectByRequestId(requestId);
        if (batch == null) {
            return status(requestId, KeyProvisioningStatus.KEY_PROVISIONING_STATUS_UNKNOWN,
                    KeyprovConstants.ERR_NOT_FOUND, "开通批次不存在");
        }
        String sessionDigest = domainService.digestHsmUid(ctx.hsmUid());
        if (batch.getHsmUidDigest() == null || !batch.getHsmUidDigest().equals(sessionDigest)) {
            log.warn("keyprov 状态查询会话身份不一致: requestId={}", requestId);
            return status(requestId, KeyProvisioningStatus.KEY_PROVISIONING_STATUS_UNKNOWN,
                    KeyprovConstants.ERR_DEVICE_NOT_ELIGIBLE, "会话身份与开通批次不一致");
        }

        KeyProvisioningStatusResponse.Builder builder = KeyProvisioningStatusResponse.newBuilder()
                .setRequestId(requestId)
                .setStatus(toProtoStatus(batch))
                .setOccurredAtMs(System.currentTimeMillis());
        for (BusinessKeyProvisioningItem item : provisioningItemRepository.selectByProvisioningId(batch.getId())) {
            builder.addItems(KeyPurposeStatus.newBuilder()
                    .setPurpose(KeyprovConstants.purposeProto(item.getPurpose()))
                    .setKeyId(item.getKeyId() == null ? "" : item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion() == null ? 0 : item.getBusinessKeyVersion())
                    .setImportResult(toProtoImportResult(item.getDeviceResult()))
                    .build());
        }
        return builder.build();
    }

    private KeyProvisioningStatus toProtoStatus(BusinessKeyProvisioning batch) {
        return switch (batch.getStatus()) {
            case PREPARING -> KeyProvisioningStatus.KEY_PROVISIONING_STATUS_REQUESTED;
            case WAIT_DEVICE_ACK -> KeyProvisioningStatus.KEY_PROVISIONING_STATUS_ACK_PENDING;
            case COMPLETED -> KeyProvisioningStatus.KEY_PROVISIONING_STATUS_ACTIVE;
            case FAILED, EXPIRED -> KeyProvisioningStatus.KEY_PROVISIONING_STATUS_FAILED;
            default -> KeyProvisioningStatus.KEY_PROVISIONING_STATUS_UNKNOWN;
        };
    }

    private KeyImportResult toProtoImportResult(ProvisioningDeviceResult result) {
        if (result == null) {
            return KeyImportResult.KEY_IMPORT_RESULT_UNSPECIFIED;
        }
        return switch (result) {
            case SUCCEEDED -> KeyImportResult.KEY_IMPORT_RESULT_IMPORTED;
            case FAILED -> KeyImportResult.KEY_IMPORT_RESULT_FAILED;
            case UNKNOWN -> KeyImportResult.KEY_IMPORT_RESULT_UNKNOWN;
            default -> KeyImportResult.KEY_IMPORT_RESULT_UNSPECIFIED;
        };
    }

    private KeyProvisioningStatusResponse status(String requestId, KeyProvisioningStatus status,
                                                 String code, String message) {
        // VMD-DSN-CR-067 §4.5/§5：Status 错误元组自洽——批次不存在（ERR_NOT_FOUND/806099）必须
        // retryable=false 且 action=DO_NOT_RETRY，不得引导继续对账（Status 保持只读，不查询 gate 历史、
        // 不调用 provision()、不创建批次）。其余上下文/协议类错误维持现有语义。
        boolean notFound = KeyprovConstants.ERR_NOT_FOUND.equals(code);
        return KeyProvisioningStatusResponse.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setStatus(status)
                .setOccurredAtMs(System.currentTimeMillis())
                .setError(ProtocolError.newBuilder()
                        .setCode(code == null ? "" : code)
                        .setRetryable(false)
                        .setAction(notFound ? "DO_NOT_RETRY" : "QUERY_STATUS_LATER")
                        .setMessage(message == null ? "" : message)
                        .build())
                .build();
    }
}
