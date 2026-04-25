package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehModelPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车型领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ModelConverter {

    ModelConverter INSTANCE = Mappers.getMapper(ModelConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehModelPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    Model toDomain(VehModelPo vehModelPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehModelPoList PO 列表
     * @return 领域对象列表
     */
    List<Model> toDomainList(List<VehModelPo> vehModelPoList);

    /**
     * 领域对象转 PO
     *
     * @param model 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehModelPo fromDomain(Model model);
}
