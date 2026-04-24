package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆生命周期表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehLifecycleMapper extends BaseDao<VehLifecyclePo, Long> {

    /**
     * 根据vin批量物理删除车辆生命周期
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int batchPhysicalDeletePoByVin(String vin);

}
