package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PlatformConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehPlatformMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPlatformPo;
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
public class VehPlatformRepositoryImpl implements VehPlatformRepository {

    private final VehPlatformMapper vehPlatformMapper;

    @Override
    public List<Platform> selectByMap(Map<String, Object> map) {
        List<VehPlatformPo> poList = vehPlatformMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PlatformConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehPlatformMapper.countPoByMap(map);
    }

    @Override
    public Platform selectById(Long id) {
        return PlatformConverter.INSTANCE.toDomain(vehPlatformMapper.selectPoById(id));
    }

    @Override
    public Platform selectByCode(String code) {
        return PlatformConverter.INSTANCE.toDomain(vehPlatformMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Platform platform) {
        return vehPlatformMapper.insertPo(PlatformConverter.INSTANCE.fromDomain(platform));
    }

    @Override
    public int update(Platform platform) {
        return vehPlatformMapper.updatePo(PlatformConverter.INSTANCE.fromDomain(platform));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehPlatformMapper.batchPhysicalDeletePo(ids);
    }

}
