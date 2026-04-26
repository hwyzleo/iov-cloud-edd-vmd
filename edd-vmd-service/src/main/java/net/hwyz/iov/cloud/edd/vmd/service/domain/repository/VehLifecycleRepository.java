package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;

import java.util.List;
import java.util.Map;

/**
 * 车辆生命周期数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehLifecycleRepository {

    /**
     * 根据条件查询车辆生命周期列表
     *
     * @param map 查询条件
     * @return 车辆生命周期列表
     */
    List<VehicleLifecycle> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆生命周期
     *
     * @param id 主键ID
     * @return 车辆生命周期
     */
    VehicleLifecycle selectById(Long id);

    /**
     * 根据车架号查询车辆生命周期列表
     *
     * @param vin 车架号
     * @return 车辆生命周期列表
     */
    List<VehicleLifecycle> selectByVin(String vin);

    /**
     * 新增车辆生命周期
     *
     * @param vehicleLifecycle 车辆生命周期
     * @return 影响行数
     */
    int insert(VehicleLifecycle vehicleLifecycle);

    /**
     * 修改车辆生命周期
     *
     * @param vehicleLifecycle 车辆生命周期
     * @return 影响行数
     */
    int update(VehicleLifecycle vehicleLifecycle);

    /**
     * 批量物理删除车辆生命周期
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据车架号物理删除车辆生命周期
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int physicalDeleteByVin(String vin);

}
