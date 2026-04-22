package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehManufacturerDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆生产厂商表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-23
 */
@Mapper
public interface VehManufacturerDao extends BaseDao<VmdVehManufacturerDo, Long> {

    /**
     * 通过code查询车辆工厂信息
     *
     * @param code 车辆工厂编码
     * @return 车辆工厂信息
     */
    VmdVehManufacturerDo selectPoByCode(String code);

}
