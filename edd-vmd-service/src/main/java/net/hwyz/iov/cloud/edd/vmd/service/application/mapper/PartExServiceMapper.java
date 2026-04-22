package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PartExService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdPartDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务零件信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartExServiceMapper {

    PartExServiceMapper INSTANCE = Mappers.getMapper(PartExServiceMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param partDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    PartExService fromDo(VmdPartDo partDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param partExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdPartDo toDo(PartExService partExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param partDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<PartExService> fromDoList(List<VmdPartDo> partDoList);

}
