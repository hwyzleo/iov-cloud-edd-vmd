package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.PartExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务零件信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartExServiceAssembler {

    PartExServiceAssembler INSTANCE = Mappers.getMapper(PartExServiceAssembler.class);

    /**
     * 领域对象转对外服务对象
     *
     * @param part 领域对象
     * @return 对外服务对象
     */
    PartExResponse fromDomain(Part part);

    /**
     * 对外服务对象转领域对象
     *
     * @param partExService 对外服务对象
     * @return 领域对象
     */
    Part toDomain(PartExResponse partExService);

    /**
     * 领域对象列表转对外服务对象列表
     *
     * @param partList 领域对象列表
     * @return 对外服务对象列表
     */
    List<PartExResponse> fromDomainList(List<Part> partList);

}
