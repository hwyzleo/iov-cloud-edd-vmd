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
        // 原生 BaseMapper.selectById（@TableLogic 自动过滤 row_valid=1，与旧 selectPoById 等价）
        return BrandConverter.INSTANCE.toDomain(mdmBrandMapper.selectById(id));
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
        MdmBrandPo po = BrandConverter.INSTANCE.fromDomain(brand);
        int result = mdmBrandMapper.insertPo(po);
        if (result > 0) {
            brand.setId(po.getId());
            // insertPo 专用 SQL 固定写入 row_version=1（事实性回填，非自动补版本）
            brand.setRowVersion(1);
        }
        return result;
    }

    @Override
    public int update(Brand brand) {
        return updateWithCas(brand);
    }

    @Override
    public int updateById(Brand brand) {
        return updateWithCas(brand);
    }

    /**
     * CAS 更新：必须携带可信原版本；成功回传新版本（DB 固定 +1），失败（0 行）不修改内存版本。
     */
    private int updateWithCas(Brand brand) {
        MdmBrandPo po = BrandConverter.INSTANCE.fromDomain(brand);
        int result = mdmBrandMapper.updatePo(po);
        if (result > 0 && po.getRowVersion() != null) {
            brand.setRowVersion(po.getRowVersion() + 1);
        }
        return result;
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        // 空集合明确 no-op，不生成 IN () 非法 SQL，也不触发全表删除（CR-075 受控硬删除边界）
        if (ids == null || ids.length == 0) {
            log.debug("批量物理删除品牌：空集合，no-op");
            return 0;
        }
        return mdmBrandMapper.batchPhysicalDeletePo(ids);
    }

}
