package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台特征值转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureCodeAssembler {

    FeatureCodeAssembler INSTANCE = Mappers.getMapper(FeatureCodeAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param featureCode 领域对象
     * @return 数据传输对象
     */
    FeatureCodeVo fromDomain(FeatureCode featureCode);

    /**
     * 数据传输对象转领域对象
     *
     * @param featureCodeVo 数据传输对象
     * @return 领域对象
     */
    FeatureCode toDomain(FeatureCodeVo featureCodeVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param featureCodeList 领域对象列表
     * @return 数据传输对象列表
     */
    List<FeatureCodeVo> fromDomainList(List<FeatureCode> featureCodeList);

}
