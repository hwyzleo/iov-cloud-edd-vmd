package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPartPo;
import org.mapstruct.Mapper;
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
     * @param mdmPartPo PO
     * @return 领域对象
     */
    Part toDomain(MdmPartPo mdmPartPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param mdmPartPoList PO 列表
     * @return 领域对象列表
     */
    List<Part> toDomainList(List<MdmPartPo> mdmPartPoList);

    /**
     * 领域对象转 PO
     *
     * @param part 领域对象
     * @return PO
     */
    MdmPartPo fromDomain(Part part);
}
