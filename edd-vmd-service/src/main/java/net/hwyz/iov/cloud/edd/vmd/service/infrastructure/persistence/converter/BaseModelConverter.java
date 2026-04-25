package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 基础车型领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelConverter {

    BaseModelConverter INSTANCE = Mappers.getMapper(BaseModelConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehBaseModelPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    BaseModel toDomain(VehBaseModelPo vehBaseModelPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehBaseModelPoList PO 列表
     * @return 领域对象列表
     */
    List<BaseModel> toDomainList(List<VehBaseModelPo> vehBaseModelPoList);

    /**
     * 领域对象转 PO
     *
     * @param baseModel 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehBaseModelPo fromDomain(BaseModel baseModel);
}
