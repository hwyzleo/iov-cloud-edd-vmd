package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发/复验材料不可用异常
 * <p>
 * VMD-DSN-CR-063：签发、材料解析、Profile validator 或 Keyprov capability 任一依赖暂不可用时
 * 抛此异常；调用方以原 requestId 重试（retryable），不得包装为确定性拒绝。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateMaterialUnavailableException extends VmdBaseException {

    public CertificateMaterialUnavailableException(String detail) {
        super(VmdErrorCode.CERTIFICATE_MATERIAL_UNAVAILABLE, detail);
        log.warn("证书签发/复验材料或依赖暂不可用：{}", detail);
    }

}
