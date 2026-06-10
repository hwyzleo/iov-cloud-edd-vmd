package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PlantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmPlantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPlantPo;
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
public class MdmPlantRepositoryImpl implements MdmPlantRepository {

    private final MdmPlantMapper mdmPlantMapper;

    @Override
    public List<Plant> selectByMap(Map<String, Object> map) {
        List<MdmPlantPo> poList = mdmPlantMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PlantConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmPlantMapper.countPoByMap(map);
    }

    @Override
    public Plant selectById(Long id) {
        return PlantConverter.INSTANCE.toDomain(mdmPlantMapper.selectPoById(id));
    }

    @Override
    public Plant selectByCode(String code) {
        return PlantConverter.INSTANCE.toDomain(mdmPlantMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Plant plant) {
        return mdmPlantMapper.insertPo(PlantConverter.INSTANCE.fromDomain(plant));
    }

    @Override
    public int update(Plant plant) {
        return mdmPlantMapper.updatePo(PlantConverter.INSTANCE.fromDomain(plant));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmPlantMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public Plant selectByExternalRefId(String externalRefId) {
        return PlantConverter.INSTANCE.toDomain(mdmPlantMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public int countBySource(String source) {
        return mdmPlantMapper.countPoBySource(source);
    }

}
