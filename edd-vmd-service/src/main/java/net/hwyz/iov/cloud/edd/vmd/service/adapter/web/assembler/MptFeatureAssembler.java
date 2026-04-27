package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.FeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.FeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.FeatureFamilyRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.FeatureFamilyResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureFamilyDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.FeatureFamilyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.FeatureCodeCmd;

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
    FeatureFamilyResponse fromFamilyDto(FeatureFamilyDto featureFamilyDto);

    /**
     * 特征族 VO 转 DTO
     *
     * @param featureFamilyVo 特征族 VO
     * @return 特征族 DTO
     */
    FeatureFamilyDto toFamilyDto(FeatureFamilyRequest featureFamilyVo);
    FeatureFamilyCmd toFamilyCmd(FeatureFamilyRequest vo);


    /**
     * 特征族 DTO 列表转 VO 列表
     *
     * @param featureFamilyDtoList 特征族 DTO 列表
     * @return 特征族 VO 列表
     */
    List<FeatureFamilyResponse> fromFamilyDtoList(List<FeatureFamilyDto> featureFamilyDtoList);

    /**
     * 特征值 DTO 转 VO
     *
     * @param featureCodeDto 特征值 DTO
     * @return 特征值 VO
     */
    FeatureCodeResponse fromCodeDto(FeatureCodeDto featureCodeDto);

    /**
     * 特征值 VO 转 DTO
     *
     * @param featureCodeVo 特征值 VO
     * @return 特征值 DTO
     */
    FeatureCodeDto toCodeDto(FeatureCodeRequest featureCodeVo);
    FeatureCodeCmd toCodeCmd(FeatureCodeRequest vo);


    /**
     * 特征值 DTO 列表转 VO 列表
     *
     * @param featureCodeDtoList 特征值 DTO 列表
     * @return 特征值 VO 列表
     */
    List<FeatureCodeResponse> fromCodeDtoList(List<FeatureCodeDto> featureCodeDtoList);

}
