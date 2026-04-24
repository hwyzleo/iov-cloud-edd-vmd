package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台零件信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartAssembler {

    PartAssembler INSTANCE = Mappers.getMapper(PartAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param partPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    PartVo fromPo(PartPo partPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param partVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    PartPo toPo(PartVo partVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param partPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<PartVo> fromPoList(List<PartPo> partPoList);

}
