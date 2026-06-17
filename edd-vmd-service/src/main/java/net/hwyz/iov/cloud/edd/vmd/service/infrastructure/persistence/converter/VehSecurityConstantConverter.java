package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆安全常量转换器
 * 
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Mapper
public interface VehSecurityConstantConverter {

    VehSecurityConstantConverter INSTANCE = Mappers.getMapper(VehSecurityConstantConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    VehSecurityConstant toDomain(VehSecurityConstantPo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<VehSecurityConstant> toDomainList(List<VehSecurityConstantPo> poList);

    /**
     * 领域对象转 PO
     *
     * @param entity 领域对象
     * @return PO
     */
    VehSecurityConstantPo fromDomain(VehSecurityConstant entity);

    /**
     * 领域对象列表转 PO 列表
     *
     * @param entityList 领域对象列表
     * @return PO 列表
     */
    List<VehSecurityConstantPo> fromDomainList(List<VehSecurityConstant> entityList);

    default SecurityConstantState mapStringToSecurityConstantState(String state) {
        return state != null ? SecurityConstantState.valOf(state) : null;
    }

    default String mapSecurityConstantStateToString(SecurityConstantState state) {
        return state != null ? state.getValue() : null;
    }
}
