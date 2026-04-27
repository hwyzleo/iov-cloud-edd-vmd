package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BaseModelFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BaseModelRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BaseModelFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BaseModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BaseModelCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BaseModelFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelFeatureCodeDto;
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
     * DTO 转 Response
     *
     * @param baseModelDto DTO
     * @return Response
     */
    BaseModelResponse fromDto(BaseModelDto baseModelDto);

    /**
     * Request 转命令
     *
     * @param baseModelRequest Request
     * @return 命令
     */
    BaseModelCmd toCmd(BaseModelRequest baseModelRequest);

    /**
     * DTO 列表转 Response 列表
     *
     * @param baseModelDtoList DTO 列表
     * @return Response 列表
     */
    List<BaseModelResponse> fromDtoList(List<BaseModelDto> baseModelDtoList);

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
