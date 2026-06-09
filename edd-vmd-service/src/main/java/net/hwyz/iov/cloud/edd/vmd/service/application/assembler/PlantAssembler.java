package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PlantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlantDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产工厂 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlantAssembler {

    PlantAssembler INSTANCE = Mappers.getMapper(PlantAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param plant 领域对象
     * @return DTO
     */
    PlantDto fromDomain(Plant plant);

    /**
     * DTO 转领域对象
     *
     * @param plantDto DTO
     * @return 领域对象
     */
    Plant toDomain(PlantDto plantDto);

    /**
     * CMD 转领域对象
     *
     * @param plantCmd CMD
     * @return 领域对象
     */
    Plant toDomain(PlantCmd plantCmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param plantList 领域对象列表
     * @return DTO 列表
     */
    List<PlantDto> fromDomainList(List<Plant> plantList);

}
