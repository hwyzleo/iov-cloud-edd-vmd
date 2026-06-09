package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BaseModelFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VariantRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BaseModelFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VariantResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BaseModelFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台版本 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVariantAssembler {

    MptVariantAssembler INSTANCE = Mappers.getMapper(MptVariantAssembler.class);

    /**
     * DTO 转 Response
     *
     * @param variantDto DTO
     * @return Response
     */
    VariantResponse fromDto(VariantDto variantDto);

    /**
     * Request 转命令
     *
     * @param variantRequest Request
     * @return 命令
     */
    VariantCmd toCmd(VariantRequest variantRequest);

    /**
     * DTO 列表转 Response 列表
     *
     * @param variantDtoList DTO 列表
     * @return Response 列表
     */
    List<VariantResponse> fromDtoList(List<VariantDto> variantDtoList);

    /**
     * 特征值 DTO 转 Response
     *
     * @param baseModelFeatureCodeDto 特征值 DTO
     * @return 特征值 Response
     */
    BaseModelFeatureCodeResponse fromFeatureCodeDto(BaseModelFeatureCodeDto baseModelFeatureCodeDto);

    /**
     * 特征值 Request 转命令
     *
     * @param baseModelFeatureCodeRequest 特征值 Request
     * @return 特征值命令
     */
    BaseModelFeatureCodeCmd toFeatureCodeCmd(BaseModelFeatureCodeRequest baseModelFeatureCodeRequest);

    /**
     * 特征值 DTO 列表转 Response 列表
     *
     * @param baseModelFeatureCodeDtoList 特征值 DTO 列表
     * @return 特征值 Response 列表
     */
    List<BaseModelFeatureCodeResponse> fromFeatureCodeDtoList(List<BaseModelFeatureCodeDto> baseModelFeatureCodeDtoList);

}
