package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 选装值 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface OptionCodeAssembler {

    OptionCodeAssembler INSTANCE = Mappers.getMapper(OptionCodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param optionCode 领域对象
     * @return DTO
     */
    OptionCodeDto fromDomain(OptionCode optionCode);

    /**
     * DTO 转领域对象
     *
     * @param optionCodeDto DTO
     * @return 领域对象
     */
    OptionCode toDomain(OptionCodeDto optionCodeDto);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    OptionCode toDomain(OptionCodeCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param optionCodeList 领域对象列表
     * @return DTO 列表
     */
    List<OptionCodeDto> fromDomainList(List<OptionCode> optionCodeList);

}
