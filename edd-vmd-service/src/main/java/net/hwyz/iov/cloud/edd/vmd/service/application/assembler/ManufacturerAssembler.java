package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ManufacturerDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产厂商 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ManufacturerAssembler {

    ManufacturerAssembler INSTANCE = Mappers.getMapper(ManufacturerAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param manufacturer 领域对象
     * @return DTO
     */
    ManufacturerDto fromDomain(Manufacturer manufacturer);

    /**
     * DTO 转领域对象
     *
     * @param manufacturerDto DTO
     * @return 领域对象
     */
    Manufacturer toDomain(ManufacturerDto manufacturerDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param manufacturerList 领域对象列表
     * @return DTO 列表
     */
    List<ManufacturerDto> fromDomainList(List<Manufacturer> manufacturerList);

}
