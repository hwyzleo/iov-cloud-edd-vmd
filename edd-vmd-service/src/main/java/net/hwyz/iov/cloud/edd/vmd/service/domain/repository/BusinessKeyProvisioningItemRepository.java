package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;

import java.util.List;

/**
 * 业务密钥开通批次 ACK 明细仓储接口（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public interface BusinessKeyProvisioningItemRepository {

    /**
     * 根据主键查询明细
     */
    BusinessKeyProvisioningItem selectById(Long id);

    /**
     * 按批次查询全部用途明细（按 purpose 升序）
     */
    List<BusinessKeyProvisioningItem> selectByProvisioningId(Long provisioningId);

    /**
     * 按批次查询全部用途明细并加行锁（VMD-DSN-CR-065 维护终止锁顺序，须在事务内调用）
     * <p>按 purpose 升序锁定，保证 Request/ACK/Status 对账与运维终止锁顺序一致、避免死锁。
     */
    List<BusinessKeyProvisioningItem> selectByProvisioningIdForUpdate(Long provisioningId);

    /**
     * 按批次与用途查询明细
     */
    BusinessKeyProvisioningItem selectByProvisioningIdAndPurpose(Long provisioningId, String purpose);

    /**
     * 按目录行主键查询关联明细（CR-066 身份一致性校验：比较既有 canonical 行的 KCV 证据）。
     * <p>同一目录行可能被多个批次 item 引用；返回首个（按 id 升序）。
     */
    BusinessKeyProvisioningItem selectByDirectoryKeyId(Long directoryKeyId);

    /**
     * 重挂目录行（CR-066 §6.3 claim-or-reuse）：item 指向 canonical 目录行。
     * <p>在同一事务中调用；保留原占位 item 的 id 与冲突原因到审计（由调用方写入日志/审计）。
     */
    int rebindDirectoryKey(Long itemId, Long directoryKeyId, String keyId, Long businessKeyVersion);

    /**
     * 插入明细
     */
    int insert(BusinessKeyProvisioningItem entity);

    /**
     * 更新明细（乐观锁由 SQL row_version 控制）
     */
    int update(BusinessKeyProvisioningItem entity);
}
