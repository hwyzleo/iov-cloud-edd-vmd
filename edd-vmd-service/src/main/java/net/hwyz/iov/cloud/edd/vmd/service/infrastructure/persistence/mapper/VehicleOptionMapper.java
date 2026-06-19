package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 单车选项值快照表 DAO
 * </p>
 *
 * @author VMD-DSN-CR-030 / US-043
 * @since 2026-06-19
 */
@Mapper
public interface VehicleOptionMapper extends BaseDao<VehicleOptionPo, Long> {

}
