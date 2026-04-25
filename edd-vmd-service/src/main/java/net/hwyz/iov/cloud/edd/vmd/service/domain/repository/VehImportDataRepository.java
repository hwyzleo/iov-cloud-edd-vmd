package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;

import java.util.List;
import java.util.Map;

/**
 * 车辆导入数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehImportDataRepository {

    /**
     * 根据条件查询车辆导入数据列表
     *
     * @param map 查询条件
     * @return 车辆导入数据列表
     */
    List<VehicleImportData> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆导入数据
     *
     * @param id 主键ID
     * @return 车辆导入数据
     */
    VehicleImportData selectById(Long id);

    /**
     * 根据批次号查询车辆导入数据
     *
     * @param batchNum 批次号
     * @return 车辆导入数据
     */
    VehicleImportData selectByBatchNum(String batchNum);

    /**
     * 新增车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 影响行数
     */
    int insert(VehicleImportData vehicleImportData);

    /**
     * 修改车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 影响行数
     */
    int update(VehicleImportData vehicleImportData);

    /**
     * 批量物理删除车辆导入数据
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
