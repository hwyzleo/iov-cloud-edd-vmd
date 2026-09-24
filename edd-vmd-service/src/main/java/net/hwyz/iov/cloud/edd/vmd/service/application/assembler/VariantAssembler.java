package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VariantHierarchy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantAssembler {

    VariantAssembler INSTANCE = Mappers.getMapper(VariantAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param variant 领域对象
     * @return DTO
     */
    VariantDto fromDomain(Variant variant);

    /**
     * DTO 转领域对象
     *
     * @param variantDto DTO
     * @return 领域对象
     */
    Variant toDomain(VariantDto variantDto);

    /**
     * 命令转领域对象
     *
     * @param variantCmd 命令
     * @return 领域对象
     */
    Variant toDomain(VariantCmd variantCmd);

    /**
     * 产品树补全视图转 DTO（基础字段 + 派生平台/车系，CR-048）
     *
     * @param hierarchy 版本产品树补全值对象
     * @return DTO
     */
    VariantDto fromHierarchy(VariantHierarchy hierarchy);

    /**
     * 产品树补全视图列表转 DTO 列表
     *
     * @param hierarchyList 版本产品树补全值对象列表
     * @return DTO 列表
     */
    List<VariantDto> fromHierarchyList(List<VariantHierarchy> hierarchyList);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param variantList 领域对象列表
     * @return DTO 列表
     */
    List<VariantDto> fromDomainList(List<Variant> variantList);

}
