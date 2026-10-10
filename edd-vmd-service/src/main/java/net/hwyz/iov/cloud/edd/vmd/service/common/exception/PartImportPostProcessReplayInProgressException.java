package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件导入后置处理重放进行中异常
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
public class PartImportPostProcessReplayInProgressException extends VmdBaseException {

    public PartImportPostProcessReplayInProgressException(Long partImportDataId) {
        super(VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_IN_PROGRESS);
        log.warn("零件导入记录[{}]正在重放后置处理，请勿重复操作", partImportDataId);
    }
}
