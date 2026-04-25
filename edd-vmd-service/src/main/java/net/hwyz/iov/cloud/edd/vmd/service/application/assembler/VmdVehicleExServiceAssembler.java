package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleExService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车辆转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VmdVehicleExServiceAssembler {

    VmdVehicleExServiceAssembler INSTANCE = Mappers.getMapper(VmdVehicleExServiceAssembler.class);

    /**
     * 领域对象转对外服务对象
     *
     * @param vehicleBasicInfo 领域对象
     * @return 对外服务对象
     */
    @Mappings({})
    VehicleExService fromDomain(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 对外服务对象转领域对象
     *
     * @param vehicleExService 对外服务对象
     * @return 领域对象
     */
    @Mappings({})
    VehicleBasicInfo toDomain(VehicleExService vehicleExService);

    /**
     * 领域对象列表转对外服务对象列表
     *
     * @param vehicleBasicInfoList 领域对象列表
     * @return 对外服务对象列表
     */
    List<VehicleExService> fromDomainList(List<VehicleBasicInfo> vehicleBasicInfoList);

}
