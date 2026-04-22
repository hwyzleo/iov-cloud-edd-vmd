package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehPresetOwnerDo;
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
public interface VehPresetOwnerDao extends BaseDao<VmdVehPresetOwnerDo, Long> {

}
