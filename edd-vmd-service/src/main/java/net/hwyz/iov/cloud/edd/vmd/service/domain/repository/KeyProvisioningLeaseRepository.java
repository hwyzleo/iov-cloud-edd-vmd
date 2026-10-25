package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.KeyProvisioningLease;

import java.util.List;

/**
 * KL1 交付租约仓储接口（VMD-DSN-CR-069）
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
public interface KeyProvisioningLeaseRepository {

    /**
     * 按主键查询租约
     */
    KeyProvisioningLease selectById(Long id);

    /**
     * 按批次 + 租约序号查询
     */
    KeyProvisioningLease selectByProvisioningIdAndSeq(Long provisioningId, Integer leaseSeq);

    /**
     * 按 messageId 查询（唯一性校验）
     */
    KeyProvisioningLease selectByMessageId(String messageId);

    /**
     * 按批次查询全部租约（审计/对账）
     */
    List<KeyProvisioningLease> selectListByProvisioningId(Long provisioningId);

    /**
     * 本批次当前最大租约序号
     */
    Integer maxLeaseSeq(Long provisioningId);

    /**
     * 插入租约（返回主键）
     */
    int insert(KeyProvisioningLease lease);

    /**
     * 更新租约（乐观锁由 SQL row_version 控制）
     */
    int update(KeyProvisioningLease lease);
}
