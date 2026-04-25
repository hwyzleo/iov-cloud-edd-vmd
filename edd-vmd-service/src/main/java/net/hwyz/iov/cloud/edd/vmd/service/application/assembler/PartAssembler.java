package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台零件转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartAssembler {

    PartAssembler INSTANCE = Mappers.getMapper(PartAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param part 领域对象
     * @return 数据传输对象
     */
    PartVo fromDomain(Part part);

    /**
     * 数据传输对象转领域对象
     *
     * @param partVo 数据传输对象
     * @return 领域对象
     */
    Part toDomain(PartVo partVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param partList 领域对象列表
     * @return 数据传输对象列表
     */
    List<PartVo> fromDomainList(List<Part> partList);

}
