package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车辆-零件绑定关系转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartBindingExServiceAssembler {

    VehiclePartBindingExServiceAssembler INSTANCE = Mappers.getMapper(VehiclePartBindingExServiceAssembler.class);

    /**
     * 领域对象转对外服务对象
     *
     * @param vehiclePart 领域对象
     * @return 对外服务对象
     */
    @Mappings({
            @Mapping(source = "id", target = "bindingId"),
            @Mapping(target = "partCode", ignore = true),
            @Mapping(target = "sn", ignore = true),
            @Mapping(target = "deviceCategory", ignore = true)
    })
    VehiclePartBindingExResponse fromDomain(VehiclePart vehiclePart);

    /**
     * 领域对象列表转对外服务对象列表
     *
     * @param vehiclePartList 领域对象列表
     * @return 对外服务对象列表
     */
    List<VehiclePartBindingExResponse> fromDomainList(List<VehiclePart> vehiclePartList);

}