package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartBindingFactPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 根据车架号物理删除绑定关系
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int physicalDeleteByVin(String vin);

    /**
     * 按设备序列号与设备类别查询 active 绑定事实聚合（CR-076）
     * <p>
     * 仅匹配 bind_state=1 且 row_valid=1 的绑定事实；设备类别取实例快照
     * tb_part_info.device_category，快照为空时回退按实例车载节点解析的 MDM 节点类别。
     * 结果按 VIN 分组，返回该 VIN 的命中条数与最后更新时间。
     * </p>
     *
     * @param sn             设备序列号（tb_part_info.sn）
     * @param deviceCategory 设备类别（如 IVI）
     * @return 按 VIN 分组的聚合结果列表
     */
    List<VehiclePartBindingFactPo> selectActiveBindingFactsBySnAndDeviceCategory(
            @Param("sn") String sn, @Param("deviceCategory") String deviceCategory);

}
