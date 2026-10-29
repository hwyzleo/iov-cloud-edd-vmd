package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleInfoChangeLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 车辆基础信息属性变更历史表 Mapper 接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface VehicleInfoChangeLogMapper extends BaseMapper<VehicleInfoChangeLogPo> {

    /**
     * 按车辆基础信息ID与业务版本查询
     *
     * @param vehicleId 车辆基础信息ID
     * @param rowVersion 业务版本
     * @return 变更日志
     */
    VehicleInfoChangeLogPo selectByVehicleIdAndRowVersion(@Param("vehicleId") Long vehicleId, @Param("rowVersion") Integer rowVersion);

}
