package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 首次激活事实查询不可用（VMD-DSN-CR-077 / D-VMD-077-09，错误码 806137）
 * <p>
 * fail-closed：查询技术失败（存储不可用 / 超时 / 未知异常）时抛出，
 * <strong>严禁</strong>降级为「无事实（NOT_RECORDED）」，避免调用方把不可用当成未激活。
 *
 * @author hwyz_leo
 */
@Slf4j
public class ActivationFactQueryUnavailableException extends VmdBaseException {

    public ActivationFactQueryUnavailableException(String detail) {
        super(VmdErrorCode.ACTIVATION_FACT_QUERY_UNAVAILABLE, detail);
        log.error("首次激活事实查询不可用（fail-closed）: {}", detail);
    }

}
