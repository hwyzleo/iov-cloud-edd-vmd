package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmOptionCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmOptionFamilyPo;
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

    OptionFamily toFamilyDomain(MdmOptionFamilyPo po);

    List<OptionFamily> toFamilyDomainList(List<MdmOptionFamilyPo> poList);

    MdmOptionFamilyPo fromFamilyDomain(OptionFamily domain);

    // ==================== 选装值 ====================

    OptionCode toCodeDomain(MdmOptionCodePo po);

    List<OptionCode> toCodeDomainList(List<MdmOptionCodePo> poList);

    MdmOptionCodePo fromCodeDomain(OptionCode domain);
}
