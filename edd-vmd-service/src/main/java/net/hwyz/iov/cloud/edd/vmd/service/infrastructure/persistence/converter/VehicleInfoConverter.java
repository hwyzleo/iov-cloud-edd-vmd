package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePresetOwner;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehDetailInfoPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPresetOwnerPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆信息相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleInfoConverter {

    VehicleInfoConverter INSTANCE = Mappers.getMapper(VehicleInfoConverter.class);

    // ==================== 车辆基础信息 ====================

    VehicleBasicInfo toBasicDomain(VehBasicInfoPo po);

    List<VehicleBasicInfo> toBasicDomainList(List<VehBasicInfoPo> poList);

    VehBasicInfoPo fromBasicDomain(VehicleBasicInfo domain);

    // ==================== 车辆详细信息 ====================

    VehicleDetail toDetailDomain(VehDetailInfoPo po);

    List<VehicleDetail> toDetailDomainList(List<VehDetailInfoPo> poList);

    VehDetailInfoPo fromDetailDomain(VehicleDetail domain);

    List<VehDetailInfoPo> fromDetailDomainList(List<VehicleDetail> domainList);

    // ==================== 车辆预设车主 ====================

    VehiclePresetOwner toPresetOwnerDomain(VehPresetOwnerPo po);

    List<VehiclePresetOwner> toPresetOwnerDomainList(List<VehPresetOwnerPo> poList);

    VehPresetOwnerPo fromPresetOwnerDomain(VehiclePresetOwner domain);
}
