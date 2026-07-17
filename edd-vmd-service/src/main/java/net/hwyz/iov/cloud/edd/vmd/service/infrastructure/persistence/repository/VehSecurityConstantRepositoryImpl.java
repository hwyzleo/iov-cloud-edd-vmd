package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehSecurityConstantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehSecurityConstantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import org.springframework.stereotype.Repository;

/**
 * 车辆安全常量仓储实现
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehSecurityConstantRepositoryImpl implements VehSecurityConstantRepository {

    private final VehSecurityConstantMapper vehSecurityConstantMapper;

    @Override
    public VehSecurityConstant selectByVin(String vin) {
        VehSecurityConstantPo po = vehSecurityConstantMapper.selectPoByVin(vin);
        return po != null ? VehSecurityConstantConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public VehSecurityConstant selectByVinAndConstantType(String vin, String constantType) {
        VehSecurityConstantPo po = vehSecurityConstantMapper.selectPoByVinAndConstantType(vin, constantType);
        return po != null ? VehSecurityConstantConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public int insert(VehSecurityConstant vehSecurityConstant) {
        VehSecurityConstantPo po = VehSecurityConstantConverter.INSTANCE.fromDomain(vehSecurityConstant);
        int rows = vehSecurityConstantMapper.insertPo(po);
        vehSecurityConstant.setId(po.getId());
        return rows;
    }

    @Override
    public int update(VehSecurityConstant vehSecurityConstant) {
        VehSecurityConstantPo po = VehSecurityConstantConverter.INSTANCE.fromDomain(vehSecurityConstant);
        return vehSecurityConstantMapper.updatePo(po);
    }

    @Override
    public long countByVin(String vin) {
        return vehSecurityConstantMapper.countPoByVin(vin);
    }

    @Override
    public int physicalDeleteByVin(String vin) {
        return vehSecurityConstantMapper.physicalDeleteByVin(vin);
    }
}
