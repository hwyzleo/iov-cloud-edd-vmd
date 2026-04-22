package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBrandDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆品牌表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehBrandDao extends BaseDao<VmdVehBrandDo, Long> {

    /**
     * 通过code查询车辆品牌信息
     *
     * @param code 车辆品牌编码
     * @return 车辆品牌信息
     */
    VmdVehBrandDo selectPoByCode(String code);

}
