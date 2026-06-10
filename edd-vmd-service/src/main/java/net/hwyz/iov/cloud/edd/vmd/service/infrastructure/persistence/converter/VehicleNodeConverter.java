package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVehicleNodePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车载节点领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleNodeConverter {

    VehicleNodeConverter INSTANCE = Mappers.getMapper(VehicleNodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    VehicleNode toDomain(MdmVehicleNodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<VehicleNode> toDomainList(List<MdmVehicleNodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param vehicleNode 领域对象
     * @return PO
     */
    MdmVehicleNodePo fromDomain(VehicleNode vehicleNode);
}
