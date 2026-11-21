package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.DeviceBindingFact;

import java.util.List;
import java.util.Map;

/**
 * 车辆-零件绑定关系数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehiclePartRepository {

    /**
     * 根据条件查询绑定关系列表
     *
     * @param map 查询条件
     * @return 绑定关系列表
     */
    List<VehiclePart> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询绑定关系
     *
     * @param id 主键ID
     * @return 绑定关系
     */
    VehiclePart selectById(Long id);

    /**
     * 根据车架号和零件实例ID查询活跃绑定
     *
     * @param vin 车架号
     * @param partId 零件实例ID
     * @return 绑定关系
     */
    VehiclePart selectActiveByVinAndPartId(String vin, Long partId);

    /**
     * 根据车架号和车载节点代码查询活跃绑定
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码
     * @return 绑定关系
     */
    VehiclePart selectActiveByVinAndVehicleNodeCode(String vin, String vehicleNodeCode);

    /**
     * 根据零件实例ID查询活跃绑定
     *
     * @param partId 零件实例ID
     * @return 绑定关系
     */
    VehiclePart selectActiveByPartId(Long partId);

    /**
     * 新增绑定关系
     *
     * @param vehiclePart 绑定关系
     * @return 影响行数
     */
    int insert(VehiclePart vehiclePart);

    /**
     * 批量新增绑定关系
     *
     * @param vehiclePartList 绑定关系列表
     * @return 影响行数
     */
    int batchInsert(List<VehiclePart> vehiclePartList);

    /**
     * 修改绑定关系
     *
     * @param vehiclePart 绑定关系
     * @return 影响行数
     */
    int update(VehiclePart vehiclePart);

    /**
     * 批量物理删除绑定关系
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据车架号物理删除绑定关系
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int physicalDeleteByVin(String vin);

    /**
     * 按设备序列号与设备类别查询 active 绑定事实聚合（CR-076，只读）
     * <p>
     * 仅匹配 bind_state=1 且 row_valid=1 的绑定事实；设备类别取实例快照，
     * 快照为空时回退按实例车载节点解析的 MDM 节点类别；结果按 VIN 分组。
     * </p>
     *
     * @param sn             设备序列号
     * @param deviceCategory 设备类别
     * @return 按 VIN 分组的聚合结果（无绑定时为空列表）
     */
    List<DeviceBindingFact> selectActiveBindingFactsBySnAndDeviceCategory(String sn, String deviceCategory);

}
