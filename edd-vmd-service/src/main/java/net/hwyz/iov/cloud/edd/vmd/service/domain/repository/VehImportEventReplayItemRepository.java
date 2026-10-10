package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplayItem;

import java.util.List;

/**
 * 车辆导入事件补发逐项动作审计仓储接口
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public interface VehImportEventReplayItemRepository {

    VehImportEventReplayItem selectById(Long id);

    int insert(VehImportEventReplayItem item);

    int update(VehImportEventReplayItem item);

    /**
     * 按 (replayId, actionType, aggregateType, aggregateId, aggregateVersion) 查询动作明细
     *
     * @param replayId        补发任务ID
     * @param actionType      动作类型
     * @param aggregateType   聚合类型
     * @param aggregateId     聚合ID
     * @param aggregateVersion 聚合版本
     * @return 动作明细
     */
    VehImportEventReplayItem selectByUniqueKey(String replayId, String actionType, String aggregateType,
                                               String aggregateId, Long aggregateVersion);

    /**
     * 查询指定补发任务下的全部动作明细
     *
     * @param replayId 补发任务ID
     * @return 动作明细列表
     */
    List<VehImportEventReplayItem> selectListByReplayId(String replayId);
}
