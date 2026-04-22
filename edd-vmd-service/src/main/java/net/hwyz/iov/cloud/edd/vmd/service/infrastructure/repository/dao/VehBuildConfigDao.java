package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBuildConfigDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆车型配置表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Mapper
public interface VehBuildConfigDao extends BaseDao<VmdVehBuildConfigDo, Long> {

    /**
     * 通过code查询车型配置信息
     *
     * @param code 车型配置编码
     * @return 车型配置信息
     */
    VmdVehBuildConfigDo selectPoByCode(String code);

}
