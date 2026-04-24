package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆特征值转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureCodeAssembler {

    FeatureCodeAssembler INSTANCE = Mappers.getMapper(FeatureCodeAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehFeatureCodePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    FeatureCodeVo fromPo(VehFeatureCodePo vehFeatureCodePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param featureCodeVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehFeatureCodePo toPo(FeatureCodeVo featureCodeVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehFeatureCodePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FeatureCodeVo> fromPoList(List<VehFeatureCodePo> vehFeatureCodePoList);

}
