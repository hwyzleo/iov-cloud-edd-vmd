package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.OptionConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmOptionFamilyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmOptionCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmOptionFamilyPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选装数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MdmOptionFamilyRepositoryImpl implements MdmOptionFamilyRepository {

    private final MdmOptionFamilyMapper mdmOptionFamilyMapper;
    private final MdmOptionCodeMapper mdmOptionCodeMapper;

    // ==================== 选装族 ====================

    @Override
    public List<OptionFamily> selectByMap(Map<String, Object> map) {
        List<MdmOptionFamilyPo> poList = mdmOptionFamilyMapper.selectPoByMap(map);
        return PageUtil.convert(poList, OptionConverter.INSTANCE::toFamilyDomain);
    }

    @Override
    public OptionFamily selectById(Long id) {
        return OptionConverter.INSTANCE.toFamilyDomain(mdmOptionFamilyMapper.selectPoById(id));
    }

    @Override
    public OptionFamily selectByCode(String code) {
        return OptionConverter.INSTANCE.toFamilyDomain(mdmOptionFamilyMapper.selectPoByCode(code));
    }

    @Override
    public OptionFamily selectByExternalRefId(String externalRefId) {
        return OptionConverter.INSTANCE.toFamilyDomain(mdmOptionFamilyMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(String source) {
        return mdmOptionFamilyMapper.countPoBySource(source);
    }

    @Override
    public int insert(OptionFamily optionFamily) {
        return mdmOptionFamilyMapper.insertPo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int update(OptionFamily optionFamily) {
        return mdmOptionFamilyMapper.updatePo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int updateById(OptionFamily optionFamily) {
        return mdmOptionFamilyMapper.updatePo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmOptionFamilyMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== 选装值 ====================

    @Override
    public List<OptionCode> selectOptionCodeByOptionFamilyCode(String optionFamilyCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("optionFamilyCode", optionFamilyCode);
        List<MdmOptionCodePo> poList = mdmOptionCodeMapper.selectPoByMap(map);
        return PageUtil.convert(poList, OptionConverter.INSTANCE::toCodeDomain);
    }

    @Override
    public OptionCode selectOptionCodeById(Long id) {
        return OptionConverter.INSTANCE.toCodeDomain(mdmOptionCodeMapper.selectPoById(id));
    }

    @Override
    public OptionCode selectOptionCodeByCode(String code) {
        return OptionConverter.INSTANCE.toCodeDomain(mdmOptionCodeMapper.selectPoByCode(code));
    }

    @Override
    public OptionCode selectOptionCodeByExternalRefId(String externalRefId) {
        return OptionConverter.INSTANCE.toCodeDomain(mdmOptionCodeMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countOptionCodeBySource(String source) {
        return mdmOptionCodeMapper.countPoBySource(source);
    }

    @Override
    public int insertOptionCode(OptionCode optionCode) {
        return mdmOptionCodeMapper.insertPo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int updateOptionCode(OptionCode optionCode) {
        return mdmOptionCodeMapper.updatePo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int updateOptionCodeById(OptionCode optionCode) {
        return mdmOptionCodeMapper.updatePo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int batchPhysicalDeleteOptionCode(Long[] ids) {
        return mdmOptionCodeMapper.batchPhysicalDeletePo(ids);
    }

}
