package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.FeatureFamilyCmd;

import java.util.List;

/**
 * 特征族 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureFamilyAssembler {

    FeatureFamilyAssembler INSTANCE = Mappers.getMapper(FeatureFamilyAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param featureFamily 领域对象
     * @return DTO
     */
    FeatureFamilyDto fromDomain(FeatureFamily featureFamily);

    /**
     * DTO 转领域对象
     *
     * @param featureFamilyDto DTO
     * @return 领域对象
     */
    FeatureFamily toDomain(FeatureFamilyDto featureFamilyDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    FeatureFamily toDomain(FeatureFamilyCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param featureFamilyList 领域对象列表
     * @return DTO 列表
     */
    List<FeatureFamilyDto> fromDomainList(List<FeatureFamily> featureFamilyList);

}
