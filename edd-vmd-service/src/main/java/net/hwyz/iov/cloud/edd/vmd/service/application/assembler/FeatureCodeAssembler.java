package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 特征值 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureCodeAssembler {

    FeatureCodeAssembler INSTANCE = Mappers.getMapper(FeatureCodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param featureCode 领域对象
     * @return DTO
     */
    FeatureCodeDto fromDomain(FeatureCode featureCode);

    /**
     * DTO 转领域对象
     *
     * @param featureCodeDto DTO
     * @return 领域对象
     */
    FeatureCode toDomain(FeatureCodeDto featureCodeDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param featureCodeList 领域对象列表
     * @return DTO 列表
     */
    List<FeatureCodeDto> fromDomainList(List<FeatureCode> featureCodeList);

}
