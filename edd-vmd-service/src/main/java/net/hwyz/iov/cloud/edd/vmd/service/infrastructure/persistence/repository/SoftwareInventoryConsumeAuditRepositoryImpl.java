package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SoftwareInventoryConsumeAuditRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.SoftwareInventoryConsumeAuditConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.SoftwareInventoryConsumeAuditMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SoftwareInventoryConsumeAuditPo;
import org.springframework.stereotype.Repository;

/**
 * OTA 车辆软件观测消费审计仓储接口实现类
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SoftwareInventoryConsumeAuditRepositoryImpl implements SoftwareInventoryConsumeAuditRepository {

    private final SoftwareInventoryConsumeAuditMapper softwareInventoryConsumeAuditMapper;

    @Override
    public SoftwareInventoryConsumeAudit selectById(Long id) {
        return SoftwareInventoryConsumeAuditConverter.INSTANCE.toDomain(softwareInventoryConsumeAuditMapper.selectById(id));
    }

    @Override
    public SoftwareInventoryConsumeAudit selectByEventId(String eventId) {
        LambdaQueryWrapper<SoftwareInventoryConsumeAuditPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SoftwareInventoryConsumeAuditPo::getEventId, eventId)
               .eq(SoftwareInventoryConsumeAuditPo::getRowValid, 1);
        return SoftwareInventoryConsumeAuditConverter.INSTANCE.toDomain(softwareInventoryConsumeAuditMapper.selectOne(wrapper));
    }

    @Override
    public SoftwareInventoryConsumeAudit selectByObservationKey(String observationKey) {
        LambdaQueryWrapper<SoftwareInventoryConsumeAuditPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SoftwareInventoryConsumeAuditPo::getObservationKey, observationKey)
               .eq(SoftwareInventoryConsumeAuditPo::getRowValid, 1);
        return SoftwareInventoryConsumeAuditConverter.INSTANCE.toDomain(softwareInventoryConsumeAuditMapper.selectOne(wrapper));
    }

    @Override
    public int insert(SoftwareInventoryConsumeAudit audit) {
        SoftwareInventoryConsumeAuditPo po = SoftwareInventoryConsumeAuditConverter.INSTANCE.fromDomain(audit);
        int result = softwareInventoryConsumeAuditMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            audit.setId(po.getId());
        }
        return result;
    }

    @Override
    public int update(SoftwareInventoryConsumeAudit audit) {
        return softwareInventoryConsumeAuditMapper.updateById(
                SoftwareInventoryConsumeAuditConverter.INSTANCE.fromDomain(audit));
    }
}
