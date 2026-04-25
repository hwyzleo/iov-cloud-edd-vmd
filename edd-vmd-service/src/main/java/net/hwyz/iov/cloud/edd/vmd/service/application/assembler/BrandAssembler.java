package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台品牌转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BrandAssembler {

    BrandAssembler INSTANCE = Mappers.getMapper(BrandAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param brand 领域对象
     * @return 数据传输对象
     */
    BrandVo fromDomain(Brand brand);

    /**
     * 数据传输对象转领域对象
     *
     * @param brandVo 数据传输对象
     * @return 领域对象
     */
    Brand toDomain(BrandVo brandVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param brandList 领域对象列表
     * @return 数据传输对象列表
     */
    List<BrandVo> fromDomainList(List<Brand> brandList);

}
