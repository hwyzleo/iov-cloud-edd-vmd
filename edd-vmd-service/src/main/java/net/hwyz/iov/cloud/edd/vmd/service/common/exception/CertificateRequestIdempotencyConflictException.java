package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书申请幂等键冲突异常
 * <p>
 * CR-053：requestId 已存在，但 VIN / 设备 / Profile / CSR 指纹请求摘要不一致。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CertificateRequestIdempotencyConflictException extends VmdBaseException {

    public CertificateRequestIdempotencyConflictException(String requestId) {
        super(VmdErrorCode.CERTIFICATE_REQUEST_IDEMPOTENCY_CONFLICT);
        log.warn("证书申请[{}]幂等键冲突：requestId 已存在但请求摘要不一致", requestId);
    }

}
