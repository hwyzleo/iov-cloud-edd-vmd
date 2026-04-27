package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车型 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ModelAssembler {

    ModelAssembler INSTANCE = Mappers.getMapper(ModelAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param model 领域对象
     * @return DTO
     */
    ModelDto fromDomain(Model model);

    /**
     * DTO 转领域对象
     *
     * @param modelDto DTO
     * @return 领域对象
     */
    Model toDomain(ModelDto modelDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param modelList 领域对象列表
     * @return DTO 列表
     */
    List<ModelDto> fromDomainList(List<Model> modelList);

}
