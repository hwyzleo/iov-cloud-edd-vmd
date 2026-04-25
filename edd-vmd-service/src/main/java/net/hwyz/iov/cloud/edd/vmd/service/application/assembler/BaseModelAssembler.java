package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import org.mapstruct.Mapper;
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
     * 领域对象转数据传输对象
     *
     * @param baseModel 领域对象
     * @return 数据传输对象
     */
    BaseModelVo fromDomain(BaseModel baseModel);

    /**
     * 数据传输对象转领域对象
     *
     * @param baseModelVo 数据传输对象
     * @return 领域对象
     */
    BaseModel toDomain(BaseModelVo baseModelVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param baseModelList 领域对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelVo> fromDomainList(List<BaseModel> baseModelList);

}
