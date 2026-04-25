package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 零件领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartConverter {

    PartConverter INSTANCE = Mappers.getMapper(PartConverter.class);

    /**
     * PO 转领域对象
     *
     * @param partPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    Part toDomain(PartPo partPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param partPoList PO 列表
     * @return 领域对象列表
     */
    List<Part> toDomainList(List<PartPo> partPoList);

    /**
     * 领域对象转 PO
     *
     * @param part 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    PartPo fromDomain(Part part);
}
