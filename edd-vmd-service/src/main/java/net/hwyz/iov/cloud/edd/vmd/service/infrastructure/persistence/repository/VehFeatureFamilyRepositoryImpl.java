package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehFeatureFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.FeatureConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehFeatureFamilyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureFamilyPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特征数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehFeatureFamilyRepositoryImpl implements VehFeatureFamilyRepository {

    private final VehFeatureFamilyMapper vehFeatureFamilyMapper;
    private final VehFeatureCodeMapper vehFeatureCodeMapper;

    // ==================== 特征族 ====================

    @Override
    public List<FeatureFamily> selectByMap(Map<String, Object> map) {
        List<VehFeatureFamilyPo> poList = vehFeatureFamilyMapper.selectPoByMap(map);
        return PageUtil.convert(poList, FeatureConverter.INSTANCE::toFamilyDomain);
    }

    @Override
    public FeatureFamily selectById(Long id) {
        return FeatureConverter.INSTANCE.toFamilyDomain(vehFeatureFamilyMapper.selectPoById(id));
    }

    @Override
    public FeatureFamily selectByCode(String code) {
        return FeatureConverter.INSTANCE.toFamilyDomain(vehFeatureFamilyMapper.selectPoByCode(code));
    }

    @Override
    public int insert(FeatureFamily featureFamily) {
        return vehFeatureFamilyMapper.insertPo(FeatureConverter.INSTANCE.fromFamilyDomain(featureFamily));
    }

    @Override
    public int update(FeatureFamily featureFamily) {
        return vehFeatureFamilyMapper.updatePo(FeatureConverter.INSTANCE.fromFamilyDomain(featureFamily));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehFeatureFamilyMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== 特征值 ====================

    @Override
    public List<FeatureCode> selectFeatureCodeByFamilyCode(String familyCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("familyCode", familyCode);
        List<VehFeatureCodePo> poList = vehFeatureCodeMapper.selectPoByMap(map);
        return PageUtil.convert(poList, FeatureConverter.INSTANCE::toCodeDomain);
    }

    @Override
    public FeatureCode selectFeatureCodeById(Long id) {
        return FeatureConverter.INSTANCE.toCodeDomain(vehFeatureCodeMapper.selectPoById(id));
    }

    @Override
    public FeatureCode selectFeatureCodeByCode(String code) {
        return FeatureConverter.INSTANCE.toCodeDomain(vehFeatureCodeMapper.selectPoByCode(code));
    }

    @Override
    public int insertFeatureCode(FeatureCode featureCode) {
        return vehFeatureCodeMapper.insertPo(FeatureConverter.INSTANCE.fromCodeDomain(featureCode));
    }

    @Override
    public int updateFeatureCode(FeatureCode featureCode) {
        return vehFeatureCodeMapper.updatePo(FeatureConverter.INSTANCE.fromCodeDomain(featureCode));
    }

    @Override
    public int batchPhysicalDeleteFeatureCode(Long[] ids) {
        return vehFeatureCodeMapper.batchPhysicalDeletePo(ids);
    }

}
