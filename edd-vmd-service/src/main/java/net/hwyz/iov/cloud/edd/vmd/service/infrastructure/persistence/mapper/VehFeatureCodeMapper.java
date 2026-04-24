package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆特征值表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-06
 */
@Mapper
public interface VehFeatureCodeMapper extends BaseDao<VehFeatureCodePo, Long> {

    /**
     * 根据车辆特征值代码获取车辆特征值信息
     *
     * @param code 车辆特征值代码
     * @return 车辆特征值信息
     */
    VehFeatureCodePo selectPoByCode(String code);

}
