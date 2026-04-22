package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehPlatformDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆平台表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehPlatformDao extends BaseDao<VmdVehPlatformDo, Long> {

    /**
     * 通过code查询车辆平台信息
     *
     * @param code 车辆平台编码
     * @return 车辆平台信息
     */
    VmdVehPlatformDo selectPoByCode(String code);

}
