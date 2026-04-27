package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBaseModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BaseModelConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BaseModelFeatureCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBaseModelFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBaseModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelFeatureCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基础车型数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehBaseModelRepositoryImpl implements VehBaseModelRepository {

    private final VehBaseModelMapper vehBaseModelMapper;
    private final VehBaseModelFeatureCodeMapper vehBaseModelFeatureCodeMapper;

    @Override
    public List<BaseModel> selectByMap(Map<String, Object> map) {
        List<VehBaseModelPo> poList = vehBaseModelMapper.selectPoByMap(map);
        return PageUtil.convert(poList, BaseModelConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehBaseModelMapper.countPoByMap(map);
    }

    @Override
    public BaseModel selectById(Long id) {
        return BaseModelConverter.INSTANCE.toDomain(vehBaseModelMapper.selectPoById(id));
    }

    @Override
    public BaseModel selectByCode(String code) {
        return BaseModelConverter.INSTANCE.toDomain(vehBaseModelMapper.selectPoByCode(code));
    }

    @Override
    public int insert(BaseModel baseModel) {
        return vehBaseModelMapper.insertPo(BaseModelConverter.INSTANCE.fromDomain(baseModel));
    }

    @Override
    public int update(BaseModel baseModel) {
        return vehBaseModelMapper.updatePo(BaseModelConverter.INSTANCE.fromDomain(baseModel));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehBaseModelMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<BaseModelFeatureCode> selectFeatureCodeByExample(BaseModelFeatureCode example) {
        List<VehBaseModelFeatureCodePo> poList = vehBaseModelFeatureCodeMapper.selectPoByExample(BaseModelFeatureCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, BaseModelFeatureCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertFeatureCode(List<BaseModelFeatureCode> featureCodeList) {
        List<VehBaseModelFeatureCodePo> poList = featureCodeList.stream()
                .map(BaseModelFeatureCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return vehBaseModelFeatureCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateFeatureCode(BaseModelFeatureCode featureCode) {
        return vehBaseModelFeatureCodeMapper.updatePo(BaseModelFeatureCodeConverter.INSTANCE.fromDomain(featureCode));
    }

    @Override
    public int batchPhysicalDeleteFeatureCode(Long[] ids) {
        return vehBaseModelFeatureCodeMapper.batchPhysicalDeletePo(ids);
    }

}
