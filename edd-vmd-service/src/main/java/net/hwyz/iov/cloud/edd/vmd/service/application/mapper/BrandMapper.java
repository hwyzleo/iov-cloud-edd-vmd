package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBrandDo;
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
public interface BrandMapper {

    BrandMapper INSTANCE = Mappers.getMapper(BrandMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBrandDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BrandVo fromDo(VmdVehBrandDo vehBrandDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param brandVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBrandDo toDo(BrandVo brandVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBrandDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BrandVo> fromDoList(List<VmdVehBrandDo> vehBrandDoList);

}
