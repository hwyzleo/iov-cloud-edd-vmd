package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehModelPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车型转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ModelAssembler {

    ModelAssembler INSTANCE = Mappers.getMapper(ModelAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehModelPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    ModelVo fromPo(VehModelPo vehModelPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param modelVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VehModelPo toPo(ModelVo modelVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehModelPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ModelVo> fromPoList(List<VehModelPo> vehModelPoList);

}
