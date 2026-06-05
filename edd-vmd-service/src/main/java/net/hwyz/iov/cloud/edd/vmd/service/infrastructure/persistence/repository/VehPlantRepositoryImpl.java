package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PlantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehPlantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPlantPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 生产工厂数据仓库接口实现类（原VehManufacturerRepositoryImpl）
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehPlantRepositoryImpl implements VehPlantRepository {

    private final VehPlantMapper vehPlantMapper;

    @Override
    public List<Plant> selectByMap(Map<String, Object> map) {
        List<VehPlantPo> poList = vehPlantMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PlantConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehPlantMapper.countPoByMap(map);
    }

    @Override
    public Plant selectById(Long id) {
        return PlantConverter.INSTANCE.toDomain(vehPlantMapper.selectPoById(id));
    }

    @Override
    public Plant selectByCode(String code) {
        return PlantConverter.INSTANCE.toDomain(vehPlantMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Plant plant) {
        return vehPlantMapper.insertPo(PlantConverter.INSTANCE.fromDomain(plant));
    }

    @Override
    public int update(Plant plant) {
        return vehPlantMapper.updatePo(PlantConverter.INSTANCE.fromDomain(plant));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehPlantMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public Plant selectByExternalRefId(String externalRefId) {
        return PlantConverter.INSTANCE.toDomain(vehPlantMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public int countBySource(String source) {
        return vehPlantMapper.countPoBySource(source);
    }

}