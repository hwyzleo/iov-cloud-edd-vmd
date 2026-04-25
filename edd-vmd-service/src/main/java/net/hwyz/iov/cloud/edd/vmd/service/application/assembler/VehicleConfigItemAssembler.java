package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆配置项转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigItemAssembler {

    VehicleConfigItemAssembler INSTANCE = Mappers.getMapper(VehicleConfigItemAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param vehicleConfigItem 领域对象
     * @return 数据传输对象
     */
    VehicleConfigItemVo fromDomain(VehicleConfigItem vehicleConfigItem);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehicleConfigItemVo 数据传输对象
     * @return 领域对象
     */
    VehicleConfigItem toDomain(VehicleConfigItemVo vehicleConfigItemVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehicleConfigItemList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigItemVo> fromDomainList(List<VehicleConfigItem> vehicleConfigItemList);

}
