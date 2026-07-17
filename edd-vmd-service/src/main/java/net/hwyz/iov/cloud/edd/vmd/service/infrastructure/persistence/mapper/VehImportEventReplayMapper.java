package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 车辆导入成功事件补发审计表 DAO
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VehImportEventReplayMapper extends BaseDao<VehImportEventReplayPo, Long> {

    /**
     * 根据补发请求ID查询
     *
     * @param replayId 补发请求ID
     * @return 补发审计记录
     */
    @Select("SELECT * FROM tb_veh_import_event_replay WHERE replay_id = #{replayId} AND row_valid = 1")
    VehImportEventReplayPo selectPoByReplayId(@Param("replayId") String replayId);

    /**
     * 查询指定车辆导入数据ID和事件类型下执行中的任务数量
     *
     * @param vehImportDataId 车辆导入数据ID
     * @param eventType 事件类型
     * @return 执行中的任务数量
     */
    @Select("SELECT COUNT(*) FROM tb_veh_import_event_replay WHERE veh_import_data_id = #{vehImportDataId} AND event_type = #{eventType} AND status = 'RUNNING' AND row_valid = 1")
    long countRunningByVehImportDataIdAndEventType(@Param("vehImportDataId") Long vehImportDataId, @Param("eventType") String eventType);

    /**
     * 查询超时的RUNNING状态记录
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时的记录列表
     */
    @Select("SELECT * FROM tb_veh_import_event_replay WHERE status = 'RUNNING' AND row_valid = 1 AND create_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<VehImportEventReplayPo> selectTimeoutRunningRecords(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 批量更新超时的RUNNING状态记录为FAILED
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 更新的记录数
     */
    @Update("UPDATE tb_veh_import_event_replay SET status = 'FAILED', failure_detail = '执行超时', finished_at = NOW(), modify_time = NOW(), row_version = row_version + 1 WHERE status = 'RUNNING' AND row_valid = 1 AND create_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int updateTimeoutRunningToFailed(@Param("timeoutMinutes") int timeoutMinutes);
}
