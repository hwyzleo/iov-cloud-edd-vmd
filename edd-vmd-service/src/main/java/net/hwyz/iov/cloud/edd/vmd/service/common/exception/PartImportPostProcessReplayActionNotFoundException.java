package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件导入后置处理重放动作不存在异常
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
public class PartImportPostProcessReplayActionNotFoundException extends VmdBaseException {

    public PartImportPostProcessReplayActionNotFoundException(String actionType) {
        super(VmdErrorCode.PART_IMPORT_POST_PROCESS_REPLAY_ACTION_NOT_FOUND, "不支持的零件导入后置处理动作: " + actionType);
        log.warn("不支持的零件导入后置处理动作: {}", actionType);
    }
}
