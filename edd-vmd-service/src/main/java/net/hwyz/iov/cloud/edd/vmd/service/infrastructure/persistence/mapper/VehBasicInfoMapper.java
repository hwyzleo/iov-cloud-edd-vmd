package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆基础信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehBasicInfoMapper extends BaseDao<VehBasicInfoPo, Long> {

    /**
     * 根据车架号查询车辆基础信息
     *
     * @param vin 车架号
     * @return 车辆基础信息
     */
    VehBasicInfoPo selectPoByVin(String vin);

}
