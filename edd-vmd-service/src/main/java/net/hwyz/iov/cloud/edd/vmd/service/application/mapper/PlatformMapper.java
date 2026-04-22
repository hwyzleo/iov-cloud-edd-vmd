package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehPlatformDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆平台转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlatformMapper {

    PlatformMapper INSTANCE = Mappers.getMapper(PlatformMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehPlatformDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    PlatformVo fromDo(VmdVehPlatformDo vehPlatformDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param platformVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VmdVehPlatformDo toDo(PlatformVo platformVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehPlatformDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<PlatformVo> fromDoList(List<VmdVehPlatformDo> vehPlatformDoList);

}
