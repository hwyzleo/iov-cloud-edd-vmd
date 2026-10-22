package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 逻辑材料身份冲突异常（VMD-DSN-CR-066 §8）。
 * <p>
 * 相同 keyId 但不可变元数据不一致（上下文/rootKeyVersion/derivationVersion/kmsKeyRef/算法/KCV/policyVersion），
 * 属数据一致性或安全冲突，<strong>不可重试</strong>（DO_NOT_RETRY）；DuplicateKey 后身份完全一致属于
 * claim-or-reuse 成功路径，不抛本异常。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyLogicalIdentityConflict extends VmdBaseException {

    public BusinessKeyLogicalIdentityConflict(String detail) {
        super(VmdErrorCode.KEYPROV_LOGICAL_IDENTITY_CONFLICT, detail);
    }

    public BusinessKeyLogicalIdentityConflict() {
        super(VmdErrorCode.KEYPROV_LOGICAL_IDENTITY_CONFLICT);
    }
}
