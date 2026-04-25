package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台特征族转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureFamilyAssembler {

    FeatureFamilyAssembler INSTANCE = Mappers.getMapper(FeatureFamilyAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param featureFamily 领域对象
     * @return 数据传输对象
     */
    FeatureFamilyVo fromDomain(FeatureFamily featureFamily);

    /**
     * 数据传输对象转领域对象
     *
     * @param featureFamilyVo 数据传输对象
     * @return 领域对象
     */
    FeatureFamily toDomain(FeatureFamilyVo featureFamilyVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param featureFamilyList 领域对象列表
     * @return 数据传输对象列表
     */
    List<FeatureFamilyVo> fromDomainList(List<FeatureFamily> featureFamilyList);

}
