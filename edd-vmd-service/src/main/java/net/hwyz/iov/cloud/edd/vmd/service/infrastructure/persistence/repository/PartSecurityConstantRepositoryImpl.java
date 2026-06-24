package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartSecurityConstantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartSecurityConstantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSecurityConstantPo;
import org.springframework.stereotype.Repository;

/**
 * 零件安全常量仓储实现
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartSecurityConstantRepositoryImpl implements PartSecurityConstantRepository {

    private final PartSecurityConstantMapper partSecurityConstantMapper;

    @Override
    public PartSecurityConstant selectByPartCodeAndSn(String partCode, String sn) {
        PartSecurityConstantPo po = partSecurityConstantMapper.selectPoByPartCodeAndSn(partCode, sn);
        return po != null ? PartSecurityConstantConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public int insert(PartSecurityConstant entity) {
        PartSecurityConstantPo po = PartSecurityConstantConverter.INSTANCE.fromDomain(entity);
        int rows = partSecurityConstantMapper.insert(po);
        entity.setId(po.getId());
        return rows;
    }

    @Override
    public int update(PartSecurityConstant entity) {
        PartSecurityConstantPo po = PartSecurityConstantConverter.INSTANCE.fromDomain(entity);
        return partSecurityConstantMapper.updateById(po);
    }

    @Override
    public int deleteByPartCodeAndSn(String partCode, String sn) {
        return partSecurityConstantMapper.deletePoByPartCodeAndSn(partCode, sn);
    }
}
