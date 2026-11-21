package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.DeviceBindingFact;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehiclePartConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartBindingFactPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public int physicalDeleteByVin(String vin) {
        return vehiclePartMapper.physicalDeleteByVin(vin);
    }

    @Override
    public List<DeviceBindingFact> selectActiveBindingFactsBySnAndDeviceCategory(String sn, String deviceCategory) {
        List<VehiclePartBindingFactPo> poList =
                vehiclePartMapper.selectActiveBindingFactsBySnAndDeviceCategory(sn, deviceCategory);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return poList.stream()
                .map(po -> DeviceBindingFact.builder()
                        .vin(po.getVin())
                        .bindingCount(po.getBindingCount() == null ? 0L : po.getBindingCount())
                        .updatedAt(po.getUpdatedAt() == null ? null : po.getUpdatedAt().toInstant())
                        .build())
                .collect(Collectors.toList());
    }

}
