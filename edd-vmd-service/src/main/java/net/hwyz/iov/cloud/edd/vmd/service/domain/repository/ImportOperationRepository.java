package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一导入操作数据仓库接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
public interface ImportOperationRepository {

    // ==================== 主表 ====================

    /**
     * 新增操作
     *
     * @param operation 操作
     * @return 影响行数
     */
    int insert(ImportOperation operation);

    /**
     * 按操作号查询
     *
     * @param operationId 操作号
     * @return 操作（可能为 null）
     */
    ImportOperation selectByOperationId(String operationId);

    /**
     * 查询同 domain+importData 的 active 操作（互斥检查）
     *
     * @param domain 操作域
     * @param importDataId 导入数据ID
     * @return active 操作（PENDING/RUNNING/RECOVERING），无则 null
     */
    ImportOperation selectActiveByDomainAndImportDataId(String domain, Long importDataId);

    /**
     * 认领操作（PENDING 或 RECOVERING 且租约过期；fenceToken 单调递增）
     *
     * @param operationId 操作号
     * @param owner 执行器标识
     * @param leaseUntil 租约到期
     * @return 影响行数（0=认领失败）
     */
    int claim(String operationId, String owner, LocalDateTime leaseUntil);

    /**
     * 续租（RUNNING 且 owner 匹配）
     *
     * @param operationId 操作号
     * @param owner 执行器标识
     * @param leaseUntil 新租约到期
     * @return 影响行数（0=失败）
     */
    int renewLease(String operationId, String owner, LocalDateTime leaseUntil);

    /**
     * 完成操作（RUNNING 且 owner 匹配 → 终态，清租约）
     *
     * @param operationId 操作号
     * @param owner 执行器标识
     * @param status 终态（SUCCEEDED/PARTIAL_FAILED/FAILED）
     * @param countersJson 阶段计数
     * @return 影响行数（0=失败）
     */
    int finish(String operationId, String owner, String status, String countersJson);

    /**
     * 核验 fence（业务短事务提交前置：owner 与 fenceToken 匹配且租约未过期）
     *
     * @param operationId 操作号
     * @param owner 执行器标识
     * @param fenceToken 预期 fence 令牌
     * @return true=可提交
     */
    boolean verifyFence(String operationId, String owner, Long fenceToken);

    /**
     * 查询失联操作（状态+租约过期，恢复扫描用）
     *
     * @param now 当前时刻
     * @return 失联操作列表
     */
    List<ImportOperation> selectExpired(LocalDateTime now);

    /**
     * 查询最近操作（管理后台列表）
     *
     * @param domain 操作域（可空）
     * @param operatorId 操作人（可空）
     * @param status 状态（可空）
     * @param limit 条数
     * @return 操作列表
     */
    List<ImportOperation> selectRecent(String domain, String operatorId, String status, int limit);

    /**
     * 更新操作（通用，含状态/摘要等）
     *
     * @param operation 操作
     * @return 影响行数
     */
    int update(ImportOperation operation);

    // ==================== 明细表 ====================

    /**
     * 新增明细
     *
     * @param item 明细
     * @return 影响行数
     */
    int insertItem(ImportOperationItem item);

    /**
     * 批量新增明细
     *
     * @param items 明细列表
     * @return 影响行数
     */
    int batchInsertItems(List<ImportOperationItem> items);

    /**
     * 更新明细（状态/检查点/错误等）
     *
     * @param item 明细
     * @return 影响行数
     */
    int updateItem(ImportOperationItem item);

    /**
     * 按操作号与逐项幂等键查询
     *
     * @param operationId 操作号
     * @param itemKey 逐项幂等键
     * @return 明细（可能为 null）
     */
    ImportOperationItem selectItemByKey(String operationId, byte[] itemKey);

    /**
     * 查询操作明细（状态筛选 + 游标分页）
     *
     * @param operationId 操作号
     * @param status 状态（可空）
     * @param cursorId 游标（上一页最后 id，可空）
     * @param limit 条数
     * @return 明细列表
     */
    List<ImportOperationItem> selectItems(String operationId, String status, Long cursorId, int limit);

}
