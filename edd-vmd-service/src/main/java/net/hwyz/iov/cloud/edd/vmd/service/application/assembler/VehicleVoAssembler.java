package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleVoAssembler {

    VehicleVoAssembler INSTANCE = Mappers.getMapper(VehicleVoAssembler.class);

    /**
     * 数据对象转VO
     *
     * @param vehBasicInfoPo 数据对象
     * @return VO
     */
    @Mappings({})
    VehicleVo fromPo(VehBasicInfoPo vehBasicInfoPo);

    /**
     * VO转数据对象
     *
     * @param vehicleVo VO
     * @return 数据对象
     */
    @Mappings({})
    VehBasicInfoPo toPo(VehicleVo vehicleVo);

    /**
     * 数据对象列表转VO列表
     *
     * @param vehBasicInfoPoList 数据对象列表
     * @return VO列表
     */
    List<VehicleVo> fromPoList(List<VehBasicInfoPo> vehBasicInfoPoList);

    /**
     * DTO转VO
     *
     * @param vehicleDto DTO
     * @return VO
     */
    VehicleVo fromDto(VehicleDto vehicleDto);

}
