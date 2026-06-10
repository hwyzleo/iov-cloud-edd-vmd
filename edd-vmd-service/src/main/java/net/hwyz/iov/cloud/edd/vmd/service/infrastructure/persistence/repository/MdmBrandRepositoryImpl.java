package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BrandConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmBrandMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmBrandPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
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
public class MdmBrandRepositoryImpl implements MdmBrandRepository {

    private final MdmBrandMapper mdmBrandMapper;

    @Override
    public List<Brand> selectByMap(Map<String, Object> map) {
        List<MdmBrandPo> poList = mdmBrandMapper.selectPoByMap(map);
        return PageUtil.convert(poList, BrandConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmBrandMapper.countPoByMap(map);
    }

    @Override
    public Brand selectById(Long id) {
        return BrandConverter.INSTANCE.toDomain(mdmBrandMapper.selectPoById(id));
    }

    @Override
    public Brand selectByCode(String code) {
        return BrandConverter.INSTANCE.toDomain(mdmBrandMapper.selectPoByCode(code));
    }

    @Override
    public Brand selectByExternalRefId(String externalRefId) {
        return BrandConverter.INSTANCE.toDomain(mdmBrandMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmBrandMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Brand brand) {
        return mdmBrandMapper.insertPo(BrandConverter.INSTANCE.fromDomain(brand));
    }

    @Override
    public int update(Brand brand) {
        return mdmBrandMapper.updatePo(BrandConverter.INSTANCE.fromDomain(brand));
    }

    @Override
    public int updateById(Brand brand) {
        return mdmBrandMapper.updatePo(BrandConverter.INSTANCE.fromDomain(brand));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmBrandMapper.batchPhysicalDeletePo(ids);
    }

}
