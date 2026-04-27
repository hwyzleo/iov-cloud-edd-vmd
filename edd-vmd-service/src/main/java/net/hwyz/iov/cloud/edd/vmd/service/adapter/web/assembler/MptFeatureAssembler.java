package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureFamilyDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台特征 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptFeatureAssembler {

    MptFeatureAssembler INSTANCE = Mappers.getMapper(MptFeatureAssembler.class);

    /**
     * 特征族 DTO 转 VO
     *
     * @param featureFamilyDto 特征族 DTO
     * @return 特征族 VO
     */
    FeatureFamilyVo fromFamilyDto(FeatureFamilyDto featureFamilyDto);

    /**
     * 特征族 VO 转 DTO
     *
     * @param featureFamilyVo 特征族 VO
     * @return 特征族 DTO
     */
    FeatureFamilyDto toFamilyDto(FeatureFamilyVo featureFamilyVo);

    /**
     * 特征族 DTO 列表转 VO 列表
     *
     * @param featureFamilyDtoList 特征族 DTO 列表
     * @return 特征族 VO 列表
     */
    List<FeatureFamilyVo> fromFamilyDtoList(List<FeatureFamilyDto> featureFamilyDtoList);

    /**
     * 特征值 DTO 转 VO
     *
     * @param featureCodeDto 特征值 DTO
     * @return 特征值 VO
     */
    FeatureCodeVo fromCodeDto(FeatureCodeDto featureCodeDto);

    /**
     * 特征值 VO 转 DTO
     *
     * @param featureCodeVo 特征值 VO
     * @return 特征值 DTO
     */
    FeatureCodeDto toCodeDto(FeatureCodeVo featureCodeVo);

    /**
     * 特征值 DTO 列表转 VO 列表
     *
     * @param featureCodeDtoList 特征值 DTO 列表
     * @return 特征值 VO 列表
     */
    List<FeatureCodeVo> fromCodeDtoList(List<FeatureCodeDto> featureCodeDtoList);

}
