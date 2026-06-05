package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ManufacturerCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ManufacturerDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产工厂 DTO 转换器（原ManufacturerAssembler）
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
    ManufacturerDto fromDomain(Plant plant);

    /**
     * DTO 转领域对象
     *
     * @param manufacturerDto DTO
     * @return 领域对象
     */
    Plant toDomain(ManufacturerDto manufacturerDto);

    /**
     * CMD 转领域对象
     *
     * @param manufacturerCmd CMD
     * @return 领域对象
     */
    Plant toDomain(ManufacturerCmd manufacturerCmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param plantList 领域对象列表
     * @return DTO 列表
     */
    List<ManufacturerDto> fromDomainList(List<Plant> plantList);

}