package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigPo;
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
public interface BuildConfigAssembler {

    BuildConfigAssembler INSTANCE = Mappers.getMapper(BuildConfigAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBuildConfigPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BuildConfigVo fromPo(VehBuildConfigPo vehBuildConfigPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param buildConfigVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehBuildConfigPo toPo(BuildConfigVo buildConfigVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBuildConfigPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BuildConfigVo> fromPoList(List<VehBuildConfigPo> vehBuildConfigPoList);

}
