package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportPostProcessReplayPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 零件导入后置处理重放主任务表 DAO
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface PartImportPostProcessReplayMapper extends BaseDao<PartImportPostProcessReplayPo, Long> {

    /**
     * 根据重放请求ID查询
     *
     * @param replayId 重放请求ID
     * @return 主任务
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay WHERE replay_id = #{replayId} AND row_valid = 1")
    PartImportPostProcessReplayPo selectPoByReplayId(@Param("replayId") String replayId);

    /**
     * 查询指定零件导入数据ID下执行中的完整重放任务数量
     *
     * @param partImportDataId 零件导入数据ID
     * @return 执行中的重放任务数量
     */
    @Select("SELECT COUNT(*) FROM tb_part_import_postprocess_replay WHERE part_import_data_id = #{partImportDataId} "
            + "AND status = 'RUNNING' AND row_valid = 1")
    long countRunningByPartImportDataId(@Param("partImportDataId") Long partImportDataId);

    /**
     * 查询指定零件导入数据ID下最近一次终态重放
     *
     * @param partImportDataId 零件导入数据ID
     * @return 最近一次终态重放
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay WHERE part_import_data_id = #{partImportDataId} "
            + "AND status IN ('SUCCESS','PARTIAL_SUCCESS','FAILED') AND row_valid = 1 "
            + "ORDER BY id DESC LIMIT 1")
    PartImportPostProcessReplayPo selectLatestTerminalByPartImportDataId(@Param("partImportDataId") Long partImportDataId);

    /**
     * 查询超时的RUNNING状态记录
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时的记录列表
     */
    @Select("SELECT * FROM tb_part_import_postprocess_replay WHERE status = 'RUNNING' AND row_valid = 1 "
            + "AND create_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<PartImportPostProcessReplayPo> selectTimeoutRunningRecords(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 批量更新超时的RUNNING状态记录为FAILED
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 更新的记录数
     */
    @Update("UPDATE tb_part_import_postprocess_replay SET status = 'FAILED', finished_at = NOW(), "
            + "modify_time = NOW(), row_version = row_version + 1 WHERE status = 'RUNNING' AND row_valid = 1 "
            + "AND create_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int updateTimeoutRunningToFailed(@Param("timeoutMinutes") int timeoutMinutes);
}
