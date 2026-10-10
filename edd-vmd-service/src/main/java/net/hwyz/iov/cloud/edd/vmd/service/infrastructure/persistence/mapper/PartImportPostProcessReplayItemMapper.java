package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportPostProcessReplayItemPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 零件导入后置处理重放动作明细表 DAO
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface PartImportPostProcessReplayItemMapper extends BaseDao<PartImportPostProcessReplayItemPo, Long> {

    /**
     * 按 (replayId, partCode, sn, actionType) 查询动作明细
     *
     * @param replayId   重放任务ID
     * @param partCode   零件编码
     * @param sn         零件序列号
     * @param actionType 动作类型
     * @return 动作明细
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay_item WHERE replay_id = #{replayId} "
            + "AND part_code = #{partCode} AND sn = #{sn} AND action_type = #{actionType} AND row_valid = 1")
    PartImportPostProcessReplayItemPo selectPoByUniqueKey(@Param("replayId") String replayId,
                                                          @Param("partCode") String partCode,
                                                          @Param("sn") String sn,
                                                          @Param("actionType") String actionType);

    /**
     * 查询指定重放任务下的全部动作明细
     *
     * @param replayId 重放任务ID
     * @return 动作明细列表
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay_item WHERE replay_id = #{replayId} AND row_valid = 1")
    List<PartImportPostProcessReplayItemPo> selectPoListByReplayId(@Param("replayId") String replayId);

    /**
     * 查询指定重放任务下失败或未完成的动作明细
     *
     * @param replayId 重放任务ID
     * @return 失败或未完成的动作明细列表
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay_item WHERE replay_id = #{replayId} AND row_valid = 1 "
            + "AND status IN ('PENDING','RUNNING','FAILED_RETRYABLE','FAILED_FINAL')")
    List<PartImportPostProcessReplayItemPo> selectPoListRetryableByReplayId(@Param("replayId") String replayId);
}
