package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleCertificateConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleCertificateMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificatePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 车辆设备证书数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleCertificateRepositoryImpl implements VehicleCertificateRepository {

    private final VehicleCertificateMapper vehicleCertificateMapper;

    @Override
    public List<VehicleCertificate> selectByMap(Map<String, Object> map) {
        List<VehicleCertificatePo> poList = vehicleCertificateMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehicleCertificateConverter.INSTANCE::toDomain);
    }

    @Override
    public VehicleCertificate selectById(Long id) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectPoById(id));
    }

    @Override
    public VehicleCertificate selectByRequestId(String requestId) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectByRequestId(requestId));
    }

    @Override
    public VehicleCertificate selectByCertSn(String certSn) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectByCertSn(certSn));
    }

    @Override
    public VehicleCertificate selectByPkiRequestId(String pkiRequestId) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectByPkiRequestId(pkiRequestId));
    }

    @Override
    public VehicleCertificate selectActiveByVinAndDeviceCategory(String vin, String deviceCategory) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectActiveByVinAndDeviceCategory(vin, deviceCategory));
    }

    @Override
    public VehicleCertificate selectActiveByDeviceSnAndProfile(String deviceSn, String certificateProfile) {
        return VehicleCertificateConverter.INSTANCE.toDomain(vehicleCertificateMapper.selectActiveByDeviceSnAndProfile(deviceSn, certificateProfile));
    }

    @Override
    public List<VehicleCertificate> selectByDeviceSn(String deviceSn) {
        List<VehicleCertificatePo> poList = vehicleCertificateMapper.selectByDeviceSn(deviceSn);
        return PageUtil.convert(poList, VehicleCertificateConverter.INSTANCE::toDomain);
    }

    @Override
    public List<VehicleCertificate> selectByVin(String vin) {
        List<VehicleCertificatePo> poList = vehicleCertificateMapper.selectByVin(vin);
        return PageUtil.convert(poList, VehicleCertificateConverter.INSTANCE::toDomain);
    }

    @Override
    public List<VehicleCertificate> selectUpdatedAfter(Instant updatedAfter, int limit) {
        List<VehicleCertificatePo> poList = vehicleCertificateMapper.selectUpdatedAfter(updatedAfter, limit);
        return PageUtil.convert(poList, VehicleCertificateConverter.INSTANCE::toDomain);
    }

    @Override
    public int insert(VehicleCertificate vehicleCertificate) {
        return vehicleCertificateMapper.insertPo(VehicleCertificateConverter.INSTANCE.fromDomain(vehicleCertificate));
    }

    @Override
    public int update(VehicleCertificate vehicleCertificate) {
        return vehicleCertificateMapper.updatePo(VehicleCertificateConverter.INSTANCE.fromDomain(vehicleCertificate));
    }

    @Override
    public int updateStatusByRequestId(String requestId, CertificateStatus fromStatus, CertificateStatus toStatus) {
        return vehicleCertificateMapper.updateStatusByRequestId(requestId, fromStatus.name(), toStatus.name());
    }

    @Override
    public int physicalDeleteById(Long id) {
        return vehicleCertificateMapper.physicalDeletePo(id);
    }

}
