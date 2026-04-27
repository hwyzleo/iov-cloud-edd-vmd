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
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehDetailInfoPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehPresetOwnerPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
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
        List<VehBasicInfoPo> poList = vehBasicInfoMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehicleInfoConverter.INSTANCE::toBasicDomain);
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
        List<VehDetailInfoPo> poList = vehDetailInfoMapper.selectPoByVin(vin);
        return PageUtil.convert(poList, VehicleInfoConverter.INSTANCE::toDetailDomain);
    }

    @Override
    public List<VehiclePresetOwner> selectPresetOwnerByExample(VehiclePresetOwner example) {
        List<VehPresetOwnerPo> poList = vehPresetOwnerMapper.selectPoByExample(VehicleInfoConverter.INSTANCE.fromPresetOwnerDomain(example));
        return PageUtil.convert(poList, VehicleInfoConverter.INSTANCE::toPresetOwnerDomain);
    }

    @Override
    public int batchInsertDetail(List<VehicleDetail> detailList) {
        return vehDetailInfoMapper.batchInsertPo(VehicleInfoConverter.INSTANCE.fromDetailDomainList(detailList));
    }

}
