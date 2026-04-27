package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehManufacturerRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ManufacturerConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehManufacturerMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehManufacturerPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 生产厂商数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehManufacturerRepositoryImpl implements VehManufacturerRepository {

    private final VehManufacturerMapper vehManufacturerMapper;

    @Override
    public List<Manufacturer> selectByMap(Map<String, Object> map) {
        List<VehManufacturerPo> poList = vehManufacturerMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ManufacturerConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehManufacturerMapper.countPoByMap(map);
    }

    @Override
    public Manufacturer selectById(Long id) {
        return ManufacturerConverter.INSTANCE.toDomain(vehManufacturerMapper.selectPoById(id));
    }

    @Override
    public Manufacturer selectByCode(String code) {
        return ManufacturerConverter.INSTANCE.toDomain(vehManufacturerMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Manufacturer manufacturer) {
        return vehManufacturerMapper.insertPo(ManufacturerConverter.INSTANCE.fromDomain(manufacturer));
    }

    @Override
    public int update(Manufacturer manufacturer) {
        return vehManufacturerMapper.updatePo(ManufacturerConverter.INSTANCE.fromDomain(manufacturer));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehManufacturerMapper.batchPhysicalDeletePo(ids);
    }

}
