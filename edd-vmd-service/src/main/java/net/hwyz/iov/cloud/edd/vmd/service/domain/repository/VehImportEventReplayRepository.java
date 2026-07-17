package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;

import java.util.List;

/**
 * 车辆导入成功事件补发审计仓储接口
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
public interface VehImportEventReplayRepository {

    VehImportEventReplay selectById(Long id);

    VehImportEventReplay selectByReplayId(String replayId);

    int insert(VehImportEventReplay vehImportEventReplay);

    int update(VehImportEventReplay vehImportEventReplay);

    List<VehImportEventReplay> selectList(VehImportEventReplay vehImportEventReplay);

    /**
     * 查询指定车辆导入数据ID和事件类型下是否有执行中的任务
     *
     * @param vehImportDataId 车辆导入数据ID
     * @param eventType 事件类型
     * @return 执行中的任务数量
     */
    long countRunningByVehImportDataIdAndEventType(Long vehImportDataId, String eventType);

    /**
     * 查询超时的RUNNING状态记录
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时的记录列表
     */
    List<VehImportEventReplay> selectTimeoutRunningRecords(int timeoutMinutes);

    /**
     * 批量更新超时的RUNNING状态记录为FAILED
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 更新的记录数
     */
    int updateTimeoutRunningToFailed(int timeoutMinutes);
}
