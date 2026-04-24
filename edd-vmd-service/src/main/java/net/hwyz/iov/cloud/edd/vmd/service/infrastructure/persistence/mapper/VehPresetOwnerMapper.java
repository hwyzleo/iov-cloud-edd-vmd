package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPresetOwnerPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆预设车主表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-25
 */
@Mapper
public interface VehPresetOwnerMapper extends BaseDao<VehPresetOwnerPo, Long> {

}
