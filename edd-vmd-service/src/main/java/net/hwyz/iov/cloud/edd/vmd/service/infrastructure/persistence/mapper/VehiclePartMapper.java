package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆零件表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-27
 */
@Mapper
public interface VehiclePartMapper extends BaseDao<VehiclePartPo, Long> {

    /**
     * 根据零件编号和序列号查询
     *
     * @param pn 零件编号
     * @param sn 序列号
     * @return 零件信息
     */
    VehiclePartPo selectPoByPnAndSn(String pn, String sn);

}
