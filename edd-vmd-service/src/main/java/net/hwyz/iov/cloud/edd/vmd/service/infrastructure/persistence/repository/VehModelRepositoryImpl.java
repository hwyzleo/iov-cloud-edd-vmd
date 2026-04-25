package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ModelConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车型数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehModelRepositoryImpl implements VehModelRepository {

    private final VehModelMapper vehModelMapper;

    @Override
    public List<Model> selectByMap(Map<String, Object> map) {
        return ModelConverter.INSTANCE.toDomainList(vehModelMapper.selectPoByMap(map));
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehModelMapper.countPoByMap(map);
    }

    @Override
    public Model selectById(Long id) {
        return ModelConverter.INSTANCE.toDomain(vehModelMapper.selectPoById(id));
    }

    @Override
    public Model selectByCode(String code) {
        return ModelConverter.INSTANCE.toDomain(vehModelMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Model model) {
        return vehModelMapper.insertPo(ModelConverter.INSTANCE.fromDomain(model));
    }

    @Override
    public int update(Model model) {
        return vehModelMapper.updatePo(ModelConverter.INSTANCE.fromDomain(model));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehModelMapper.batchPhysicalDeletePo(ids);
    }

}
