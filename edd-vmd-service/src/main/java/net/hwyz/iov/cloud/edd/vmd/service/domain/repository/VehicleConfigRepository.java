package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;

import java.util.List;
import java.util.Map;

/**
 * 车辆配置数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleConfigRepository {

    /**
     * 根据条件查询车辆配置列表
     *
     * @param map 查询条件
     * @return 车辆配置列表
     */
    List<VehicleConfig> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆配置
     *
     * @param id 主键ID
     * @return 车辆配置
     */
    VehicleConfig selectById(Long id);

    /**
     * 新增车辆配置
     *
     * @param vehicleConfig 车辆配置
     * @return 影响行数
     */
    int insert(VehicleConfig vehicleConfig);

    /**
     * 修改车辆配置
     *
     * @param vehicleConfig 车辆配置
     * @return 影响行数
     */
    int update(VehicleConfig vehicleConfig);

    /**
     * 批量物理删除车辆配置
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据条件查询车辆配置项列表
     *
     * @param map 查询条件
     * @return 车辆配置项列表
     */
    List<VehicleConfigItem> selectConfigItemByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆配置项
     *
     * @param id 主键ID
     * @return 车辆配置项
     */
    VehicleConfigItem selectConfigItemById(Long id);

    /**
     * 新增车辆配置项
     *
     * @param vehicleConfigItem 车辆配置项
     * @return 影响行数
     */
    int insertConfigItem(VehicleConfigItem vehicleConfigItem);

    /**
     * 修改车辆配置项
     *
     * @param vehicleConfigItem 车辆配置项
     * @return 影响行数
     */
    int updateConfigItem(VehicleConfigItem vehicleConfigItem);

    /**
     * 批量物理删除车辆配置项
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteConfigItem(Long[] ids);

}
