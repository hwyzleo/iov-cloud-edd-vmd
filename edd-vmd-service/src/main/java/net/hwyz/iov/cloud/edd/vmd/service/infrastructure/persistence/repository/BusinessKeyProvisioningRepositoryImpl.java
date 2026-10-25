package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BusinessKeyProvisioningConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.BusinessKeyProvisioningMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningPo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 业务密钥多用途开通批次仓储实现（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Repository
@RequiredArgsConstructor
public class BusinessKeyProvisioningRepositoryImpl implements BusinessKeyProvisioningRepository {

    private final BusinessKeyProvisioningMapper businessKeyProvisioningMapper;

    @Override
    public BusinessKeyProvisioning selectById(Long id) {
        BusinessKeyProvisioningPo po = businessKeyProvisioningMapper.selectPoById(id);
        return po != null ? BusinessKeyProvisioningConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public BusinessKeyProvisioning selectByRequestId(String requestId) {
        BusinessKeyProvisioningPo po = businessKeyProvisioningMapper.selectPoByRequestId(requestId);
        return po != null ? BusinessKeyProvisioningConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public BusinessKeyProvisioning selectByRequestIdForUpdate(String requestId) {
        BusinessKeyProvisioningPo po = businessKeyProvisioningMapper.selectPoByRequestIdForUpdate(requestId);
        return po != null ? BusinessKeyProvisioningConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public BusinessKeyProvisioning selectOpenByDeviceAndDomain(String deviceSn, String businessDomain) {
        BusinessKeyProvisioningPo po = businessKeyProvisioningMapper.selectOpenPoByDeviceAndDomain(deviceSn, businessDomain);
        return po != null ? BusinessKeyProvisioningConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public List<BusinessKeyProvisioning> selectOpenListByDeviceAndDomain(String deviceSn, String businessDomain) {
        List<BusinessKeyProvisioningPo> poList =
                businessKeyProvisioningMapper.selectOpenPoListByDeviceAndDomain(deviceSn, businessDomain);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return BusinessKeyProvisioningConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public List<BusinessKeyProvisioning> selectTerminalListByDeviceAndDomain(String deviceSn, String businessDomain) {
        List<BusinessKeyProvisioningPo> poList =
                businessKeyProvisioningMapper.selectTerminalPoListByDeviceAndDomain(deviceSn, businessDomain);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return BusinessKeyProvisioningConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public List<BusinessKeyProvisioning> selectListByStatusBefore(BusinessKeyProvisioningState status,
                                                                  LocalDateTime before, int limit) {
        List<BusinessKeyProvisioningPo> poList = businessKeyProvisioningMapper.selectPoListByStatusBefore(
                status != null ? status.getValue() : null, before, limit);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return BusinessKeyProvisioningConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public List<BusinessKeyProvisioning> selectKl1PendingBeforeDeadline(LocalDateTime now, int limit) {
        List<BusinessKeyProvisioningPo> poList = businessKeyProvisioningMapper
                .selectKl1PoListPendingBeforeDeadline(now, limit);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return BusinessKeyProvisioningConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public int insert(BusinessKeyProvisioning entity) {
        BusinessKeyProvisioningPo po = BusinessKeyProvisioningConverter.INSTANCE.fromDomain(entity);
        int rows = businessKeyProvisioningMapper.insertPo(po);
        entity.setId(po.getId());
        entity.setRowVersion(1);
        return rows;
    }

    @Override
    public int update(BusinessKeyProvisioning entity) {
        BusinessKeyProvisioningPo po = BusinessKeyProvisioningConverter.INSTANCE.fromDomain(entity);
        return businessKeyProvisioningMapper.updatePo(po);
    }
}
