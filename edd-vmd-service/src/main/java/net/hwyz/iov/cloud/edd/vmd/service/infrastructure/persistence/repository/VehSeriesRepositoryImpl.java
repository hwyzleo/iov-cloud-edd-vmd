package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSeriesRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.SeriesConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehSeriesMapper;
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
public class VehSeriesRepositoryImpl implements VehSeriesRepository {

    private final VehSeriesMapper vehSeriesMapper;

    @Override
    public List<Series> selectByMap(Map<String, Object> map) {
        return SeriesConverter.INSTANCE.toDomainList(vehSeriesMapper.selectPoByMap(map));
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehSeriesMapper.countPoByMap(map);
    }

    @Override
    public Series selectById(Long id) {
        return SeriesConverter.INSTANCE.toDomain(vehSeriesMapper.selectPoById(id));
    }

    @Override
    public Series selectByCode(String code) {
        return SeriesConverter.INSTANCE.toDomain(vehSeriesMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Series series) {
        return vehSeriesMapper.insertPo(SeriesConverter.INSTANCE.fromDomain(series));
    }

    @Override
    public int update(Series series) {
        return vehSeriesMapper.updatePo(SeriesConverter.INSTANCE.fromDomain(series));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehSeriesMapper.batchPhysicalDeletePo(ids);
    }

}
