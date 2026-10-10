package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;

import java.util.List;

/**
 * 车辆导入事件补发主任务审计仓储接口
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * VMD-DSN-CR-057: 扩展为按 ImportType 路由的动作补偿主任务
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
     * 查询指定车辆导入数据ID下是否有执行中的主任务
     * <p>
     * VMD-DSN-CR-057: 同一导入记录同时仅允许一个 RUNNING（不再按 eventType 区分）
     *
     * @param vehImportDataId 车辆导入数据ID
     * @return 执行中的任务数量
     */
    long countRunningByVehImportDataId(Long vehImportDataId);

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
