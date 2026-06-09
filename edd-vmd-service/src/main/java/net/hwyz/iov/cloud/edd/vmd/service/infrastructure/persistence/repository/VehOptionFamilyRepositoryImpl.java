package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.OptionConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehOptionFamilyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehOptionCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehOptionFamilyPo;
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
public class VehOptionFamilyRepositoryImpl implements VehOptionFamilyRepository {

    private final VehOptionFamilyMapper vehOptionFamilyMapper;
    private final VehOptionCodeMapper vehOptionCodeMapper;

    // ==================== 选装族 ====================

    @Override
    public List<OptionFamily> selectByMap(Map<String, Object> map) {
        List<VehOptionFamilyPo> poList = vehOptionFamilyMapper.selectPoByMap(map);
        return PageUtil.convert(poList, OptionConverter.INSTANCE::toFamilyDomain);
    }

    @Override
    public OptionFamily selectById(Long id) {
        return OptionConverter.INSTANCE.toFamilyDomain(vehOptionFamilyMapper.selectPoById(id));
    }

    @Override
    public OptionFamily selectByCode(String code) {
        return OptionConverter.INSTANCE.toFamilyDomain(vehOptionFamilyMapper.selectPoByCode(code));
    }

    @Override
    public OptionFamily selectByExternalRefId(String externalRefId) {
        return OptionConverter.INSTANCE.toFamilyDomain(vehOptionFamilyMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(String source) {
        return vehOptionFamilyMapper.countPoBySource(source);
    }

    @Override
    public int insert(OptionFamily optionFamily) {
        return vehOptionFamilyMapper.insertPo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int update(OptionFamily optionFamily) {
        return vehOptionFamilyMapper.updatePo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int updateById(OptionFamily optionFamily) {
        return vehOptionFamilyMapper.updatePo(OptionConverter.INSTANCE.fromFamilyDomain(optionFamily));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehOptionFamilyMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== 选装值 ====================

    @Override
    public List<OptionCode> selectOptionCodeByOptionFamilyCode(String optionFamilyCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("optionFamilyCode", optionFamilyCode);
        List<VehOptionCodePo> poList = vehOptionCodeMapper.selectPoByMap(map);
        return PageUtil.convert(poList, OptionConverter.INSTANCE::toCodeDomain);
    }

    @Override
    public OptionCode selectOptionCodeById(Long id) {
        return OptionConverter.INSTANCE.toCodeDomain(vehOptionCodeMapper.selectPoById(id));
    }

    @Override
    public OptionCode selectOptionCodeByCode(String code) {
        return OptionConverter.INSTANCE.toCodeDomain(vehOptionCodeMapper.selectPoByCode(code));
    }

    @Override
    public OptionCode selectOptionCodeByExternalRefId(String externalRefId) {
        return OptionConverter.INSTANCE.toCodeDomain(vehOptionCodeMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countOptionCodeBySource(String source) {
        return vehOptionCodeMapper.countPoBySource(source);
    }

    @Override
    public int insertOptionCode(OptionCode optionCode) {
        return vehOptionCodeMapper.insertPo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int updateOptionCode(OptionCode optionCode) {
        return vehOptionCodeMapper.updatePo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int updateOptionCodeById(OptionCode optionCode) {
        return vehOptionCodeMapper.updatePo(OptionConverter.INSTANCE.fromCodeDomain(optionCode));
    }

    @Override
    public int batchPhysicalDeleteOptionCode(Long[] ids) {
        return vehOptionCodeMapper.batchPhysicalDeletePo(ids);
    }

}
