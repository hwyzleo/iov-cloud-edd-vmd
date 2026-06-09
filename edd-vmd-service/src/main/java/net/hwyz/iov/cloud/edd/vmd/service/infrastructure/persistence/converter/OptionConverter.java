package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehOptionCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehOptionFamilyPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 选装相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface OptionConverter {

    OptionConverter INSTANCE = Mappers.getMapper(OptionConverter.class);

    // ==================== 选装族 ====================

    OptionFamily toFamilyDomain(VehOptionFamilyPo po);

    List<OptionFamily> toFamilyDomainList(List<VehOptionFamilyPo> poList);

    VehOptionFamilyPo fromFamilyDomain(OptionFamily domain);

    // ==================== 选装值 ====================

    OptionCode toCodeDomain(VehOptionCodePo po);

    List<OptionCode> toCodeDomainList(List<VehOptionCodePo> poList);

    VehOptionCodePo fromCodeDomain(OptionCode domain);
}
