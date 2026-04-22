package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureFamilyDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆特征族转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureFamilyMapper {

    FeatureFamilyMapper INSTANCE = Mappers.getMapper(FeatureFamilyMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehFeatureFamilyDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    FeatureFamilyVo fromDo(VmdVehFeatureFamilyDo vehFeatureFamilyDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param featureFamilyVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehFeatureFamilyDo toDo(FeatureFamilyVo featureFamilyVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehFeatureFamilyDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FeatureFamilyVo> fromDoList(List<VmdVehFeatureFamilyDo> vehFeatureFamilyDoList);

}
