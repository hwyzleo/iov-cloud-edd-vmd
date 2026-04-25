package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePartHistory;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehiclePartRepository {

    /**
     * 根据条件查询车辆零件列表
     *
     * @param map 查询条件
     * @return 车辆零件列表
     */
    List<VehiclePart> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车辆零件
     *
     * @param id 主键ID
     * @return 车辆零件
     */
    VehiclePart selectById(Long id);

    /**
     * 根据零件号和序列号查询车辆零件
     *
     * @param pn 零件号
     * @param sn 序列号
     * @return 车辆零件
     */
    VehiclePart selectByPnAndSn(String pn, String sn);

    /**
     * 新增车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 影响行数
     */
    int insert(VehiclePart vehiclePart);

    /**
     * 批量新增车辆零件
     *
     * @param vehiclePartList 车辆零件列表
     * @return 影响行数
     */
    int batchInsert(List<VehiclePart> vehiclePartList);

    /**
     * 修改车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 影响行数
     */
    int update(VehiclePart vehiclePart);

    /**
     * 批量物理删除车辆零件
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 新增车辆零件变更历史
     *
     * @param vehiclePartHistory 车辆零件变更历史
     * @return 影响行数
     */
    int insertHistory(VehiclePartHistory vehiclePartHistory);

    /**
     * 批量新增车辆零件变更历史
     *
     * @param vehiclePartHistoryList 车辆零件变更历史列表
     * @return 影响行数
     */
    int batchInsertHistory(List<VehiclePartHistory> vehiclePartHistoryList);

}
