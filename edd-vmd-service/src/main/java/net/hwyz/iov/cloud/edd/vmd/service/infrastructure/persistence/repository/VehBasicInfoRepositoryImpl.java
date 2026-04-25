package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePresetOwner;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleInfoConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehDetailInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehPresetOwnerMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆基础信息数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehBasicInfoRepositoryImpl implements VehBasicInfoRepository {

    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehDetailInfoMapper vehDetailInfoMapper;
    private final VehPresetOwnerMapper vehPresetOwnerMapper;

    @Override
    public List<VehicleBasicInfo> selectByMap(Map<String, Object> map) {
        return VehicleInfoConverter.INSTANCE.toBasicDomainList(vehBasicInfoMapper.selectPoByMap(map));
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehBasicInfoMapper.countPoByMap(map);
    }

    @Override
    public VehicleBasicInfo selectById(Long id) {
        return VehicleInfoConverter.INSTANCE.toBasicDomain(vehBasicInfoMapper.selectPoById(id));
    }

    @Override
    public VehicleBasicInfo selectByVin(String vin) {
        return VehicleInfoConverter.INSTANCE.toBasicDomain(vehBasicInfoMapper.selectPoByVin(vin));
    }

    @Override
    public int insert(VehicleBasicInfo vehicleBasicInfo) {
        return vehBasicInfoMapper.insertPo(VehicleInfoConverter.INSTANCE.fromBasicDomain(vehicleBasicInfo));
    }

    @Override
    public int update(VehicleBasicInfo vehicleBasicInfo) {
        return vehBasicInfoMapper.updatePo(VehicleInfoConverter.INSTANCE.fromBasicDomain(vehicleBasicInfo));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehBasicInfoMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<VehicleDetail> selectDetailByVin(String vin) {
        return VehicleInfoConverter.INSTANCE.toDetailDomainList(vehDetailInfoMapper.selectPoByVin(vin));
    }

    @Override
    public List<VehiclePresetOwner> selectPresetOwnerByExample(VehiclePresetOwner example) {
        return VehicleInfoConverter.INSTANCE.toPresetOwnerDomainList(vehPresetOwnerMapper.selectPoByExample(VehicleInfoConverter.INSTANCE.fromPresetOwnerDomain(example)));
    }

    @Override
    public int batchInsertDetail(List<VehicleDetail> detailList) {
        return vehDetailInfoMapper.batchInsertPo(VehicleInfoConverter.INSTANCE.fromDetailDomainList(detailList));
    }

}
