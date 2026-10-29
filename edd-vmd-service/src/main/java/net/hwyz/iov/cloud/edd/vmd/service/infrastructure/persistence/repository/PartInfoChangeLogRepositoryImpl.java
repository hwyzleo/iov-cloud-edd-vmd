package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartInfoChangeLogConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartInfoChangeLogMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoChangeLogPo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 零件实例属性变更历史数据仓库实现（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartInfoChangeLogRepositoryImpl implements PartInfoChangeLogRepository {

    private final PartInfoChangeLogMapper partInfoChangeLogMapper;

    /**
     * 审计字段兜底填充（不依赖框架自动填充，保证 NOT NULL 列可写）
     */
    private void fillAudit(net.hwyz.iov.cloud.framework.mysql.po.BasePo po) {
        if (po.getCreateTime() == null) {
            po.setCreateTime(new java.util.Date());
        }
        if (po.getModifyTime() == null) {
            po.setModifyTime(new java.util.Date());
        }
        if (po.getRowVersion() == null) {
            po.setRowVersion(1);
        }
        if (po.getRowValid() == null) {
            po.setRowValid(true);
        }
    }

    @Override
    public int insert(PartInfoChangeLog changeLog) {
        PartInfoChangeLogPo po = PartInfoChangeLogConverter.INSTANCE.fromDomain(changeLog);
        fillAudit(po);
        int result = partInfoChangeLogMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            changeLog.setId(po.getId());
        }
        return result;
    }

    @Override
    public PartInfoChangeLog selectByPartIdAndRowVersion(Long partId, Integer rowVersion) {
        PartInfoChangeLogPo po = partInfoChangeLogMapper.selectByPartIdAndRowVersion(partId, rowVersion);
        return PartInfoChangeLogConverter.INSTANCE.toDomain(po);
    }

    @Override
    public List<PartInfoChangeLog> selectByPartId(Long partId) {
        List<PartInfoChangeLogPo> poList = partInfoChangeLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PartInfoChangeLogPo>()
                        .eq(PartInfoChangeLogPo::getPartId, partId)
                        .orderByDesc(PartInfoChangeLogPo::getChangeTime));
        return PartInfoChangeLogConverter.INSTANCE.toDomainList(poList);
    }

}
