package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ModelConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmModelPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
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
public class MdmModelRepositoryImpl implements MdmModelRepository {

    private final MdmModelMapper mdmModelMapper;

    @Override
    public List<Model> selectByMap(Map<String, Object> map) {
        List<MdmModelPo> poList = mdmModelMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ModelConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmModelMapper.countPoByMap(map);
    }

    @Override
    public Model selectById(Long id) {
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectPoById(id));
    }

    @Override
    public Model selectByCode(String code) {
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectPoByCode(code));
    }

    @Override
    public Model selectByExternalRefId(String externalRefId) {
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmModelMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Model model) {
        MdmModelPo po = ModelConverter.INSTANCE.fromDomain(model);
        int result = mdmModelMapper.insertPo(po);
        model.setId(po.getId());
        return result;
    }

    @Override
    public int update(Model model) {
        return mdmModelMapper.updatePo(ModelConverter.INSTANCE.fromDomain(model));
    }

    @Override
    public int updateById(Model model) {
        return mdmModelMapper.updatePo(ModelConverter.INSTANCE.fromDomain(model));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmModelMapper.batchPhysicalDeletePo(ids);
    }

}
