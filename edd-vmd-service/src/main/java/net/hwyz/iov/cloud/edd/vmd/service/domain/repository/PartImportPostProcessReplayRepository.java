package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplay;

import java.util.List;

/**
 * 零件导入后置处理重放主任务仓储接口
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public interface PartImportPostProcessReplayRepository {

    PartImportPostProcessReplay selectById(Long id);

    PartImportPostProcessReplay selectByReplayId(String replayId);

    int insert(PartImportPostProcessReplay replay);

    int update(PartImportPostProcessReplay replay);

    List<PartImportPostProcessReplay> selectList(PartImportPostProcessReplay replay);

    /**
     * 查询指定零件导入数据ID下执行中的完整重放任务数量
     *
     * @param partImportDataId 零件导入数据ID
     * @return 执行中的重放任务数量
     */
    long countRunningByPartImportDataId(Long partImportDataId);

    /**
     * 查询指定零件导入数据ID下最近一次终态重放（用于 retryFailedOnly 播种）
     *
     * @param partImportDataId 零件导入数据ID
     * @return 最近一次终态重放
     */
    PartImportPostProcessReplay selectLatestTerminalByPartImportDataId(Long partImportDataId);

    /**
     * 查询超时的RUNNING状态记录
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时的记录列表
     */
    List<PartImportPostProcessReplay> selectTimeoutRunningRecords(int timeoutMinutes);

    /**
     * 批量更新超时的RUNNING状态记录为FAILED
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 更新的记录数
     */
    int updateTimeoutRunningToFailed(int timeoutMinutes);
}
