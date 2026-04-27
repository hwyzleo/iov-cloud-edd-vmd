package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BaseModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BaseModelFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基础车型 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptBaseModelAssembler {

    MptBaseModelAssembler INSTANCE = Mappers.getMapper(MptBaseModelAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param baseModelDto DTO
     * @return VO
     */
    BaseModelVo fromDto(BaseModelDto baseModelDto);

    /**
     * VO 转 DTO
     *
     * @param baseModelVo VO
     * @return DTO
     */
    BaseModelDto toDto(BaseModelVo baseModelVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param baseModelDtoList DTO 列表
     * @return VO 列表
     */
    List<BaseModelVo> fromDtoList(List<BaseModelDto> baseModelDtoList);

    /**
     * 特征值 DTO 转 VO
     *
     * @param baseModelFeatureCodeDto 特征值 DTO
     * @return 特征值 VO
     */
    BaseModelFeatureCodeVo fromFeatureCodeDto(BaseModelFeatureCodeDto baseModelFeatureCodeDto);

    /**
     * 特征值 VO 转 DTO
     *
     * @param baseModelFeatureCodeVo 特征值 VO
     * @return 特征值 DTO
     */
    BaseModelFeatureCodeDto toFeatureCodeDto(BaseModelFeatureCodeVo baseModelFeatureCodeVo);

    /**
     * 特征值 DTO 列表转 VO 列表
     *
     * @param baseModelFeatureCodeDtoList 特征值 DTO 列表
     * @return 特征值 VO 列表
     */
    List<BaseModelFeatureCodeVo> fromFeatureCodeDtoList(List<BaseModelFeatureCodeDto> baseModelFeatureCodeDtoList);

}
