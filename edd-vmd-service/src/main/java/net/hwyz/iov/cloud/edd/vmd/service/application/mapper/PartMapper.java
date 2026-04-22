package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdPartDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台零件信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartMapper {

    PartMapper INSTANCE = Mappers.getMapper(PartMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param partDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    PartVo fromDo(VmdPartDo partDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param partVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdPartDo toDo(PartVo partVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param partDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<PartVo> fromDoList(List<VmdPartDo> partDoList);

}
