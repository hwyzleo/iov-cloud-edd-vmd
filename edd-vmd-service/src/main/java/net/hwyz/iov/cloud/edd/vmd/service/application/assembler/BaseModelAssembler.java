package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基础车型转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelAssembler {

    BaseModelAssembler INSTANCE = Mappers.getMapper(BaseModelAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBaseModelPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BaseModelVo fromPo(VehBaseModelPo vehBaseModelPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param baseModelVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehBaseModelPo toPo(BaseModelVo baseModelVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBaseModelPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelVo> fromPoList(List<VehBaseModelPo> vehBaseModelPoList);

}
