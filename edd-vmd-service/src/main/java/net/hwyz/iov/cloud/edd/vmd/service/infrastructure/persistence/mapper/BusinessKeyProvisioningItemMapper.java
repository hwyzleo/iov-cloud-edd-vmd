package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningItemPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务密钥开通批次 ACK 明细表 DAO（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Mapper
public interface BusinessKeyProvisioningItemMapper extends BaseDao<BusinessKeyProvisioningItemPo, Long> {

    /**
     * 按批次查询全部用途明细
     */
    List<BusinessKeyProvisioningItemPo> selectPoListByProvisioningId(@Param("provisioningId") Long provisioningId);

    /**
     * 按批次查询全部用途明细并加行锁（VMD-DSN-CR-065 维护终止锁顺序，按 purpose 升序）
     */
    List<BusinessKeyProvisioningItemPo> selectPoListByProvisioningIdForUpdate(@Param("provisioningId") Long provisioningId);

    /**
     * 按批次与用途查询
     */
    BusinessKeyProvisioningItemPo selectPoByProvisioningIdAndPurpose(@Param("provisioningId") Long provisioningId,
                                                                     @Param("purpose") String purpose);
}
