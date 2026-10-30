package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 车载节点设备类别解析失败异常（CR-072）
 * <p>
 * 按最终 vehicleNodeCode 解析 MDM 本地投影失败。区分可恢复与数据治理两类：
 * - retryable=true：投影尚未到达 / 同步未就绪 / 读取异常 —— 可重试（有次数上限，见重导 runbook）；
 * - retryable=false：已确认的数据治理问题（类别空、trim 后超长、节点码非法）。
 * 证据不足时记录"来源未就绪/待治理"，不直接认定永久非法。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@Getter
@Slf4j
public class DeviceCategoryResolveException extends VmdBaseException {

    private final String nodeCode;
    private final boolean retryable;

    public DeviceCategoryResolveException(String nodeCode, boolean retryable, String reason) {
        super(VmdErrorCode.DEVICE_CATEGORY_RESOLVE_FAILED,
                "节点[" + nodeCode + "]设备类别解析失败: " + reason + (retryable ? "（可重试）" : "（待治理）"));
        this.nodeCode = nodeCode;
        this.retryable = retryable;
        log.warn("节点[{}]设备类别解析失败: reason={}, retryable={}", nodeCode, reason, retryable);
    }

}
