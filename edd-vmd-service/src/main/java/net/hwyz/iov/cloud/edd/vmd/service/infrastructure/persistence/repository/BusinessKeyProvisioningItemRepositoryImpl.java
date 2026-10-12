package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BusinessKeyProvisioningItemConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.BusinessKeyProvisioningItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningItemPo;

import java.util.Collections;
import java.util.List;

/**
 * 业务密钥开通批次 ACK 明细仓储实现（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Repository
@RequiredArgsConstructor
public class BusinessKeyProvisioningItemRepositoryImpl implements BusinessKeyProvisioningItemRepository {

    private final BusinessKeyProvisioningItemMapper businessKeyProvisioningItemMapper;

    @Override
    public BusinessKeyProvisioningItem selectById(Long id) {
        BusinessKeyProvisioningItemPo po = businessKeyProvisioningItemMapper.selectPoById(id);
        return po != null ? BusinessKeyProvisioningItemConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public List<BusinessKeyProvisioningItem> selectByProvisioningId(Long provisioningId) {
        List<BusinessKeyProvisioningItemPo> poList = businessKeyProvisioningItemMapper.selectPoListByProvisioningId(provisioningId);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return BusinessKeyProvisioningItemConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public BusinessKeyProvisioningItem selectByProvisioningIdAndPurpose(Long provisioningId, String purpose) {
        BusinessKeyProvisioningItemPo po = businessKeyProvisioningItemMapper
                .selectPoByProvisioningIdAndPurpose(provisioningId, purpose);
        return po != null ? BusinessKeyProvisioningItemConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public int insert(BusinessKeyProvisioningItem entity) {
        BusinessKeyProvisioningItemPo po = BusinessKeyProvisioningItemConverter.INSTANCE.fromDomain(entity);
        int rows = businessKeyProvisioningItemMapper.insertPo(po);
        entity.setId(po.getId());
        entity.setRowVersion(1);
        return rows;
    }

    @Override
    public int update(BusinessKeyProvisioningItem entity) {
        BusinessKeyProvisioningItemPo po = BusinessKeyProvisioningItemConverter.INSTANCE.fromDomain(entity);
        return businessKeyProvisioningItemMapper.updatePo(po);
    }
}
