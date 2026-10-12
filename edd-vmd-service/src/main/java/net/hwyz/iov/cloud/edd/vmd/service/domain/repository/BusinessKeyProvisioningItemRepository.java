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
     * 按批次查询全部用途明细
     */
    List<BusinessKeyProvisioningItem> selectByProvisioningId(Long provisioningId);

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
