package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SupplierRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.SupplierConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.SupplierMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SupplierPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 供应商数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SupplierRepositoryImpl implements SupplierRepository {

    private final SupplierMapper supplierMapper;

    @Override
    public List<Supplier> selectByMap(Map<String, Object> map) {
        List<SupplierPo> poList = supplierMapper.selectPoByMap(map);
        return PageUtil.convert(poList, SupplierConverter.INSTANCE::toDomain);
    }

    @Override
    public Supplier selectById(Long id) {
        return SupplierConverter.INSTANCE.toDomain(supplierMapper.selectPoById(id));
    }

    @Override
    public Supplier selectByCode(String code) {
        return SupplierConverter.INSTANCE.toDomain(supplierMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Supplier supplier) {
        return supplierMapper.insertPo(SupplierConverter.INSTANCE.fromDomain(supplier));
    }

    @Override
    public int update(Supplier supplier) {
        return supplierMapper.updatePo(SupplierConverter.INSTANCE.fromDomain(supplier));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return supplierMapper.batchPhysicalDeletePo(ids);
    }

}
