package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VariantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VariantOptionCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVariantOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVariantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantOptionCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantPo;
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
public class MdmVariantRepositoryImpl implements MdmVariantRepository {

    private final MdmVariantMapper mdmVariantMapper;
    private final MdmVariantOptionCodeMapper mdmVariantOptionCodeMapper;

    @Override
    public List<Variant> selectByMap(Map<String, Object> map) {
        List<MdmVariantPo> poList = mdmVariantMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VariantConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmVariantMapper.countPoByMap(map);
    }

    @Override
    public Variant selectById(Long id) {
        return VariantConverter.INSTANCE.toDomain(mdmVariantMapper.selectPoById(id));
    }

    @Override
    public Variant selectByCode(String code) {
        return VariantConverter.INSTANCE.toDomain(mdmVariantMapper.selectPoByCode(code));
    }

    @Override
    public Variant selectByExternalRefId(String externalRefId) {
        return VariantConverter.INSTANCE.toDomain(mdmVariantMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmVariantMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Variant variant) {
        return mdmVariantMapper.insertPo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int update(Variant variant) {
        return mdmVariantMapper.updatePo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int updateById(Variant variant) {
        return mdmVariantMapper.updatePo(VariantConverter.INSTANCE.fromDomain(variant));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmVariantMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<VariantOptionCode> selectOptionCodeByExample(VariantOptionCode example) {
        List<MdmVariantOptionCodePo> poList = mdmVariantOptionCodeMapper.selectPoByExample(VariantOptionCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, VariantOptionCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertOptionCode(List<VariantOptionCode> optionCodeList) {
        List<MdmVariantOptionCodePo> poList = optionCodeList.stream()
                .map(VariantOptionCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return mdmVariantOptionCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateOptionCode(VariantOptionCode optionCode) {
        return mdmVariantOptionCodeMapper.updatePo(VariantOptionCodeConverter.INSTANCE.fromDomain(optionCode));
    }

    @Override
    public int batchPhysicalDeleteOptionCode(Long[] ids) {
        return mdmVariantOptionCodeMapper.batchPhysicalDeletePo(ids);
    }

}
