package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehSeriesDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆车系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehSeriesDao extends BaseDao<VmdVehSeriesDo, Long> {

    /**
     * 通过code查询车系信息
     *
     * @param code 车系编码
     * @return 车系信息
     */
    VmdVehSeriesDo selectPoByCode(String code);

}
