package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmModelPo;
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
     * @param mdmModelPo PO
     * @return 领域对象
     */
    Model toDomain(MdmModelPo mdmModelPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param mdmModelPoList PO 列表
     * @return 领域对象列表
     */
    List<Model> toDomainList(List<MdmModelPo> mdmModelPoList);

    /**
     * 领域对象转 PO
     *
     * @param model 领域对象
     * @return PO
     */
    MdmModelPo fromDomain(Model model);
}
