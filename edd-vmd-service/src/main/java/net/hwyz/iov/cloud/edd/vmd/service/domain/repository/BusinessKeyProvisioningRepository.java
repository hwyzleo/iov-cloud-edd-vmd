package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务密钥多用途开通批次仓储接口（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public interface BusinessKeyProvisioningRepository {

    /**
     * 根据主键查询批次
     */
    BusinessKeyProvisioning selectById(Long id);

    /**
     * 根据 requestId 查询批次
     */
    BusinessKeyProvisioning selectByRequestId(String requestId);

    /**
     * 根据 requestId 查询批次并加行锁（完成事务串行化，须在事务内调用）
     */
    BusinessKeyProvisioning selectByRequestIdForUpdate(String requestId);

    /**
     * 查询同设备+业务域未终结批次
     */
    BusinessKeyProvisioning selectOpenByDeviceAndDomain(String deviceSn, String businessDomain);

    /**
     * 查询同设备+业务域全部未终结批次（证书维度寻址，VMD-DSN-CR-065 §6.2）
     * <p>
     * 返回完整集合（不 LIMIT）：0 条返回空列表，&gt;1 条由调用方判定数据一致性异常并 fail-closed。
     */
    List<BusinessKeyProvisioning> selectOpenListByDeviceAndDomain(String deviceSn, String businessDomain);

    /**
     * 查询同设备+业务域历史终结批次（COMPLETED/FAILED/EXPIRED，按 modify_time DESC, create_time DESC）
     * <p>仅供审计展示，不进入清理选择（VMD-DSN-CR-065 §6.2 includeTerminal=true）。
     */
    List<BusinessKeyProvisioning> selectTerminalListByDeviceAndDomain(String deviceSn, String businessDomain);

    /**
     * 按状态扫描修改时间早于 before 的批次（超时/对账扫批）
     */
    List<BusinessKeyProvisioning> selectListByStatusBefore(BusinessKeyProvisioningState status, LocalDateTime before, int limit);

    /**
     * KL1：扫描未终结且 ack_deadline_at ≤ now 的批次（sweeper ≤5s，VMD-DSN-CR-069 §4）
     */
    List<BusinessKeyProvisioning> selectKl1PendingBeforeDeadline(LocalDateTime now, int limit);

    /**
     * KL1 存量迁移候选：非 KL1 未终结批次且已持久化最终 Response（VMD-DSN-CR-069 §5）
     */
    List<BusinessKeyProvisioning> selectKl1MigrationCandidates(int limit);

    /**
     * 插入批次
     */
    int insert(BusinessKeyProvisioning entity);

    /**
     * 更新批次（乐观锁由 SQL row_version 控制）
     */
    int update(BusinessKeyProvisioning entity);
}
