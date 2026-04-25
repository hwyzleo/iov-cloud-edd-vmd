package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产配置转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BuildConfigAssembler {

    BuildConfigAssembler INSTANCE = Mappers.getMapper(BuildConfigAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param buildConfig 领域对象
     * @return 数据传输对象
     */
    BuildConfigVo fromDomain(BuildConfig buildConfig);

    /**
     * 数据传输对象转领域对象
     *
     * @param buildConfigVo 数据传输对象
     * @return 领域对象
     */
    BuildConfig toDomain(BuildConfigVo buildConfigVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param buildConfigList 领域对象列表
     * @return 数据传输对象列表
     */
    List<BuildConfigVo> fromDomainList(List<BuildConfig> buildConfigList);

}
