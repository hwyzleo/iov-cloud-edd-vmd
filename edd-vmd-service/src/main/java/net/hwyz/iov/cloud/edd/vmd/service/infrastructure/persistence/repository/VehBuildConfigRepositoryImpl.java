package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BuildConfigConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBuildConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 生产配置数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehBuildConfigRepositoryImpl implements VehBuildConfigRepository {

    private final VehBuildConfigMapper vehBuildConfigMapper;

    @Override
    public List<BuildConfig> selectByMap(Map<String, Object> map) {
        return BuildConfigConverter.INSTANCE.toDomainList(vehBuildConfigMapper.selectPoByMap(map));
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehBuildConfigMapper.countPoByMap(map);
    }

    @Override
    public BuildConfig selectById(Long id) {
        return BuildConfigConverter.INSTANCE.toDomain(vehBuildConfigMapper.selectPoById(id));
    }

    @Override
    public BuildConfig selectByCode(String code) {
        return BuildConfigConverter.INSTANCE.toDomain(vehBuildConfigMapper.selectPoByCode(code));
    }

    @Override
    public int insert(BuildConfig buildConfig) {
        return vehBuildConfigMapper.insertPo(BuildConfigConverter.INSTANCE.fromDomain(buildConfig));
    }

    @Override
    public int update(BuildConfig buildConfig) {
        return vehBuildConfigMapper.updatePo(BuildConfigConverter.INSTANCE.fromDomain(buildConfig));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehBuildConfigMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<BuildConfig> selectByExample(BuildConfig example) {
        return BuildConfigConverter.INSTANCE.toDomainList(vehBuildConfigMapper.selectPoByExample(BuildConfigConverter.INSTANCE.fromDomain(example)));
    }

}
