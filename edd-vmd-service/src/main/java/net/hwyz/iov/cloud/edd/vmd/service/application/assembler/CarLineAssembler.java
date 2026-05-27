package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CarLineDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CarLineCmd;

import java.util.List;

/**
 * 车系 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface CarLineAssembler {

    CarLineAssembler INSTANCE = Mappers.getMapper(CarLineAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param carLine 领域对象
     * @return DTO
     */
    CarLineDto fromDomain(CarLine carLine);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    CarLine toDomain(CarLineCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param carLines 领域对象列表
     * @return DTO 列表
     */
    List<CarLineDto> fromDomainList(List<CarLine> carLines);

}
