package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayItemPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 车辆导入事件补发逐项动作审计表 DAO
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface VehImportEventReplayItemMapper extends BaseDao<VehImportEventReplayItemPo, Long> {

    /**
     * 按 (replayId, actionType, aggregateType, aggregateId, aggregateVersion) 查询动作明细
     *
     * @param replayId         补发任务ID
     * @param actionType       动作类型
     * @param aggregateType    聚合类型
     * @param aggregateId      聚合ID
     * @param aggregateVersion 聚合版本
     * @return 动作明细
     */
    @Select("SELECT * FROM tb_veh_import_event_replay_item WHERE replay_id = #{replayId} "
            + "AND action_type = #{actionType} AND aggregate_type = #{aggregateType} "
            + "AND aggregate_id = #{aggregateId} AND aggregate_version = #{aggregateVersion} AND row_valid = 1")
    VehImportEventReplayItemPo selectPoByUniqueKey(@Param("replayId") String replayId,
                                                   @Param("actionType") String actionType,
                                                   @Param("aggregateType") String aggregateType,
                                                   @Param("aggregateId") String aggregateId,
                                                   @Param("aggregateVersion") Long aggregateVersion);

    /**
     * 查询指定补发任务下的全部动作明细
     *
     * @param replayId 补发任务ID
     * @return 动作明细列表
     */
    @Select("SELECT * FROM tb_veh_import_event_replay_item WHERE replay_id = #{replayId} AND row_valid = 1")
    List<VehImportEventReplayItemPo> selectPoListByReplayId(@Param("replayId") String replayId);
}
