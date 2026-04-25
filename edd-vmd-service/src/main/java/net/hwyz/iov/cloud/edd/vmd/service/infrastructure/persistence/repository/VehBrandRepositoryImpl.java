package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BrandConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBrandMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 品牌数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehBrandRepositoryImpl implements VehBrandRepository {

    private final VehBrandMapper vehBrandMapper;

    @Override
    public List<Brand> selectByMap(Map<String, Object> map) {
        return BrandConverter.INSTANCE.toDomainList(vehBrandMapper.selectPoByMap(map));
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehBrandMapper.countPoByMap(map);
    }

    @Override
    public Brand selectById(Long id) {
        return BrandConverter.INSTANCE.toDomain(vehBrandMapper.selectPoById(id));
    }

    @Override
    public Brand selectByCode(String code) {
        return BrandConverter.INSTANCE.toDomain(vehBrandMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Brand brand) {
        return vehBrandMapper.insertPo(BrandConverter.INSTANCE.fromDomain(brand));
    }

    @Override
    public int update(Brand brand) {
        return vehBrandMapper.updatePo(BrandConverter.INSTANCE.fromDomain(brand));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehBrandMapper.batchPhysicalDeletePo(ids);
    }

}
