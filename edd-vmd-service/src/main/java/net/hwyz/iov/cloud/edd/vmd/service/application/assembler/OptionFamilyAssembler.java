package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionFamilyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 选装族 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface OptionFamilyAssembler {

    OptionFamilyAssembler INSTANCE = Mappers.getMapper(OptionFamilyAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param optionFamily 领域对象
     * @return DTO
     */
    OptionFamilyDto fromDomain(OptionFamily optionFamily);

    /**
     * DTO 转领域对象
     *
     * @param optionFamilyDto DTO
     * @return 领域对象
     */
    OptionFamily toDomain(OptionFamilyDto optionFamilyDto);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    OptionFamily toDomain(OptionFamilyCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param optionFamilyList 领域对象列表
     * @return DTO 列表
     */
    List<OptionFamilyDto> fromDomainList(List<OptionFamily> optionFamilyList);

}
