package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * Keyprov 信封完整性拒绝（VMD-DSN-CR-062 §4.2/RD-062-9）。
 * <p>
 * Response 映射前发现 envelope view 为空、版本不受支持、顶层元数据与视图不一致、
 * canonical 校验失败或 view→wire 字段不一一对应时抛出；确定性失败，不可重试，
 * 整批拒绝且不返回部分 wire material。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
public class KeyprovEnvelopeIntegrityException extends RuntimeException {

    public KeyprovEnvelopeIntegrityException(String message) {
        super(message);
    }

    public KeyprovEnvelopeIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
