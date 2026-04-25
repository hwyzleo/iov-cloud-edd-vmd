package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产厂商转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ManufacturerAssembler {

    ManufacturerAssembler INSTANCE = Mappers.getMapper(ManufacturerAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param manufacturer 领域对象
     * @return 数据传输对象
     */
    ManufacturerVo fromDomain(Manufacturer manufacturer);

    /**
     * 数据传输对象转领域对象
     *
     * @param manufacturerVo 数据传输对象
     * @return 领域对象
     */
    Manufacturer toDomain(ManufacturerVo manufacturerVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param manufacturerList 领域对象列表
     * @return 数据传输对象列表
     */
    List<ManufacturerVo> fromDomainList(List<Manufacturer> manufacturerList);

}
