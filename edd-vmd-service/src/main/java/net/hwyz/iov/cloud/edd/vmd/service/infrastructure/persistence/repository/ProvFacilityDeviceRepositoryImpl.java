package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ProvFacilityDevice;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ProvFacilityDeviceRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ProvFacilityDeviceConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ProvFacilityDeviceMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ProvFacilityDevicePo;
import org.springframework.stereotype.Repository;

/**
 * 安全灌注机注册仓储实现
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProvFacilityDeviceRepositoryImpl implements ProvFacilityDeviceRepository {

    private final ProvFacilityDeviceMapper provFacilityDeviceMapper;

    @Override
    public ProvFacilityDevice selectByFacilityUid(String facilityUid) {
        ProvFacilityDevicePo po = provFacilityDeviceMapper.selectPoByFacilityUid(facilityUid);
        return po != null ? ProvFacilityDeviceConverter.INSTANCE.toDomain(po) : null;
    }

    @Override
    public int insert(ProvFacilityDevice provFacilityDevice) {
        ProvFacilityDevicePo po = ProvFacilityDeviceConverter.INSTANCE.fromDomain(provFacilityDevice);
        int rows = provFacilityDeviceMapper.insertPo(po);
        provFacilityDevice.setId(po.getId());
        return rows;
    }

    @Override
    public int update(ProvFacilityDevice provFacilityDevice) {
        ProvFacilityDevicePo po = ProvFacilityDeviceConverter.INSTANCE.fromDomain(provFacilityDevice);
        return provFacilityDeviceMapper.updatePo(po);
    }
}
