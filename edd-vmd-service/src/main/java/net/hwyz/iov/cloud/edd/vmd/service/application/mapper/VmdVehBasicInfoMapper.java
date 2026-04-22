package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆基础信息数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VmdVehBasicInfoMapper {

    VmdVehBasicInfoMapper INSTANCE = Mappers.getMapper(VmdVehBasicInfoMapper.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehBasicInfoDo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    Vehicle toDo(VmdVehBasicInfoDo vehBasicInfoDo);

    /**
     * 领域对象转数据对象
     *
     * @param vehicleDo 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBasicInfoDo fromDo(Vehicle vehicleDo);

}
