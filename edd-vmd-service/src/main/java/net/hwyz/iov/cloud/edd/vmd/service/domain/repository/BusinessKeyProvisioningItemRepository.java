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
     * 插入明细
     */
    int insert(BusinessKeyProvisioningItem entity);

    /**
     * 更新明细（乐观锁由 SQL row_version 控制）
     */
    int update(BusinessKeyProvisioningItem entity);
}
