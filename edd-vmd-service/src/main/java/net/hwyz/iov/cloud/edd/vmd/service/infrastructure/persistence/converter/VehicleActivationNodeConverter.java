package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationNodePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆首次激活节点投影 领域对象转换器（VMD-DSN-CR-077）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleActivationNodeConverter {

    VehicleActivationNodeConverter INSTANCE = Mappers.getMapper(VehicleActivationNodeConverter.class);

    VehicleActivationNode toDomain(VehicleActivationNodePo po);

    List<VehicleActivationNode> toDomainList(List<VehicleActivationNodePo> poList);

    VehicleActivationNodePo fromDomain(VehicleActivationNode domain);

    List<VehicleActivationNodePo> fromDomainList(List<VehicleActivationNode> domainList);
}
