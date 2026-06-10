package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.CarLineConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmCarLineMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmCarLinePo;
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
public class MdmCarLineRepositoryImpl implements MdmCarLineRepository {

    private final MdmCarLineMapper mdmCarLineMapper;

    @Override
    public List<CarLine> selectByMap(Map<String, Object> map) {
        List<MdmCarLinePo> mdmCarLinePoList = mdmCarLineMapper.selectPoByMap(map);
        return PageUtil.convert(mdmCarLinePoList, CarLineConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmCarLineMapper.countPoByMap(map);
    }

    @Override
    public CarLine selectById(Long id) {
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectPoById(id));
    }

    @Override
    public CarLine selectByCode(String code) {
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectPoByCode(code));
    }

    @Override
    public CarLine selectByExternalRefId(String externalRefId) {
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmCarLineMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(CarLine carLine) {
        return mdmCarLineMapper.insertPo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int update(CarLine carLine) {
        return mdmCarLineMapper.updatePo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int updateById(CarLine carLine) {
        return mdmCarLineMapper.updatePo(CarLineConverter.INSTANCE.fromDomain(carLine));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmCarLineMapper.batchPhysicalDeletePo(ids);
    }

}
