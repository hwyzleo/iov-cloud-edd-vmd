package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPlatformPo;
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
public interface PlatformAssembler {

    PlatformAssembler INSTANCE = Mappers.getMapper(PlatformAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehPlatformPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    PlatformVo fromPo(VehPlatformPo vehPlatformPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param platformVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VehPlatformPo toPo(PlatformVo platformVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehPlatformPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<PlatformVo> fromPoList(List<VehPlatformPo> vehPlatformPoList);

}
