package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleInfoChangeLogConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleInfoChangeLogMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleInfoChangeLogPo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车辆基础信息属性变更历史数据仓库实现（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleInfoChangeLogRepositoryImpl implements VehicleInfoChangeLogRepository {

    private final VehicleInfoChangeLogMapper vehicleInfoChangeLogMapper;

    /**
     * 审计字段兜底填充（不依赖框架自动填充，保证 NOT NULL 列可写）
     */
    private void fillAudit(net.hwyz.iov.cloud.framework.mysql.po.BasePo po) {
        if (po.getCreateTime() == null) {
            po.setCreateTime(new java.util.Date());
        }
        if (po.getModifyTime() == null) {
            po.setModifyTime(new java.util.Date());
        }
        if (po.getRowVersion() == null) {
            po.setRowVersion(1);
        }
        if (po.getRowValid() == null) {
            po.setRowValid(true);
        }
    }

    @Override
    public int insert(VehicleInfoChangeLog changeLog) {
        VehicleInfoChangeLogPo po = VehicleInfoChangeLogConverter.INSTANCE.fromDomain(changeLog);
        fillAudit(po);
        int result = vehicleInfoChangeLogMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            changeLog.setId(po.getId());
        }
        return result;
    }

    @Override
    public VehicleInfoChangeLog selectByVehicleIdAndRowVersion(Long vehicleId, Integer rowVersion) {
        VehicleInfoChangeLogPo po = vehicleInfoChangeLogMapper.selectByVehicleIdAndRowVersion(vehicleId, rowVersion);
        return VehicleInfoChangeLogConverter.INSTANCE.toDomain(po);
    }

    @Override
    public List<VehicleInfoChangeLog> selectByVehicleId(Long vehicleId) {
        List<VehicleInfoChangeLogPo> poList = vehicleInfoChangeLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VehicleInfoChangeLogPo>()
                        .eq(VehicleInfoChangeLogPo::getVehicleId, vehicleId)
                        .orderByDesc(VehicleInfoChangeLogPo::getChangeTime));
        return VehicleInfoChangeLogConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public List<VehicleInfoChangeLog> selectByVin(String vin) {
        List<VehicleInfoChangeLogPo> poList = vehicleInfoChangeLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VehicleInfoChangeLogPo>()
                        .eq(VehicleInfoChangeLogPo::getVin, vin)
                        .orderByDesc(VehicleInfoChangeLogPo::getChangeTime));
        return VehicleInfoChangeLogConverter.INSTANCE.toDomainList(poList);
    }

}
