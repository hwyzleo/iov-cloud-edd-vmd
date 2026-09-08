package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 软件实装幂等键冲突异常
 * <p>
 * VMD-DSN-CR-045/046: 幂等键重复但内容不一致（如 observationKey 相同但 canonicalDigest 不同）
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
public class SoftwareIdempotencyConflictException extends VmdBaseException {

    public SoftwareIdempotencyConflictException(String reason) {
        super(VmdErrorCode.SOFTWARE_IDEMPOTENCY_CONFLICT, reason);
        log.warn("软件实装幂等键冲突: {}", reason);
    }
}
