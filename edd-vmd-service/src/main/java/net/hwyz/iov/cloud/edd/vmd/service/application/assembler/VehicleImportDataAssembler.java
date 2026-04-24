package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆导入数据转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleImportDataAssembler {

    VehicleImportDataAssembler INSTANCE = Mappers.getMapper(VehicleImportDataAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleImportDataPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleImportDataVo fromPo(VehImportDataPo vehicleImportDataPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleImportDataVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehImportDataPo toPo(VehicleImportDataVo vehicleImportDataVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleImportDataPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleImportDataVo> fromPoList(List<VehImportDataPo> vehicleImportDataPoList);

}
