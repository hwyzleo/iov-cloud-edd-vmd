package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.DeviceBusinessKeyConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.DeviceBusinessKeyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DeviceBusinessKeyPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 设备业务密钥目录仓储实现
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceBusinessKeyRepositoryImpl implements DeviceBusinessKeyRepository {

    private final DeviceBusinessKeyMapper deviceBusinessKeyMapper;

    @Override
    public DeviceBusinessKey selectById(Long id) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectPoById(id);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public DeviceBusinessKey selectByKeyId(String keyId) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectPoByKeyId(keyId);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public DeviceBusinessKey selectByRequestId(String requestId) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectPoByRequestId(requestId);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public DeviceBusinessKey selectActiveByContext(String deviceSn, String businessDomain, String purpose) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectActivePoByContext(deviceSn, businessDomain, purpose);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public DeviceBusinessKey selectActiveByContextForUpdate(String deviceSn, String businessDomain, String purpose) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectActivePoByContextForUpdate(deviceSn, businessDomain, purpose);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public List<DeviceBusinessKey> selectByContext(String deviceSn, String businessDomain, String purpose) {
        List<DeviceBusinessKeyPo> poList = deviceBusinessKeyMapper.selectPoListByContext(deviceSn, businessDomain, purpose);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return DeviceBusinessKeyConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public DeviceBusinessKey selectByContextAndVersion(String deviceSn, String businessDomain, String purpose, Long businessKeyVersion) {
        DeviceBusinessKeyPo po = deviceBusinessKeyMapper.selectPoByContextAndVersion(deviceSn, businessDomain, purpose, businessKeyVersion);
        return po != null ? DeviceBusinessKeyConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public Long maxBusinessKeyVersion(String deviceSn, String businessDomain, String purpose) {
        Long max = deviceBusinessKeyMapper.selectMaxBusinessKeyVersion(deviceSn, businessDomain, purpose);
        return max != null ? max : 0L;
    }

    @Override
    public int countActiveByContext(String deviceSn, String businessDomain, String purpose) {
        return deviceBusinessKeyMapper.countActivePoByContext(deviceSn, businessDomain, purpose);
    }

    @Override
    public List<DeviceBusinessKey> selectByDeviceSn(String deviceSn) {
        List<DeviceBusinessKeyPo> poList = deviceBusinessKeyMapper.selectPoListByDeviceSn(deviceSn);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return DeviceBusinessKeyConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public List<DeviceBusinessKey> selectDeprecatedExpired(LocalDateTime now, int limit) {
        List<DeviceBusinessKeyPo> poList = deviceBusinessKeyMapper.selectDeprecatedExpiredPo(now, limit);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return DeviceBusinessKeyConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public int insert(DeviceBusinessKey entity) {
        DeviceBusinessKeyPo po = DeviceBusinessKeyConverter.INSTANCE.fromDomain(entity);
        int rows = deviceBusinessKeyMapper.insertPo(po);
        entity.setId(po.getId());
        entity.setRowVersion(1);
        return rows;
    }

    @Override
    public int update(DeviceBusinessKey entity) {
        DeviceBusinessKeyPo po = DeviceBusinessKeyConverter.INSTANCE.fromDomain(entity);
        return deviceBusinessKeyMapper.updatePo(po);
    }
}
