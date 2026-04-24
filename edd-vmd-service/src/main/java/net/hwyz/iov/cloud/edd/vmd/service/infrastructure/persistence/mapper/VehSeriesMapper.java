package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo;
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
public interface VehSeriesMapper extends BaseDao<VehSeriesPo, Long> {

    /**
     * 通过code查询车系信息
     *
     * @param code 车系编码
     * @return 车系信息
     */
    VehSeriesPo selectPoByCode(String code);

}
