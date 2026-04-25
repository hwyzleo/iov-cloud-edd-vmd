package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆 VO 转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleVoAssembler {

    VehicleVoAssembler INSTANCE = Mappers.getMapper(VehicleVoAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param vehicleBasicInfo 领域对象
     * @return 数据传输对象
     */
    VehicleVo fromDomain(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehicleVo 数据传输对象
     * @return 领域对象
     */
    VehicleBasicInfo toDomain(VehicleVo vehicleVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehicleBasicInfoList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehicleVo> fromDomainList(List<VehicleBasicInfo> vehicleBasicInfoList);

    /**
     * DTO 转数据传输对象
     *
     * @param vehicleDto DTO
     * @return 数据传输对象
     */
    VehicleVo fromDto(VehicleDto vehicleDto);

}
