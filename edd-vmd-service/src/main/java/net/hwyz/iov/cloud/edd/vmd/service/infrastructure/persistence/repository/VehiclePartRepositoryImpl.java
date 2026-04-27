package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePartHistory;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehiclePartConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartHistoryMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehiclePartRepositoryImpl implements VehiclePartRepository {

    private final VehiclePartMapper vehiclePartMapper;
    private final VehiclePartHistoryMapper vehiclePartHistoryMapper;

    @Override
    public List<VehiclePart> selectByMap(Map<String, Object> map) {
        List<VehiclePartPo> poList = vehiclePartMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehiclePartConverter.INSTANCE::toDomain);
    }

    @Override
    public VehiclePart selectById(Long id) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectPoById(id));
    }

    @Override
    public VehiclePart selectByPnAndSn(String pn, String sn) {
        return VehiclePartConverter.INSTANCE.toDomain(vehiclePartMapper.selectPoByPnAndSn(pn, sn));
    }

    @Override
    public int insert(VehiclePart vehiclePart) {
        return vehiclePartMapper.insertPo(VehiclePartConverter.INSTANCE.fromDomain(vehiclePart));
    }

    @Override
    public int batchInsert(List<VehiclePart> vehiclePartList) {
        return vehiclePartMapper.batchInsertPo(VehiclePartConverter.INSTANCE.fromDomainList(vehiclePartList));
    }

    @Override
    public int update(VehiclePart vehiclePart) {
        return vehiclePartMapper.updatePo(VehiclePartConverter.INSTANCE.fromDomain(vehiclePart));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehiclePartMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public int insertHistory(VehiclePartHistory vehiclePartHistory) {
        return vehiclePartHistoryMapper.insertPo(VehiclePartConverter.INSTANCE.fromHistoryDomain(vehiclePartHistory));
    }

    @Override
    public int batchInsertHistory(List<VehiclePartHistory> vehiclePartHistoryList) {
        return vehiclePartHistoryMapper.batchInsertPo(VehiclePartConverter.INSTANCE.fromHistoryDomainList(vehiclePartHistoryList));
    }

}
