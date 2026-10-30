package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 重导预览计划陈旧异常（CR-072）
 * <p>
 * 执行必须关联服务端保存的已批准计划：执行期重扫与预览摘要不一致、候选实例/节点/类别/版本
 * 与计划条目不符、计划缺失条目或新实例被并发创建等均判定 PREVIEW_STALE —— 该项不写入，
 * 需重新预览；绝不静默采用新候选或跳过校验。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@Slf4j
public class PreviewStaleException extends VmdBaseException {

    public PreviewStaleException(String detail) {
        super(VmdErrorCode.DEVICE_CATEGORY_PREVIEW_STALE, detail);
        log.warn("重导预览计划陈旧: {}", detail);
    }

}
