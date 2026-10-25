package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.KeyProvisioningLeasePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KL1 交付租约表 DAO（VMD-DSN-CR-069）
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Mapper
public interface KeyProvisioningLeaseMapper extends BaseDao<KeyProvisioningLeasePo, Long> {

    /**
     * 按批次 + 租约序号查询
     */
    KeyProvisioningLeasePo selectPoByProvisioningIdAndSeq(@Param("provisioningId") Long provisioningId,
                                                          @Param("leaseSeq") Integer leaseSeq);

    /**
     * 按 messageId 查询（同 messageId 不得对应不同 bytes，唯一约束兜底）
     */
    KeyProvisioningLeasePo selectPoByMessageId(@Param("messageId") String messageId);

    /**
     * 按批次查询全部租约（审计/对账）
     */
    List<KeyProvisioningLeasePo> selectPoListByProvisioningId(@Param("provisioningId") Long provisioningId);

    /**
     * 本批次当前最大租约序号（lease_seq = COALESCE(MAX)+1 原子签发）
     */
    Integer selectMaxLeaseSeq(@Param("provisioningId") Long provisioningId);
}
