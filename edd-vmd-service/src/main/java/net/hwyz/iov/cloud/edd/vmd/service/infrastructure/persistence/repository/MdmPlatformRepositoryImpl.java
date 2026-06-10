package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PlatformConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmPlatformMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPlatformPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 平台数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MdmPlatformRepositoryImpl implements MdmPlatformRepository {

    private final MdmPlatformMapper mdmPlatformMapper;

    @Override
    public List<Platform> selectByMap(Map<String, Object> map) {
        List<MdmPlatformPo> poList = mdmPlatformMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PlatformConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmPlatformMapper.countPoByMap(map);
    }

    @Override
    public Platform selectById(Long id) {
        return PlatformConverter.INSTANCE.toDomain(mdmPlatformMapper.selectPoById(id));
    }

    @Override
    public Platform selectByCode(String code) {
        return PlatformConverter.INSTANCE.toDomain(mdmPlatformMapper.selectPoByCode(code));
    }

    @Override
    public Platform selectByExternalRefId(String externalRefId) {
        return PlatformConverter.INSTANCE.toDomain(mdmPlatformMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmPlatformMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Platform platform) {
        return mdmPlatformMapper.insertPo(PlatformConverter.INSTANCE.fromDomain(platform));
    }

    @Override
    public int update(Platform platform) {
        return mdmPlatformMapper.updatePo(PlatformConverter.INSTANCE.fromDomain(platform));
    }

    @Override
    public int updateById(Platform platform) {
        return mdmPlatformMapper.updatePo(PlatformConverter.INSTANCE.fromDomain(platform));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmPlatformMapper.batchPhysicalDeletePo(ids);
    }

}
