package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;

/**
 * 车辆安全常量仓储接口
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
public interface VehSecurityConstantRepository {

    /**
     * 根据车架号查询
     *
     * @param vin 车架号
     * @return 车辆安全常量
     */
    VehSecurityConstant selectByVin(String vin);

    /**
     * 插入记录
     *
     * @param vehSecurityConstant 车辆安全常量
     * @return 影响行数
     */
    int insert(VehSecurityConstant vehSecurityConstant);

    /**
     * 更新记录
     *
     * @param vehSecurityConstant 车辆安全常量
     * @return 影响行数
     */
    int update(VehSecurityConstant vehSecurityConstant);

    /**
     * 根据车架号统计数量
     *
     * @param vin 车架号
     * @return 数量
     */
    long countByVin(String vin);
}
