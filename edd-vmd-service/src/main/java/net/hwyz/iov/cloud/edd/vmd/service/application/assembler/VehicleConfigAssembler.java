package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigPo;
import org.mapstruct.Mapper;
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
     * 数据对象转数据传输对象
     *
     * @param vehicleConfigPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleConfigVo fromPo(VehicleConfigPo vehicleConfigPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleConfigVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehicleConfigPo toPo(VehicleConfigVo vehicleConfigVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleConfigPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigVo> fromPoList(List<VehicleConfigPo> vehicleConfigPoList);

}
