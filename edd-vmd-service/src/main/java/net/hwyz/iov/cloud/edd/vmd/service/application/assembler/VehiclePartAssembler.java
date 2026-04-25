package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆零件转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartAssembler {

    VehiclePartAssembler INSTANCE = Mappers.getMapper(VehiclePartAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param vehiclePart 领域对象
     * @return 数据传输对象
     */
    VehiclePartVo fromDomain(VehiclePart vehiclePart);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehiclePartVo 数据传输对象
     * @return 领域对象
     */
    VehiclePart toDomain(VehiclePartVo vehiclePartVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehiclePartList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehiclePartVo> fromDomainList(List<VehiclePart> vehiclePartList);

}
