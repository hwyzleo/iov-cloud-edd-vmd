package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import org.mapstruct.Mapper;
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
     * 领域对象转数据传输对象
     *
     * @param model 领域对象
     * @return 数据传输对象
     */
    ModelVo fromDomain(Model model);

    /**
     * 数据传输对象转领域对象
     *
     * @param modelVo 数据传输对象
     * @return 领域对象
     */
    Model toDomain(ModelVo modelVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param modelList 领域对象列表
     * @return 数据传输对象列表
     */
    List<ModelVo> fromDomainList(List<Model> modelList);

}
