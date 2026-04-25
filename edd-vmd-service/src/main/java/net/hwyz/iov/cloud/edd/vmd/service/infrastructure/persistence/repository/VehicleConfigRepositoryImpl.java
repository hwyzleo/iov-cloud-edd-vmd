package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleConfigRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleConfigConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleConfigItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆配置数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleConfigRepositoryImpl implements VehicleConfigRepository {

    private final VehicleConfigMapper vehicleConfigMapper;
    private final VehicleConfigItemMapper vehicleConfigItemMapper;

    @Override
    public List<VehicleConfig> selectByMap(Map<String, Object> map) {
        return VehicleConfigConverter.INSTANCE.toDomainList(vehicleConfigMapper.selectPoByMap(map));
    }

    @Override
    public VehicleConfig selectById(Long id) {
        return VehicleConfigConverter.INSTANCE.toDomain(vehicleConfigMapper.selectPoById(id));
    }

    @Override
    public int insert(VehicleConfig vehicleConfig) {
        return vehicleConfigMapper.insertPo(VehicleConfigConverter.INSTANCE.fromDomain(vehicleConfig));
    }

    @Override
    public int update(VehicleConfig vehicleConfig) {
        return vehicleConfigMapper.updatePo(VehicleConfigConverter.INSTANCE.fromDomain(vehicleConfig));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehicleConfigMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<VehicleConfigItem> selectConfigItemByMap(Map<String, Object> map) {
        return VehicleConfigConverter.INSTANCE.toItemDomainList(vehicleConfigItemMapper.selectPoByMap(map));
    }

    @Override
    public VehicleConfigItem selectConfigItemById(Long id) {
        return VehicleConfigConverter.INSTANCE.toItemDomain(vehicleConfigItemMapper.selectPoById(id));
    }

    @Override
    public int insertConfigItem(VehicleConfigItem vehicleConfigItem) {
        return vehicleConfigItemMapper.insertPo(VehicleConfigConverter.INSTANCE.fromItemDomain(vehicleConfigItem));
    }

    @Override
    public int updateConfigItem(VehicleConfigItem vehicleConfigItem) {
        return vehicleConfigItemMapper.updatePo(VehicleConfigConverter.INSTANCE.fromItemDomain(vehicleConfigItem));
    }

    @Override
    public int batchPhysicalDeleteConfigItem(Long[] ids) {
        return vehicleConfigItemMapper.batchPhysicalDeletePo(ids);
    }

}
