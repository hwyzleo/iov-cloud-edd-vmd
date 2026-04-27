package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 零件数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartRepositoryImpl implements PartRepository {

    private final PartMapper partMapper;

    @Override
    public List<Part> selectByMap(Map<String, Object> map) {
        List<PartPo> poList = partMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PartConverter.INSTANCE::toDomain);
    }

    @Override
    public Part selectById(Long id) {
        return PartConverter.INSTANCE.toDomain(partMapper.selectPoById(id));
    }

    @Override
    public Part selectByPn(String pn) {
        return PartConverter.INSTANCE.toDomain(partMapper.selectPoByPn(pn));
    }

    @Override
    public int insert(Part part) {
        return partMapper.insertPo(PartConverter.INSTANCE.fromDomain(part));
    }

    @Override
    public int update(Part part) {
        return partMapper.updatePo(PartConverter.INSTANCE.fromDomain(part));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return partMapper.batchPhysicalDeletePo(ids);
    }

}
