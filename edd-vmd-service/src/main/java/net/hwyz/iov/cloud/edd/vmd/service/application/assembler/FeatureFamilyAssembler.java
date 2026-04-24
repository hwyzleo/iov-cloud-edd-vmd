package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureFamilyPo;
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
public interface FeatureFamilyAssembler {

    FeatureFamilyAssembler INSTANCE = Mappers.getMapper(FeatureFamilyAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehFeatureFamilyPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    FeatureFamilyVo fromPo(VehFeatureFamilyPo vehFeatureFamilyPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param featureFamilyVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehFeatureFamilyPo toPo(FeatureFamilyVo featureFamilyVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehFeatureFamilyPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FeatureFamilyVo> fromPoList(List<VehFeatureFamilyPo> vehFeatureFamilyPoList);

}
