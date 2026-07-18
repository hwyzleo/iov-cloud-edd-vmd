package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehImportDataConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆导入数据仓储实现
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehImportDataRepositoryImpl implements VehImportDataRepository {

    private final VehImportDataMapper vehImportDataMapper;

    @Override
    public VehImportData selectById(Long id) {
        VehImportDataPo po = vehImportDataMapper.selectPoById(id);
        return VehImportDataConverter.INSTANCE.toEntity(po);
    }

    @Override
    public VehImportData selectByBatchNum(String batchNum) {
        VehImportDataPo po = vehImportDataMapper.selectPoByBatchNum(batchNum);
        return VehImportDataConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(VehImportData vehImportData) {
        VehImportDataPo po = VehImportDataConverter.INSTANCE.toPo(vehImportData);
        return vehImportDataMapper.insertPo(po);
    }

    @Override
    public int update(VehImportData vehImportData) {
        VehImportDataPo po = VehImportDataConverter.INSTANCE.toPo(vehImportData);
        return vehImportDataMapper.updatePo(po);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return vehImportDataMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<VehImportData> selectList(VehImportData vehImportData) {
        Map<String, Object> map = new HashMap<>();
        if (vehImportData.getBatchNum() != null) {
            map.put("batchNum", vehImportData.getBatchNum());
        }
        if (vehImportData.getType() != null) {
            map.put("type", vehImportData.getType());
        }
        if (vehImportData.getHandle() != null) {
            map.put("handle", vehImportData.getHandle());
        }
        List<VehImportDataPo> poList = vehImportDataMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehImportDataConverter.INSTANCE::toEntity);
    }

    @Override
    public boolean checkBatchNumUnique(Long id, String batchNum) {
        if (id != null) {
            return vehImportDataMapper.countPoByBatchNumAndIdNot(batchNum, id) == 0;
        }
        return vehImportDataMapper.countPoByBatchNum(batchNum) == 0;
    }
}
