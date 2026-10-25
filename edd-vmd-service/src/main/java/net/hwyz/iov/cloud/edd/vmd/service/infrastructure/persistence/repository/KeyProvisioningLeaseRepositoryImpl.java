package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.KeyProvisioningLease;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.KeyProvisioningLeaseRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.KeyProvisioningLeaseConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.KeyProvisioningLeaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.KeyProvisioningLeasePo;

import java.util.Collections;
import java.util.List;

/**
 * KL1 交付租约仓储实现（VMD-DSN-CR-069）
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Repository
@RequiredArgsConstructor
public class KeyProvisioningLeaseRepositoryImpl implements KeyProvisioningLeaseRepository {

    private final KeyProvisioningLeaseMapper keyProvisioningLeaseMapper;

    @Override
    public KeyProvisioningLease selectById(Long id) {
        KeyProvisioningLeasePo po = keyProvisioningLeaseMapper.selectPoById(id);
        return po != null ? KeyProvisioningLeaseConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public KeyProvisioningLease selectByProvisioningIdAndSeq(Long provisioningId, Integer leaseSeq) {
        KeyProvisioningLeasePo po = keyProvisioningLeaseMapper
                .selectPoByProvisioningIdAndSeq(provisioningId, leaseSeq);
        return po != null ? KeyProvisioningLeaseConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public KeyProvisioningLease selectByMessageId(String messageId) {
        KeyProvisioningLeasePo po = keyProvisioningLeaseMapper.selectPoByMessageId(messageId);
        return po != null ? KeyProvisioningLeaseConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public List<KeyProvisioningLease> selectListByProvisioningId(Long provisioningId) {
        List<KeyProvisioningLeasePo> poList = keyProvisioningLeaseMapper.selectPoListByProvisioningId(provisioningId);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return KeyProvisioningLeaseConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public Integer maxLeaseSeq(Long provisioningId) {
        Integer max = keyProvisioningLeaseMapper.selectMaxLeaseSeq(provisioningId);
        return max != null ? max : 0;
    }

    @Override
    public int insert(KeyProvisioningLease lease) {
        KeyProvisioningLeasePo po = KeyProvisioningLeaseConverter.INSTANCE.fromDomain(lease);
        int rows = keyProvisioningLeaseMapper.insertPo(po);
        lease.setId(po.getId());
        lease.setRowVersion(1);
        return rows;
    }

    @Override
    public int update(KeyProvisioningLease lease) {
        KeyProvisioningLeasePo po = KeyProvisioningLeaseConverter.INSTANCE.fromDomain(lease);
        return keyProvisioningLeaseMapper.updatePo(po);
    }
}
