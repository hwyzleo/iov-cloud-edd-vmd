package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehManufacturerPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产厂商领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ManufacturerConverter {

    ManufacturerConverter INSTANCE = Mappers.getMapper(ManufacturerConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehManufacturerPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    Manufacturer toDomain(VehManufacturerPo vehManufacturerPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehManufacturerPoList PO 列表
     * @return 领域对象列表
     */
    List<Manufacturer> toDomainList(List<VehManufacturerPo> vehManufacturerPoList);

    /**
     * 领域对象转 PO
     *
     * @param manufacturer 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehManufacturerPo fromDomain(Manufacturer manufacturer);
}
