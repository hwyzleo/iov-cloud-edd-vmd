package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BaseModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 基础车型 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelAssembler {

    BaseModelAssembler INSTANCE = Mappers.getMapper(BaseModelAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param baseModel 领域对象
     * @return DTO
     */
    BaseModelDto fromDomain(BaseModel baseModel);

    /**
     * DTO 转领域对象
     *
     * @param baseModelDto DTO
     * @return 领域对象
     */
    BaseModel toDomain(BaseModelDto baseModelDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param baseModelList 领域对象列表
     * @return DTO 列表
     */
    List<BaseModelDto> fromDomainList(List<BaseModel> baseModelList);

}
