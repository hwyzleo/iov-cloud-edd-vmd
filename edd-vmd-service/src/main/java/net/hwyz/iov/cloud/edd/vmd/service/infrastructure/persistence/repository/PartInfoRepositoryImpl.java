package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartInfoConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 物理零件实例数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartInfoRepositoryImpl implements PartInfoRepository {

    private final PartInfoMapper partInfoMapper;

    @Override
    public List<PartInfo> selectByMap(Map<String, Object> map) {
        List<PartInfoPo> poList = partInfoMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PartInfoConverter.INSTANCE::toDomain);
    }

    @Override
    public PartInfo selectById(Long id) {
        return PartInfoConverter.INSTANCE.toDomain(partInfoMapper.selectPoById(id));
    }

    @Override
    public PartInfo selectByPartCodeAndSn(String partCode, String sn) {
        return PartInfoConverter.INSTANCE.toDomain(partInfoMapper.selectPoByPartCodeAndSn(partCode, sn));
    }

    @Override
    public PartInfo selectBySn(String sn) {
        return PartInfoConverter.INSTANCE.toDomain(partInfoMapper.selectPoBySn(sn));
    }

    @Override
    public int insert(PartInfo partInfo) {
        PartInfoPo po = PartInfoConverter.INSTANCE.fromDomain(partInfo);
        int result = partInfoMapper.insertPo(po);
        // 将生成的 ID 回写到领域对象
        if (result > 0 && po.getId() != null) {
            partInfo.setId(po.getId());
        }
        return result;
    }

    @Override
    public int batchInsert(List<PartInfo> partInfoList) {
        return partInfoMapper.batchInsertPo(PartInfoConverter.INSTANCE.fromDomainList(partInfoList));
    }

    @Override
    public int update(PartInfo partInfo) {
        return partInfoMapper.updatePo(PartInfoConverter.INSTANCE.fromDomain(partInfo));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return partInfoMapper.batchPhysicalDeletePo(ids);
    }

}
