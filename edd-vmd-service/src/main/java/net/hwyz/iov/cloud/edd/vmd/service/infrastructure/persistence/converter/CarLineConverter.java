package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehCarLinePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车系领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface CarLineConverter {

    CarLineConverter INSTANCE = Mappers.getMapper(CarLineConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehCarLinePo PO
     * @return 领域对象
     */
    CarLine toDomain(VehCarLinePo vehCarLinePo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehCarLinePoList PO 列表
     * @return 领域对象列表
     */
    List<CarLine> toDomainList(List<VehCarLinePo> vehCarLinePoList);

    /**
     * 领域对象转 PO
     *
     * @param carLine 领域对象
     * @return PO
     */
    VehCarLinePo fromDomain(CarLine carLine);
}
