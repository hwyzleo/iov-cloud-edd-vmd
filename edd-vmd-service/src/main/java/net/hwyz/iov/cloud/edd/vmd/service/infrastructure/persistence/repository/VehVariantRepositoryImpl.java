package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehVariantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VariantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VariantFeatureCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehVariantFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehVariantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantFeatureCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 版本数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehVariantRepositoryImpl implements VehVariantRepository {

    private final VehVariantMapper vehVariantMapper;
    private final VehVariantFeatureCodeMapper vehVariantFeatureCodeMapper;

    @Override
    public List<Variant> selectByMap(Map<String, Object> map) {
        List<VehVariantPo> poList = vehVariantMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VariantConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehVariantMapper.countPoByMap(map);
    }

    @Override
    public Variant selectById(Long id) {
        return VariantConverter.INSTANCE.toDomain(vehVariantMapper.selectPoById(id));
    }

    @Override
    public Variant selectByCode(String code) {
        return VariantConverter.INSTANCE.toDomain(vehVariantMapper.selectPoByCode(code));
    }

    @Override
    public Variant selectByExternalRefId(String externalRefId) {
        return VariantConverter.INSTANCE.toDomain(vehVariantMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return vehVariantMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Variant variant) {
        return vehVariantMapper.insertPo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int update(Variant variant) {
        return vehVariantMapper.updatePo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int updateById(Variant variant) {
        return vehVariantMapper.updatePo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehVariantMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<VariantFeatureCode> selectFeatureCodeByExample(VariantFeatureCode example) {
        List<VehVariantFeatureCodePo> poList = vehVariantFeatureCodeMapper.selectPoByExample(VariantFeatureCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, VariantFeatureCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertFeatureCode(List<VariantFeatureCode> featureCodeList) {
        List<VehVariantFeatureCodePo> poList = featureCodeList.stream()
                .map(VariantFeatureCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return vehVariantFeatureCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateFeatureCode(VariantFeatureCode featureCode) {
        return vehVariantFeatureCodeMapper.updatePo(VariantFeatureCodeConverter.INSTANCE.fromDomain(featureCode));
    }

    @Override
    public int batchPhysicalDeleteFeatureCode(Long[] ids) {
        return vehVariantFeatureCodeMapper.batchPhysicalDeletePo(ids);
    }

}
