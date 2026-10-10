package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplayItem;

import java.util.List;

/**
 * 零件导入后置处理重放动作明细仓储接口
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public interface PartImportPostProcessReplayItemRepository {

    PartImportPostProcessReplayItem selectById(Long id);

    int insert(PartImportPostProcessReplayItem item);

    int update(PartImportPostProcessReplayItem item);

    /**
     * 按 (replayId, partCode, sn, actionType) 查询动作明细
     *
     * @param replayId   重放任务ID
     * @param partCode   零件编码
     * @param sn         零件序列号
     * @param actionType 动作类型
     * @return 动作明细
     */
    PartImportPostProcessReplayItem selectByUniqueKey(String replayId, String partCode, String sn, String actionType);

    /**
     * 查询指定重放任务下的全部动作明细
     *
     * @param replayId 重放任务ID
     * @return 动作明细列表
     */
    List<PartImportPostProcessReplayItem> selectListByReplayId(String replayId);

    /**
     * 查询指定重放任务下失败或未完成的动作明细（retryFailedOnly 播种）
     *
     * @param replayId 重放任务ID
     * @return 失败或未完成的动作明细列表
     */
    List<PartImportPostProcessReplayItem> selectRetryableByReplayId(String replayId);
}
