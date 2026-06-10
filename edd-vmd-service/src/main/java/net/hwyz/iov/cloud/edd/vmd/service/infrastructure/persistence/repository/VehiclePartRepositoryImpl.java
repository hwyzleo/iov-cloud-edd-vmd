package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehiclePartConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆-零件绑定关系数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehiclePartRepositoryImpl implements VehiclePartRepository {

    private final VehiclePartMapper vehiclePartMapper;

    @Override
    public List<VehiclePart> selectByMap(Map<String, Object> map) {
        List<VehiclePartPo> poList = vehiclePartMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehiclePartConverter.INSTANCE::toDomain);
    }

    @Override
    public VehiclePart selectById(Long id) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectPoById(id));
    }

    @Override
    public VehiclePart selectActiveByVinAndPartId(String vin, Long partId) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectActiveByVinAndPartId(vin, partId));
    }

    @Override
    public VehiclePart selectActiveByVinAndVehicleNodeCode(String vin, String vehicleNodeCode) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectActiveByVinAndVehicleNodeCode(vin, vehicleNodeCode));
    }

    @Override
    public VehiclePart selectActiveByPartId(Long partId) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectActiveByPartId(partId));
    }

    @Override
    public int insert(VehiclePart vehiclePart) {
        return vehiclePartMapper.insertPo(VehiclePartConverter.INSTANCE.fromDomain(vehiclePart));
    }

    @Override
    public int batchInsert(List<VehiclePart> vehiclePartList) {
        return vehiclePartMapper.batchInsertPo(VehiclePartConverter.INSTANCE.fromDomainList(vehiclePartList));
    }

    @Override
    public int update(VehiclePart vehiclePart) {
        return vehiclePartMapper.updatePo(VehiclePartConverter.INSTANCE.fromDomain(vehiclePart));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehiclePartMapper.batchPhysicalDeletePo(ids);
    }

}
