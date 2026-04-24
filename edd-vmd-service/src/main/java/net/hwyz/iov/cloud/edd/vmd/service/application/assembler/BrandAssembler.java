package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBrandPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆品牌转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BrandAssembler {

    BrandAssembler INSTANCE = Mappers.getMapper(BrandAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBrandPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BrandVo fromPo(VehBrandPo vehBrandPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param brandVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehBrandPo toPo(BrandVo brandVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBrandPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BrandVo> fromPoList(List<VehBrandPo> vehBrandPoList);

}
