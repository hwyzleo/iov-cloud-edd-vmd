package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBrandPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 品牌领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BrandConverter {

    BrandConverter INSTANCE = Mappers.getMapper(BrandConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehBrandPo PO
     * @return 领域对象
     */
    Brand toDomain(VehBrandPo vehBrandPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehBrandPoList PO 列表
     * @return 领域对象列表
     */
    List<Brand> toDomainList(List<VehBrandPo> vehBrandPoList);

    /**
     * 领域对象转 PO
     *
     * @param brand 领域对象
     * @return PO
     */
    VehBrandPo fromDomain(Brand brand);
}
