package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆-零件绑定关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Mapper
public interface VehiclePartMapper extends BaseDao<VehiclePartPo, Long> {

    /**
     * 根据车架号和零件实例ID查询活跃绑定
     *
     * @param vin 车架号
     * @param partId 零件实例ID
     * @return 绑定信息
     */
    VehiclePartPo selectActiveByVinAndPartId(String vin, Long partId);

    /**
     * 根据车架号和车载节点代码查询活跃绑定
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码
     * @return 绑定信息
     */
    VehiclePartPo selectActiveByVinAndVehicleNodeCode(String vin, String vehicleNodeCode);

    /**
     * 根据零件实例ID查询活跃绑定
     *
     * @param partId 零件实例ID
     * @return 绑定信息
     */
    VehiclePartPo selectActiveByPartId(Long partId);

}
