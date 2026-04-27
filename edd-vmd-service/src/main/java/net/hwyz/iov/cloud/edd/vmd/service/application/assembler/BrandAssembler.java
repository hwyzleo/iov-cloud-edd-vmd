package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BrandCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BrandDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 品牌 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BrandAssembler {

    BrandAssembler INSTANCE = Mappers.getMapper(BrandAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param brand 领域对象
     * @return DTO
     */
    BrandDto fromDomain(Brand brand);

    /**
     * DTO 转领域对象
     *
     * @param brandDto DTO
     * @return 领域对象
     */
    Brand toDomain(BrandDto brandDto);

    /**
     * 命令转领域对象
     *
     * @param brandCmd 命令
     * @return 领域对象
     */
    Brand toDomain(BrandCmd brandCmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param brandList 领域对象列表
     * @return DTO 列表
     */
    List<BrandDto> fromDomainList(List<Brand> brandList);

}
