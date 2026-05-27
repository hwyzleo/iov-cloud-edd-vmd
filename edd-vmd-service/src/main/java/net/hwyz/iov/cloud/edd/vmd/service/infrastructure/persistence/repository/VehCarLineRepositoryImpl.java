package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.CarLineConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehCarLineMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehCarLinePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车系数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehCarLineRepositoryImpl implements VehCarLineRepository {

    private final VehCarLineMapper vehCarLineMapper;

    @Override
    public List<CarLine> selectByMap(Map<String, Object> map) {
        List<VehCarLinePo> vehCarLinePoList = vehCarLineMapper.selectPoByMap(map);
        return PageUtil.convert(vehCarLinePoList, CarLineConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehCarLineMapper.countPoByMap(map);
    }

    @Override
    public CarLine selectById(Long id) {
        return CarLineConverter.INSTANCE.toDomain(vehCarLineMapper.selectPoById(id));
    }

    @Override
    public CarLine selectByCode(String code) {
        return CarLineConverter.INSTANCE.toDomain(vehCarLineMapper.selectPoByCode(code));
    }

    @Override
    public CarLine selectByExternalRefId(String externalRefId) {
        return CarLineConverter.INSTANCE.toDomain(vehCarLineMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return vehCarLineMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(CarLine carLine) {
        return vehCarLineMapper.insertPo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int update(CarLine carLine) {
        return vehCarLineMapper.updatePo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int updateById(CarLine carLine) {
        return vehCarLineMapper.updatePo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehCarLineMapper.batchPhysicalDeletePo(ids);
    }

}
