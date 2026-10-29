package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleInfoChangeLog;

import java.util.List;

/**
 * 车辆基础信息属性变更历史数据仓库接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
public interface VehicleInfoChangeLogRepository {

    /**
     * 新增变更日志
     *
     * @param changeLog 变更日志
     * @return 影响行数
     */
    int insert(VehicleInfoChangeLog changeLog);

    /**
     * 按车辆基础信息ID与业务版本查询变更日志
     *
     * @param vehicleId 车辆基础信息ID
     * @param rowVersion 业务本体提交版本
     * @return 变更日志（可能为 null）
     */
    VehicleInfoChangeLog selectByVehicleIdAndRowVersion(Long vehicleId, Integer rowVersion);

    /**
     * 查询车辆基础信息变更日志（按时间倒序）
     *
     * @param vehicleId 车辆基础信息ID
     * @return 变更日志列表
     */
    List<VehicleInfoChangeLog> selectByVehicleId(Long vehicleId);

    /**
     * 按车架号查询变更日志（按时间倒序）
     *
     * @param vin 车架号
     * @return 变更日志列表
     */
    List<VehicleInfoChangeLog> selectByVin(String vin);

}
