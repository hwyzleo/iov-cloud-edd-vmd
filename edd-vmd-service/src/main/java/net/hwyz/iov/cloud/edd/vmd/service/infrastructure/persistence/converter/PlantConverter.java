package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPlantPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产工厂领域对象转换器（原ManufacturerConverter）
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlantConverter {

    PlantConverter INSTANCE = Mappers.getMapper(PlantConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehPlantPo PO
     * @return 领域对象
     */
    Plant toDomain(VehPlantPo vehPlantPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehPlantPoList PO 列表
     * @return 领域对象列表
     */
    List<Plant> toDomainList(List<VehPlantPo> vehPlantPoList);

    /**
     * 领域对象转 PO
     *
     * @param plant 领域对象
     * @return PO
     */
    VehPlantPo fromDomain(Plant plant);
}