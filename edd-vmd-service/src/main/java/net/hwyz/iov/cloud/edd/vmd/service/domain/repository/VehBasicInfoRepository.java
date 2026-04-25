package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePresetOwner;

import java.util.List;
import java.util.Map;

/**
 * 车辆基础信息数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehBasicInfoRepository {

    /**
     * 根据条件查询车辆基础信息列表
     *
     * @param map 查询条件
     * @return 车辆基础信息列表
     */
    List<VehicleBasicInfo> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计车辆基础信息数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆基础信息
     *
     * @param id 主键ID
     * @return 车辆基础信息
     */
    VehicleBasicInfo selectById(Long id);

    /**
     * 根据车架号查询车辆基础信息
     *
     * @param vin 车架号
     * @return 车辆基础信息
     */
    VehicleBasicInfo selectByVin(String vin);

    /**
     * 新增车辆基础信息
     *
     * @param vehicleBasicInfo 车辆基础信息
     * @return 影响行数
     */
    int insert(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 修改车辆基础信息
     *
     * @param vehicleBasicInfo 车辆基础信息
     * @return 影响行数
     */
    int update(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 批量物理删除车辆基础信息
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据车架号查询车辆详细信息
     *
     * @param vin 车架号
     * @return 车辆详细信息列表
     */
    List<VehicleDetail> selectDetailByVin(String vin);

    /**
     * 根据示例查询车辆预设车主列表
     *
     * @param example 示例
     * @return 车辆预设车主列表
     */
    List<VehiclePresetOwner> selectPresetOwnerByExample(VehiclePresetOwner example);

    /**
     * 批量新增车辆详细信息
     *
     * @param detailList 车辆详细信息列表
     * @return 影响行数
     */
    int batchInsertDetail(List<VehicleDetail> detailList);

}
