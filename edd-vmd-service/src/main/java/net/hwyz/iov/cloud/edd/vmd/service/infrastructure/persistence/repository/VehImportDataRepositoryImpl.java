package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleImportDataConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆导入数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehImportDataRepositoryImpl implements VehImportDataRepository {

    private final VehImportDataMapper vehImportDataMapper;

    @Override
    public List<VehicleImportData> selectByMap(Map<String, Object> map) {
        List<VehImportDataPo> poList = vehImportDataMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehicleImportDataConverter.INSTANCE::toDomain);
    }

    @Override
    public VehicleImportData selectById(Long id) {
        return VehicleImportDataConverter.INSTANCE.toDomain(vehImportDataMapper.selectPoById(id));
    }

    @Override
    public VehicleImportData selectByBatchNum(String batchNum) {
        return VehicleImportDataConverter.INSTANCE.toDomain(vehImportDataMapper.selectPoByBatchNum(batchNum));
    }

    @Override
    public int insert(VehicleImportData vehicleImportData) {
        return vehImportDataMapper.insertPo(VehicleImportDataConverter.INSTANCE.fromDomain(vehicleImportData));
    }

    @Override
    public int update(VehicleImportData vehicleImportData) {
        return vehImportDataMapper.updatePo(VehicleImportDataConverter.INSTANCE.fromDomain(vehicleImportData));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehImportDataMapper.batchPhysicalDeletePo(ids);
    }

}
