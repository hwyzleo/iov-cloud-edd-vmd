package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆配置转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigAssembler {

    VehicleConfigAssembler INSTANCE = Mappers.getMapper(VehicleConfigAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param vehicleConfig 领域对象
     * @return 数据传输对象
     */
    @Mapping(target = "state", source = "configState")
    VehicleConfigVo fromDomain(VehicleConfig vehicleConfig);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehicleConfigVo 数据传输对象
     * @return 领域对象
     */
    @Mapping(target = "configState", source = "state")
    VehicleConfig toDomain(VehicleConfigVo vehicleConfigVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehicleConfigList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigVo> fromDomainList(List<VehicleConfig> vehicleConfigList);

}
