package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBuildConfigDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产配置转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BuildConfigMapper {

    BuildConfigMapper INSTANCE = Mappers.getMapper(BuildConfigMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBuildConfigDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BuildConfigVo fromDo(VmdVehBuildConfigDo vehBuildConfigDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param buildConfigVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBuildConfigDo toDo(BuildConfigVo buildConfigVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBuildConfigDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BuildConfigVo> fromDoList(List<VmdVehBuildConfigDo> vehBuildConfigDoList);

}
